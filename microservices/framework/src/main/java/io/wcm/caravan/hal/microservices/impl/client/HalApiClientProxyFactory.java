/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.hal.microservices.impl.client;

import java.lang.reflect.Proxy;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.reactivex.Single;
import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.api.annotations.ResourceLink;
import io.wcm.caravan.hal.microservices.api.client.HalApiClient;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.HalResponse;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.impl.metadata.EmissionStopwatch;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

/**
 * Contains static factory methods to create proxy implementations of a given interface annotated with
 * {@link HalApiInterface}
 */
final class HalApiClientProxyFactory {


  private final Cache<String, Object> proxyCache = CacheBuilder.newBuilder().build();

  private final JsonResourceLoader jsonLoader;
  private final RequestMetricsCollector metrics;


  HalApiClientProxyFactory(JsonResourceLoader jsonLoader, RequestMetricsCollector metrics) {
    this.metrics = metrics;
    this.jsonLoader = jsonLoader;
  }

  <T> T createProxyFromUrl(Class<T> relatedResourceType, String url) {

    Single<HalResource> rxHal = loadHalResource(url, relatedResourceType);

    return getProxy(relatedResourceType, rxHal, new Link(url));
  }

  <T> T createProxyFromLink(Class<T> relatedResourceType, Link link) {

    Single<HalResource> rxHal = loadHalResource(link.getHref(), relatedResourceType);

    return getProxy(relatedResourceType, rxHal, link);
  }

  <T> T createProxyFromHalResource(Class<T> relatedResourceType, HalResource contextResource, Link link) {

    Single<HalResource> rxHal = Single.just(contextResource);

    return getProxy(relatedResourceType, rxHal, link);
  }

  private <T> Single<HalResource> loadHalResource(String resourceUrl, Class<T> relatedResourceType) {

    // this additional single is only required because we want to validate the URL only on subscription
    // (e.g. right before it is actually retrieved).
    // This is because i should still be possible to create a proxy just to get a URI template
    // by calling a method annotated with @ResourceLink.
    return Single.just(resourceUrl)
        .flatMap(url -> {
          Link link = new Link(url);
          if (link.isTemplated()) {
            throw new UnsupportedOperationException("Cannot follow the link template to " + link.getHref()
                + " because it has not been expanded."
                + " If you are calling a proxy method with parameters then make sure to provide at least one parameter "
                + "(unless you are only interested in obtaining the link template by calling the method annotated with @" + ResourceLink.class.getSimpleName()
                + ")");
          }

          return jsonLoader.loadJsonResource(url)
              .map(HalResponse::getBody);
        })
        .compose(EmissionStopwatch.collectMetrics("fetching " + relatedResourceType.getSimpleName() + " resource from upstream server", metrics));
  }


  @SuppressWarnings("unchecked")
  private <T> T getProxy(Class<T> relatedResourceType, Single<HalResource> rxHal, Link linkToResource) {

    if (linkToResource == null) {
      return createProxy(relatedResourceType, rxHal, linkToResource);
    }

    String cacheKey = linkToResource.getModel().toString();

    try {
      return (T)proxyCache.get(cacheKey, () -> createProxy(relatedResourceType, rxHal, linkToResource));
    }
    catch (ExecutionException ex) {
      throw new RuntimeException(ex);
    }

  }

  private <T> T createProxy(Class<T> relatedResourceType, Single<HalResource> rxHal, Link linkToResource) {

    Stopwatch sw = Stopwatch.createStarted();

    try {
      // check that the given class is indeed a HAL api interface
      HalApiInterface annotation = relatedResourceType.getAnnotation(HalApiInterface.class);
      Preconditions.checkNotNull(annotation,
          "The given resource interface " + relatedResourceType.getName() + " does not have a @" + HalApiInterface.class.getSimpleName() + " annotation.");

      Class[] interfaces = getInterfacesToImplement(relatedResourceType);

      // the main logic of the proxy is implemented in this InvocationHandler
      HalApiInvocationHandler invocationHandler = new HalApiInvocationHandler(rxHal, relatedResourceType, linkToResource, this, metrics);

      @SuppressWarnings("unchecked")
      T proxy = (T)Proxy.newProxyInstance(relatedResourceType.getClassLoader(), interfaces, invocationHandler);

      return proxy;
    }
    finally {
      metrics.onMethodInvocationFinished(HalApiClient.class,
          "creating " + relatedResourceType.getSimpleName() + " proxy instance",
          sw.elapsed(TimeUnit.MICROSECONDS));
    }
  }

  private <T> Class[] getInterfacesToImplement(Class<T> relatedResourceType) {
    List<Class> interfaces = new LinkedList<>();
    interfaces.add(relatedResourceType);
    return interfaces.toArray(new Class[interfaces.size()]);
  }
}

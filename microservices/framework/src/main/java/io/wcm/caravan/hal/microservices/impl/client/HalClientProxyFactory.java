/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.hal.microservices.impl.client;

import java.lang.reflect.Proxy;
import java.util.LinkedList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.resource.HalResource;

/**
 * Contains static factory methods to create proxy implementations of a given HAL Api interface
 */
final class HalClientProxyFactory {

  private HalClientProxyFactory() {
    // only static methods
  }

  /**
   * @param relatedResourceType an interface annoted with {@link HalApiInterface}
   * @param url the absolute Path of the HAL/JSON resource to fetch
   * @param jsonLoader the function used to retrieve and parse that resource
   * @param responseMetadata of the current request
   * @param linkTitle the link title to use if {@link LinkableResource#createLink()} is called on the proxy
   * @return an {@link Observable} that will emit the proxy when the resource has been retrieved
   */
  static <T> T createProxyFromUrl(Class<T> relatedResourceType, String url, JsonResourceLoader jsonLoader,
      RequestMetricsCollector responseMetadata, String linkTitle) {

    Single<HalResource> rxHal = jsonLoader.loadJsonResource(url, responseMetadata)
        .map(json -> new HalResource(json));

    return createProxy(relatedResourceType, rxHal, jsonLoader, responseMetadata);
  }

  static <T> T createProxyFromHalResource(Class<T> relatedResourceType, HalResource contextResource, JsonResourceLoader jsonLoader,
      RequestMetricsCollector responseMetadata) {

    Single<HalResource> rxHal = Single.just(contextResource);

    return createProxy(relatedResourceType, rxHal, jsonLoader, responseMetadata);
  }

  private static <T> T createProxy(Class<T> relatedResourceType, Single<HalResource> rxHal, JsonResourceLoader jsonLoader,
      RequestMetricsCollector responseMetadata) {
    Class[] interfaces = getInterfacesToImplement(relatedResourceType);

    // the main logic of the proxy is implemented in this InvocationHandler
    HalApiInvocationHandler invocationHandler = new HalApiInvocationHandler(rxHal, relatedResourceType, jsonLoader, responseMetadata);

    @SuppressWarnings("unchecked")
    T proxy = (T)Proxy.newProxyInstance(relatedResourceType.getClassLoader(), interfaces, invocationHandler);

    return proxy;
  }

  private static <T> Class[] getInterfacesToImplement(Class<T> relatedResourceType) {
    List<Class> interfaces = new LinkedList<>();
    interfaces.add(relatedResourceType);

    // BYI-1693 only add LinkableResource to the list of interfaces to implement if the related resource type *extends* from LinkableResource
    // if the return type *is* only a LinkableResource then this would lead to a exception later o
    if (LinkableResource.class != relatedResourceType && LinkableResource.class.isAssignableFrom(relatedResourceType)) {
      interfaces.add(LinkableResource.class);
    }

    return interfaces.toArray(new Class[interfaces.size()]);
  }
}

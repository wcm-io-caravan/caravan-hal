/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.hal.microservices.impl.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

/**
 * Introduced as part of the BYI-1396 workaround. This will create a dynamic proxy for a Hal API resource *without*
 * fetching the resources first.
 * The main goal is that you can call {@link LinkableResource#createLink()} on these proxies *before* the resource is
 * actually fetched
 */
final class AsyncHalApiInvocationHandler implements InvocationHandler {

  private final Class relatedResourceType;
  private final String url;
  private final JsonResourceLoader jsonLoader;
  private final RequestMetricsCollector responseMetadata;
  private final String linkTitle;

  AsyncHalApiInvocationHandler(Class relatedResourceType, String url, JsonResourceLoader jsonLoader, RequestMetricsCollector responseMetadata,
      String linkTitle) {

    this.relatedResourceType = relatedResourceType;
    this.url = url;
    this.jsonLoader = jsonLoader;
    this.responseMetadata = responseMetadata;
    this.linkTitle = linkTitle;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

    // if the #createLink method was called we immediately return a link - no need to actually fetch the resources
    if (method.getName().equals("createLink") && method.getParameterCount() == 0) {
      Link link = new Link(url).setTitle(linkTitle);

      // if this resource has binary resource content then look up the contentType from the annotation
      // TODO: handle content type
      //      BinaryResourceContent resourceContent = HalApiReflectionUtils.getAnnotationFromFirstMethod(relatedResourceType, RelatedResource.class);
      //      if (resourceContent != null && StringUtils.isNotBlank(resourceContent.contentType())) {
      //        link.setType(resourceContent.contentType());
      //      }


      return link;
    }
    else {
      // otherwise fetch the resource and dispatch the invocation handling to the regular HalApiInvocationHandler
      HalApiInvocationHandler syncHandler = jsonLoader.loadJsonResource(url, responseMetadata)
          .map(json -> new HalResource(json))
          .map(contextResource -> new HalApiInvocationHandler(contextResource, relatedResourceType, jsonLoader, responseMetadata))
          .blockingGet();

      return syncHandler.invoke(proxy, method, args);
    }
  }
}

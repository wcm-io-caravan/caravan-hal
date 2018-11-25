/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2017 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.caravan.hal.microservices.impl.renderer;

import static io.wcm.caravan.hal.microservices.impl.reflection.HalApiReflectionUtils.findHalApiInterface;
import static io.wcm.caravan.hal.microservices.impl.reflection.HalApiReflectionUtils.getResourceStateObservable;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;

import io.reactivex.Single;
import io.wcm.caravan.hal.microservices.api.server.AsyncHalResourceRenderer;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.microservices.impl.renderer.RelatedResourcesRendererImpl.RelationRenderResult;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

/**
 * Asynchronously generate a {@link HalResource} instance from the given server-side HAL resource implementation
 */
public final class AsyncHalResourceRendererImpl implements AsyncHalResourceRenderer {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Override
  public Single<HalResource> renderResource(LinkableResource resourceImpl) {
    return renderLinkedOrEmbeddedResource(resourceImpl);
  }

  static Single<HalResource> renderLinkedOrEmbeddedResource(Object resourceImplInstance) {

    Preconditions.checkNotNull(resourceImplInstance, "Cannot create a HalResource from a null reference");

    // find the interface annotated with @HalApiInterface
    Class<?> apiInterface = findHalApiInterface(resourceImplInstance);

    // get the JSON resource state from the method annotated with @ResourceState
    Single<ObjectNode> rxState = getResourceStateAndConvertToJson(apiInterface, resourceImplInstance);

    // render links and embedded resources for each method annotated with @RelatedResource
    Single<List<RelationRenderResult>> rxRelated = RelatedResourcesRendererImpl.renderRelated(apiInterface, resourceImplInstance);

    // wait until all this is available...
    Single<HalResource> rxHalResource = Single.zip(rxState, rxRelated,
        // ...and then create the HalResource instance
        (stateNode, listOfRelated) -> createHalResource(resourceImplInstance, stateNode, listOfRelated));

    return rxHalResource;
  }

  private static Single<ObjectNode> getResourceStateAndConvertToJson(Class<?> apiInterface, Object resourceImplInstance) {

    // call the method annotated with @ResourceState (and convert the return value to Observable if necessary)
    return getResourceStateObservable(apiInterface, resourceImplInstance)
        // then convert the value emitted by the Observable to a Jackson JSON node
        .map(object -> OBJECT_MAPPER.convertValue(object, ObjectNode.class));
  }

  private static HalResource createHalResource(Object resourceImplInstance, ObjectNode stateNode, List<RelationRenderResult> listOfRelated) {

    HalResource hal = new HalResource(stateNode);

    if (resourceImplInstance instanceof LinkableResource) {
      Link selfLink = ((LinkableResource)resourceImplInstance).createLink();
      hal.setLink(selfLink);
    }

    for (RelationRenderResult related : listOfRelated) {
      hal.addLinks(related.getRelation(), related.getLinks());
      hal.addEmbedded(related.getRelation(), related.getEmbedded());
    }

    return hal;
  }
}

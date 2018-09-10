/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2018 wcm.io
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
package io.wcm.caravan.hal.api.server.impl;

import static io.wcm.caravan.hal.api.server.impl.reflection.HalApiReflectionUtils.getSortedRelatedResourceMethods;
import static io.wcm.caravan.hal.api.server.impl.reflection.RxJavaReflectionUtils.invokeMethodAndReturnObservable;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.api.annotations.RelatedResource;
import io.wcm.caravan.hal.api.server.EmbeddableResource;
import io.wcm.caravan.hal.api.server.LinkableResource;
import io.wcm.caravan.hal.api.server.impl.reflection.RxJavaReflectionUtils;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import rx.Observable;
import rx.Single;

class RelatedContentRendererImpl {

  private final Function<Object, Single<HalResource>> renderFunction;

  /**
   * @param recursiveRenderFunc the function to call to render an embedded resource
   */
  RelatedContentRendererImpl(Function<Object, Single<HalResource>> recursiveRenderFunc) {
    this.renderFunction = recursiveRenderFunc;
  }

  /**
   * @param resourceInterface an interface annotated with {@link HalApiInterface}
   * @param resourceImplInstance the resource for which the related
   * @return a {@link Single} that emits a list with one {@link RelatedContent} instance for each method annotated with
   *         {@link RelatedResource}
   */
  Single<List<RelatedContent>> render(Class<?> resourceInterface, Object resourceImplInstance) {

    // find all methods annotated with @RelatedResource
    return getSortedRelatedResourceMethods(resourceInterface)
        // create a RelatedContent instance with the links and embedded resources returned by each method
        .concatMap(method -> createRelatedContentForMethod(resourceImplInstance, method).toObservable())
        // and collect the results for each method in a single list
        .toList().toSingle();
  }

  private Single<RelatedContent> createRelatedContentForMethod(Object resourceImplInstance, Method method) {

    verifyReturnType(resourceImplInstance, method);
    String relation = method.getAnnotation(RelatedResource.class).relation();

    // call the implementation of the method to get an observable of related resource implementation instances
    Observable<?> rxRelatedResources = invokeMethodAndReturnObservable(resourceImplInstance, method).cache();

    // create links for those resources that implement LinkableResource
    Single<List<Link>> rxLinks = createLinksTo(rxRelatedResources);

    // and (asynchronously) render those resources that should be embedded
    Single<List<HalResource>> rxEmbeddedHalResources = renderEmbeddedResources(method, rxRelatedResources);

    // wait for all this to be complete before creating a RelatedResourceInfo for this method
    return Single.zip(rxLinks, rxEmbeddedHalResources, (links, embeddedResources) -> {
      return new RelatedContent(relation, links, embeddedResources);
    });
  }

  private static void verifyReturnType(Object resourceImplInstance, Method method) {
    String fullMethodName = resourceImplInstance.getClass().getSimpleName() + "#" + method.getName();

    // get the emitted result resource type from the method signature
    Class<?> relatedResourceInterface = RxJavaReflectionUtils.getObservableEmissionType(method);
    if (relatedResourceInterface.getAnnotation(HalApiInterface.class) == null && !LinkableResource.class.equals(relatedResourceInterface)) {
      throw new RuntimeException("The method " + fullMethodName + " returns an Observable<" + relatedResourceInterface.getName() + ">, "
          + " but it must return an Observable that emits objects that implement a HAL API interface annotated with the @"
          + HalApiInterface.class.getSimpleName() + " annotation");
    }
  }

  private static Single<List<Link>> createLinksTo(Observable<?> rxRelatedResources) {

    // filter only those resources that are Linkable
    Observable<LinkableResource> rxLinkedResourceImpls = rxRelatedResources
        .filter(r -> r instanceof LinkableResource)
        .map(r -> (LinkableResource)r);

    // and let each resource create a link to itself
    Observable<Link> rxLinks = rxLinkedResourceImpls
        .map(r -> r.createLink())
        .filter(Objects::nonNull);

    return rxLinks.toList().toSingle();
  }

  private Single<List<HalResource>> renderEmbeddedResources(Method method, Observable<?> rxRelatedResources) {

    // embedded resources can only occur for methods that don't have parameters
    // (because of the method has parameters, it is a link template)
    if (method.getParameterCount() == 0) {

      // filter only those resources that are actually embedded
      Observable<EmbeddableResource> rxEmbeddedResourceImpls = rxRelatedResources
          .filter(r -> r instanceof EmbeddableResource)
          .map(r -> (EmbeddableResource)r)
          .filter(r -> r.isEmbedded());

      // and render them by recursively calling the render function from AsyncHalresourceRendererImpl
      Observable<HalResource> rxHalResources = rxEmbeddedResourceImpls
          .concatMap(r -> renderFunction.apply(r).toObservable());

      return rxHalResources.toList().toSingle();
    }

    return Single.just(Collections.emptyList());
  }

  /**
   * A result class that combines all links and embedded resources for a given relation.
   */
  static class RelatedContent {

    private final String relation;
    private final List<Link> links;
    private final List<HalResource> embedded;

    private RelatedContent(String relation, List<Link> links, List<HalResource> embedded) {
      this.relation = relation;
      this.links = links;
      this.embedded = embedded;
    }

    String getRelation() {
      return this.relation;
    }

    List<Link> getLinks() {
      return this.links;
    }

    List<HalResource> getEmbedded() {
      return this.embedded;
    }
  }
}
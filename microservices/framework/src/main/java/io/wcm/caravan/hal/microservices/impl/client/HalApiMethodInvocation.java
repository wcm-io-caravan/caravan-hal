/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.hal.microservices.impl.client;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import io.wcm.caravan.hal.api.annotations.RelatedResource;
import io.wcm.caravan.hal.api.annotations.ResourceRepresentation;
import io.wcm.caravan.hal.api.annotations.ResourceState;
import io.wcm.caravan.hal.microservices.impl.reflection.RxJavaReflectionUtils;


class HalApiMethodInvocation {

  private final Class interfaze;
  private final Method method;
  private final Map<String, Object> parameters;

  HalApiMethodInvocation(Class interfaze, Method method, Object[] args) {
    this.interfaze = interfaze;
    this.method = method;
    this.parameters = new HashMap<>();

    for (int i = 0; i < method.getParameterCount(); i++) {
      Parameter parameter = method.getParameters()[i];
      parameters.put(parameter.getName(), args[i]);
    }
  }

  boolean isRelatedResource() {
    return method.getAnnotation(RelatedResource.class) != null;
  }

  //  boolean isSelectedLinkedResource() {
  //    return method.getAnnotation(SelectedLinkedResource.class) != null;
  //  }

  boolean isResourceProperties() {
    return method.getAnnotation(ResourceState.class) != null;
  }

  boolean isCreateLink() {
    return method.getName().equals("createLink") && method.getParameterCount() == 0;
  }

  boolean isResourceRepresentation() {
    return method.getAnnotation(ResourceRepresentation.class) != null;
  }

  public boolean returnsReactiveType() {
    return RxJavaReflectionUtils.hasReactiveReturnType(method);
  }

  public Class<?> getReturnType() {
    return method.getReturnType();
  }

  String getRelation() {
    String relation = null;
    RelatedResource relatedResourceAnnotation = method.getAnnotation(RelatedResource.class);
    if (relatedResourceAnnotation != null) {
      relation = relatedResourceAnnotation.relation();
    }

    // TODO: enable support for SelectedLinkedResource
    //    SelectedLinkedResource selectedLinkedResourceAnnotation = method.getAnnotation(SelectedLinkedResource.class);
    //    if (selectedLinkedResourceAnnotation != null) {
    //      relation = selectedLinkedResourceAnnotation.relation();
    //    }

    Preconditions.checkNotNull(relation, this + " does not have a @RelatedResource or @SelectedLinkedResource annotation");

    return relation;
  }

  Class<?> getEmissionType() {
    return RxJavaReflectionUtils.getObservableEmissionType(method);
  }

  Map<String, Object> getParameters() {
    return this.parameters;
  }

  @Override
  public String toString() {

    String parameterString = parameters.entrySet().stream()
        .map(entry -> entry.getKey() + "=" + entry.getValue())
        .collect(Collectors.joining(","));

    return interfaze.getSimpleName() + "#" + method.getName() + "(" + parameterString + ")";
  }

}

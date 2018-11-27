/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.hal.microservices.impl.client;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import io.wcm.caravan.hal.api.annotations.LinkName;
import io.wcm.caravan.hal.api.annotations.RelatedResource;
import io.wcm.caravan.hal.api.annotations.ResourceLink;
import io.wcm.caravan.hal.api.annotations.ResourceRepresentation;
import io.wcm.caravan.hal.api.annotations.ResourceState;
import io.wcm.caravan.hal.api.annotations.TemplateVariable;
import io.wcm.caravan.hal.microservices.impl.reflection.RxJavaReflectionUtils;


class HalApiMethodInvocation {

  private final Class interfaze;
  private final Method method;
  private final Map<String, Object> templateVariables;
  private final String linkName;

  HalApiMethodInvocation(Class interfaze, Method method, Object[] args) {
    this.interfaze = interfaze;
    this.method = method;
    this.templateVariables = new HashMap<>();

    String foundLinkName = null;
    for (int i = 0; i < method.getParameterCount(); i++) {
      Parameter parameter = method.getParameters()[i];
      TemplateVariable variable = parameter.getAnnotation(TemplateVariable.class);
      LinkName name = parameter.getAnnotation(LinkName.class);

      Preconditions.checkArgument(variable != null || name != null,
          "all parameters of " + toString() + " need to be annotated with wither @"
              + TemplateVariable.class.getSimpleName() + " or @" + LinkName.class.getSimpleName());

      if (variable != null) {
        templateVariables.put(variable.value(), args[i]);
      }

      if (name != null) {
        if (foundLinkName != null) {
          throw new IllegalArgumentException("More than one parameter of " + toString() + " is annotated with @" + LinkName.class.getSimpleName());
        }
        if (args[i] == null) {
          throw new IllegalArgumentException(
              "You must provide a non-null value for for the parameter annotated with @" + LinkName.class.getSimpleName() + " when calling " + toString());
        }
        foundLinkName = args[i] != null ? args[i].toString() : null;
      }
    }

    this.linkName = foundLinkName;
  }

  String getRelation() {
    RelatedResource relatedResourceAnnotation = method.getAnnotation(RelatedResource.class);

    Preconditions.checkNotNull(relatedResourceAnnotation, this + " does not have a @" + RelatedResource.class.getSimpleName() + " annotation");

    return relatedResourceAnnotation.relation();
  }

  boolean isRelatedResource() {
    return method.getAnnotation(RelatedResource.class) != null;
  }

  boolean isResourceLink() {
    return method.getAnnotation(ResourceLink.class) != null;
  }

  boolean isResourceProperties() {
    return method.getAnnotation(ResourceState.class) != null;
  }

  boolean isResourceRepresentation() {
    return method.getAnnotation(ResourceRepresentation.class) != null;
  }

  boolean returnsReactiveType() {
    return RxJavaReflectionUtils.hasReactiveReturnType(method);
  }

  Class<?> getReturnType() {
    return method.getReturnType();
  }

  Class<?> getEmissionType() {
    return RxJavaReflectionUtils.getObservableEmissionType(method);
  }

  Map<String, Object> getTemplateVariables() {
    return templateVariables;
  }

  String getLinkName() {
    return linkName;
  }

  @Override
  public String toString() {

    String parameterString = templateVariables.entrySet().stream()
        .map(entry -> entry.getKey() + "=" + entry.getValue())
        .collect(Collectors.joining(","));

    return interfaze.getSimpleName() + "#" + method.getName() + "(" + parameterString + ")";
  }

}

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
import io.wcm.caravan.hal.api.annotations.TemplateVariables;
import io.wcm.caravan.hal.microservices.impl.reflection.HalApiReflectionUtils;
import io.wcm.caravan.hal.microservices.impl.reflection.RxJavaReflectionUtils;


class HalApiMethodInvocation {

  private final Class interfaze;
  private final Method method;

  private final Map<String, Object> templateVariables;
  private final String linkName;
  private final boolean calledWithOnlyNullParameters;

  HalApiMethodInvocation(Class interfaze, Method method, Object[] args) {
    this.interfaze = interfaze;
    this.method = method;
    this.templateVariables = new HashMap<>();

    boolean nonNullParameterFound = false;
    String foundLinkName = null;
    for (int i = 0; i < method.getParameterCount(); i++) {
      Parameter parameter = method.getParameters()[i];

      Object parameterValue = args[i];
      nonNullParameterFound = nonNullParameterFound || parameterValue != null;

      TemplateVariable variable = parameter.getAnnotation(TemplateVariable.class);
      LinkName name = parameter.getAnnotation(LinkName.class);
      TemplateVariables variables = parameter.getType().getAnnotation(TemplateVariables.class);

      if (variable != null) {
        templateVariables.put(variable.value(), parameterValue);
      }
      else if (variables != null) {
        templateVariables.putAll(HalApiReflectionUtils.getTemplateVariablesFrom(parameterValue, parameter.getType()));
      }
      else if (name != null) {
        if (foundLinkName != null) {
          throw new UnsupportedOperationException("More than one parameter of " + toString() + " is annotated with @" + LinkName.class.getSimpleName());
        }
        if (parameterValue == null) {
          throw new IllegalArgumentException(
              "You must provide a non-null value for for the parameter annotated with @" + LinkName.class.getSimpleName() + " when calling " + toString());
        }
        foundLinkName = parameterValue.toString();
      }
      else {
        throw new UnsupportedOperationException("all parameters of " + toString() + " need to be either annotated with @"
            + TemplateVariable.class.getSimpleName() + " or @" + LinkName.class.getSimpleName() + ", "
            + " or have class type annotated with @" + TemplateVariables.class.getSimpleName());
      }

    }

    this.linkName = foundLinkName;
    this.calledWithOnlyNullParameters = !nonNullParameterFound;
  }


  String getRelation() {
    RelatedResource relatedResourceAnnotation = method.getAnnotation(RelatedResource.class);

    Preconditions.checkNotNull(relatedResourceAnnotation, this + " does not have a @" + RelatedResource.class.getSimpleName() + " annotation");

    return relatedResourceAnnotation.relation();
  }

  boolean isForMethodAnnotatedWithRelatedResource() {
    return method.getAnnotation(RelatedResource.class) != null;
  }

  boolean isForMethodAnnotatedWithResourceLink() {
    return method.getAnnotation(ResourceLink.class) != null;
  }

  boolean isForMethodAnnotatedWithResourceState() {
    return method.getAnnotation(ResourceState.class) != null;
  }

  boolean isForMethodAnnotatedWithResourceRepresentation() {
    return method.getAnnotation(ResourceRepresentation.class) != null;
  }

  Class<?> getReturnType() {
    return method.getReturnType();
  }

  Class<?> getEmissionType() {
    return RxJavaReflectionUtils.getObservableEmissionType(method);
  }

  boolean isCalledWithOnlyNullParameters() {
    return calledWithOnlyNullParameters;
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

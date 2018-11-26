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
package io.wcm.caravan.hal.microservices.impl.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.damnhandy.uri.template.UriTemplate;

import io.reactivex.Observable;
import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

class RelatedResourceHandler {

  private static final Logger log = LoggerFactory.getLogger(HalApiInvocationHandler.class);

  private final HalResource contextResource;
  private final JsonResourceLoader jsonLoader;
  private final RequestMetricsCollector metrics;

  RelatedResourceHandler(HalResource contextResource, JsonResourceLoader jsonLoader, RequestMetricsCollector metrics) {
    this.contextResource = contextResource;
    this.jsonLoader = jsonLoader;
    this.metrics = metrics;
  }

  Observable<?> handleMethodInvocation(HalApiMethodInvocation invocation) {

    // check which relation should be followed and what type of objects the Observable emits
    String relation = invocation.getRelation();
    Class<?> relatedResourceType = invocation.getEmissionType();

    if (relatedResourceType.getAnnotation(HalApiInterface.class) == null) {
      // BYI-1693 HAL-API-Interfaces can contain related links that just point to an URL (i.e. a LinkableResource, not a specific resource instance)
      // in that case, we just want to forward that link information, similar to how we handle SDL binary resources
      if (relatedResourceType.equals(LinkableResource.class)) {
        List<Link> links = contextResource.getLinks(relation);
        return createObservableFromLinkedExternalResources(relatedResourceType, links, Collections.emptyMap());
      }

      throw new RuntimeException("The method " + invocation + " has an invalid return an Observable of " + relatedResourceType.getName() +
          " which does not have a @" + HalApiInterface.class + " annotation.");
    }

    log.trace(invocation + " was invoked, method is annotated with @RelatedResources cur=" + relation + " and returns an Observable<"
        + relatedResourceType.getSimpleName() + ">");


    // the related resource might be already embedded in the context HAL resource
    if (contextResource.hasEmbedded(relation)) {
      List<HalResource> embeddedResources = contextResource.getEmbedded(relation);
      log.trace(embeddedResources.size() + " embedded resources with relation " + relation + " were found in the context resource");

      Observable<?> rxEmbedded = createObservableFromEmbeddedResources(relatedResourceType, embeddedResources);
      return rxEmbedded;
    }
    // if it's not then it must be linked
    else if (contextResource.hasLink(relation)) {
      List<Link> links = contextResource.getLinks(relation);
      log.trace(links.size() + " links with relation " + relation + " were found in the context resource and will be fetched.");

      Map<String, Object> parameters = invocation.getParameters();

      boolean hasParameters = parameters.size() > 0;

      if (hasParameters) {
        // verify that the names of the parametrs could be extracted through reflection
        // (assuming that developers don't  use arg0, arg1, arg2 as parameter names in their APIs)
        long numUnnamedParameters = parameters.keySet().stream()
            .filter(argName -> argName.startsWith("arg"))
            .filter(argName -> NumberUtils.isNumber(StringUtils.substringAfter(argName, "arg")))
            .count();

        if (numUnnamedParameters == parameters.size()) {
          throw new RuntimeException("Failed to call HAL API method " + invocation + ", because the parameter names are not available in the class files."
              + " Please ensure that parameter names are not stripped from the class files in your API bundles"
              + " (e.g. by using <arg>-parameters</arg> for the maven-compiler-plugin)");
        }
      }

      Observable<?> rxLinkedResource = createObservableFromLinkedHalResources(relatedResourceType, links, parameters);

      return rxLinkedResource;

    }
    // if it's not linked and not embedded then just return an empty observable
    else {
      return Observable.empty();
    }
  }


  private Observable<?> createObservableFromEmbeddedResources(Class<?> relatedResourceType, List<HalResource> embeddedResources) {
    // if the HAL resources are already embedded then creating the proxy is very simple
    return Observable.fromIterable(embeddedResources)
        .map(hal -> HalClientProxyFactory.createProxyFromHalResource(relatedResourceType, hal, jsonLoader, metrics));
  }

  private Observable<?> createObservableFromLinkedExternalResources(Class<?> relatedResourceType, List<Link> links, Map<String, Object> parameters) {

    List<Link> resolvedLinks = links.stream()
        .map(link -> {
          Map<String, Object> effectiveParameters = getEffectiveParameters(parameters);
          String newHref = UriTemplate.expand(link.getHref(), effectiveParameters);
          return new Link(newHref).setName(link.getName()).setTitle(link.getTitle());
        })
        .collect(Collectors.toList());

    return Observable.fromIterable(resolvedLinks)
        .map(link -> HalClientProxyFactory.createProxyFromUrl(relatedResourceType, link.getHref(), jsonLoader, metrics, link.getTitle()));
  }

  private Observable<?> createObservableFromLinkedHalResources(Class<?> relatedResourceType, List<Link> links, Map<String, Object> parameters) {

    // if the resources are linked, then we have to fetch those resources first

    return Observable.fromIterable(links)
        .map(link -> expandLinkTemplates(link, parameters))
        .map(url -> HalClientProxyFactory.createProxyFromUrl(relatedResourceType, url, jsonLoader, metrics, null));
  }


  private String expandLinkTemplates(Link link, Map<String, Object> parameters) {
    Map<String, Object> effectiveParameters = getEffectiveParameters(parameters);

    return UriTemplate.expand(link.getHref(), effectiveParameters);
  }

  /**
   * get the effective parameters including the fields of the DTO objects.
   * if the parameter is DTO object, than put its felds also into the parameter maps
   */
  private Map<String, Object> getEffectiveParameters(Map<String, Object> parameters) {
    Map<String, Object> effectiveParameters = new HashMap<>(parameters);

    //    Map<String, Object> dtoObjects = getDtoObjects(effectiveParameters);
    //
    //    if (!dtoObjects.isEmpty()) {
    //      // remove the dto parameters from the effective parameters
    //      effectiveParameters.keySet().removeAll(dtoObjects.keySet());
    //
    //      // add all not-null fields of dto objects to effective parameters
    //      Map<String, Object> dtoParameters = getQueryFieldAsParameters(dtoObjects);
    //      effectiveParameters.putAll(dtoParameters);
    //    }
    return effectiveParameters;
  }

  //  /**
  //   * get all not-null fields annotated with QueryParam or PathParam from the dto objects.
  //   * Return a map of fieldName, fieldValue
  //   */
  //  private Map<String, Object> getQueryFieldAsParameters(Map<String, Object> dtoObjects) {
  //    Map<String, Object> dtoParameters = new HashMap<>();
  //    dtoObjects.values()
  //        .forEach(dtoObject -> {
  //
  //          for (Field field : HalApiReflectionUtils.getDtoFields(dtoObject)) {
  //            String fieldName = field.getName();
  //
  //            Object fieldValue = HalApiReflectionUtils.getParameterValueFromDtoObject(dtoObject, fieldName);
  //
  //            if (fieldValue != null) {
  //              if (field.isEnumConstant()) {
  //                dtoParameters.put(fieldName, fieldValue.toString());
  //              }
  //              else {
  //                dtoParameters.put(fieldName, fieldValue);
  //              }
  //            }
  //          }
  //        });
  //    return dtoParameters;
  //  }

  //  private Map<String, Object> getDtoObjects(Map<String, Object> effectiveParameters) {
  //    return effectiveParameters.entrySet().stream()
  //        .filter(paramEntry -> {
  //          Object value = paramEntry.getValue();
  //          // is the parameter class a DTO class(annotated with HalApiParamDto)
  //          return value != null && HalApiReflectionUtils.isDtoObject(value.getClass());
  //        })
  //        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  //  }

}

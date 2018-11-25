/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.hal.microservices.impl.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.damnhandy.uri.template.UriTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Stopwatch;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.api.annotations.RelatedResource;
import io.wcm.caravan.hal.api.annotations.ResourceState;
import io.wcm.caravan.hal.api.annotations.StandardRelations;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

/**
 * Handles calls to proxy methods from dynamic proxies created with
 * {@link HalClientProxyFactory#createProxyFromHalResource(Class, HalResource, JsonResourceLoader, RequestMetricsCollector)}
 */
final class HalApiInvocationHandler implements InvocationHandler {

  private static final Logger log = LoggerFactory.getLogger(HalApiInvocationHandler.class);

  private final HalResource contextResource;
  private final Class resourceInterface;
  private final JsonResourceLoader jsonLoader;
  private final RequestMetricsCollector responseMetadata;

  HalApiInvocationHandler(HalResource contextResource, Class resourceInterface, JsonResourceLoader jsonLoader, RequestMetricsCollector responseMetadata) {

    this.contextResource = contextResource;
    this.resourceInterface = resourceInterface;
    this.jsonLoader = jsonLoader;
    this.responseMetadata = responseMetadata;
  }


  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

    // we want to measure how much time is spent for reflection magic in this proxy
    Stopwatch stopwatch = Stopwatch.createStarted();

    // create an object to help with identifiaction of methods and parameters
    HalApiMethodInvocation invocation = new HalApiMethodInvocation(resourceInterface, method, args);

    try {
      // special handling for createLink()
      if (invocation.isCreateLink()) {
        Link selfLink = contextResource.getLink(StandardRelations.SELF);
        if (selfLink == null) {
          throw new RuntimeException("Error while getting self link from the hal resource of " + invocation.toString() +
              "! Make sure the response contain the self link.");
        }

        return selfLink;
      }

      // special handling for HalApiResourceProxy#getRetrievedJsonContent
      if (invocation.isResourceRepresentation()) {
        return contextResource.getModel();
      }

      // handling of methods annotated with @ResourceProperties
      if (invocation.isResourceProperties()) {
        return handleGetResourceProperties(invocation);
      }

      // handling of methods annotated with @RelatedResource
      if (invocation.isRelatedResource()) {
        return handleGetRelatedResource(invocation);
      }

      // handling of methods annotated with @SelectedLinkedResource
      // TODO: implement selected linked resources
      //      if (invocation.isSelectedLinkedResource()) {
      //        return handleSelectedLinkedResource(invocation);
      //      }

      throw new RuntimeException("The invoked method is not annotated with " + RelatedResource.class.getSimpleName() +
          " or " + ResourceState.class.getSimpleName());

    }
    // CHECKSTYLE:OFF- we really want to catch any possible runtime exceptions here
    catch (RuntimeException e) {
      // CHECKSTYLE:ON
      throw new RuntimeException("The invocation of " + invocation + " failed", e);
    }
    finally {
      // collect the time spend calling all proxy methods during the current request in the HalResponseMetadata object
      responseMetadata.onMethodInvocationFinished(invocation.toString(), stopwatch.elapsed(TimeUnit.MICROSECONDS));
    }
  }


  private Object handleGetResourceProperties(HalApiMethodInvocation invocation) {
    Class<?> resourcePropertiesType = invocation.getResourcePropertiesType();
    log.trace(invocation + " was invoked, method is annotated with @ResourceProperties and returns type " + resourcePropertiesType.getSimpleName());


    // the signature determines whether this object should be wrapped in an Observable
    if (invocation.returnsReactiveType()) {

      // if it is an observable then we have to use the emission type as target of the conversion
      resourcePropertiesType = invocation.getRelatedResourceType();

      // and also wrap the properties in a Single
      return Single.just(convertResourceProperties(resourcePropertiesType));
    }

    // the resource properties are already available in the context resource
    return convertResourceProperties(resourcePropertiesType);
  }

  private Object convertResourceProperties(Class<?> resourcePropertiesType) {

    return contextResource.adaptTo(resourcePropertiesType);
  }

  private Object handleGetRelatedResource(HalApiMethodInvocation invocation) {

    // check which relation should be followed and what type of objects the Observable emits
    String relation = invocation.getRelation();
    Class<?> relatedResourceType = invocation.getRelatedResourceType();

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

      return createObservableFromEmbeddedResources(relatedResourceType, embeddedResources);
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

      // TODO: handle links to bnary resources
      boolean isLinkToBinaryResource = false;

      // BYI-1396 - a workaround to create an observable from a link template without actually fetching it.
      // this will be applied only for relations that have a single-link template, and no parameters are specified
      boolean hasOnlyNullValues = parameters.values().stream().filter(o -> o != null).count() == 0;
      if (links.size() == 1 && links.get(0).getHref().contains("{") && hasParameters && hasOnlyNullValues) {
        return createObservableFromLinkTemplate(relatedResourceType, links.get(0));
      }
      if (isLinkToBinaryResource) {
        return createObservableFromLinkedExternalResources(relatedResourceType, links, parameters);
      }
      else {
        Observable<?> rxLinkedResource = createObservableFromLinkedHalResources(relatedResourceType, links, parameters);

        return convertTo(rxLinkedResource, invocation.getReturnType());
      }
    }
    // if it's not linked and not embedded then just return an empty observable
    else {
      return Observable.empty();
    }
  }

  private static Object convertTo(Observable<?> observable, Class<?> otherType) {
    if (otherType.isAssignableFrom(Single.class)) {
      return observable.singleOrError();
    }
    if (otherType.isAssignableFrom(Maybe.class)) {
      return observable.singleElement();
    }
    return observable;
  }

  /*

  private Object handleSelectedLinkedResource(HalApiMethodInvocation invocation) {

    String illegalArgumentMsg = "Methods that use the " + SelectedLinkedResource.class.getSimpleName() + " annotation must have a single parameter" +
        " of type " + Predicate.class.getName() + "<" + Link.class.getName() + ">";

    // check which relation should be followed and what type of objects the Observable emits
    String relation = invocation.getRelation();
    Class<?> relatedResourceType = invocation.getRelatedResourceType();

    if (relatedResourceType.getAnnotation(HalApiInterface.class) == null) {
      throw new RuntimeException("The " + SelectedLinkedResource.class.getSimpleName() +
          " annotation can currently only be used for links to other HAL APi interfaces");
    }

    if (contextResource.hasEmbedded(relation)) {
      throw new RuntimeException("The " + SelectedLinkedResource.class.getSimpleName() +
          " annotation should only be used for relations that are guaranteed to be *linked*, but the context resource contained *embedded* resources"
          + "with relation " + relation);
    }

    if (invocation.getParameters().size() != 1) {
      throw new IllegalArgumentException(illegalArgumentMsg);
    }
    List<Link> filteredLinks;
    List<Link> links;
    try {
      Predicate<Link> linkFilter = (Predicate<Link>)invocation.getParameters().values().iterator().next();
      links = contextResource.getLinks(relation);
      filteredLinks = links.stream().filter(linkFilter).collect(Collectors.toList());
    }
    catch (RuntimeException ex) {
      throw new IllegalArgumentException("Failed to get or apply the link predicate from " + invocation + ". " + illegalArgumentMsg, ex);
    }

    log.trace(links.size() + " links with relation " + relation + " were found in the context resource, but only " + filteredLinks.size()
        + " were selected by the predicate given in the method invocation");

    return createObservableFromLinkedHalResources(relatedResourceType, filteredLinks, Collections.emptyMap());
  }
  */


  private Observable<?> createObservableFromLinkTemplate(Class<?> relatedResourceType, Link link) {
    return Observable.just(HalClientProxyFactory.createProxyFromUrl(relatedResourceType, link.getHref(), jsonLoader, responseMetadata, link.getTitle()));
  }


  private Observable<?> createObservableFromEmbeddedResources(Class<?> relatedResourceType, List<HalResource> embeddedResources) {
    // if the HAL resources are already embedded then creating the proxy is very simple
    return Observable.fromIterable(embeddedResources)
        .map(hal -> HalClientProxyFactory.createProxyFromHalResource(relatedResourceType, hal, jsonLoader, responseMetadata));
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
        .map(link -> HalClientProxyFactory.createProxyFromUrl(relatedResourceType, link.getHref(), jsonLoader, responseMetadata, link.getTitle()));
  }

  private Observable<?> createObservableFromLinkedHalResources(Class<?> relatedResourceType, List<Link> links, Map<String, Object> parameters) {

    // if the resources are linked, then we have to fetch those resources first

    List<Single<JsonNode>> jsonOutputObservables = links.stream()
        .map(link -> {
          Map<String, Object> effectiveParameters = getEffectiveParameters(parameters);

          return UriTemplate.expand(link.getHref(), effectiveParameters);
        })
        .map(href -> jsonLoader.loadJsonResource(href, responseMetadata))
        .collect(Collectors.toList());

    // use Observable#zip to fetch all those JSONs simultaneously and wait until they have all been retrieved
    Single<List<Object>> zippedObservable = Single.zip(jsonOutputObservables, jsonNodeAsArrayOfObjects -> {

      log.trace("All of " + jsonNodeAsArrayOfObjects.length + " linked resource have been retrieved");

      // then create a list of proxies in the original order of the links
      List<Object> proxies = Stream.of(jsonNodeAsArrayOfObjects)
          .map(jsonNode -> new HalResource(jsonNode))
          .map(hal -> HalClientProxyFactory.createProxyFromHalResource(relatedResourceType, hal, jsonLoader, responseMetadata))
          .collect(Collectors.toList());

      return proxies;
    });

    return zippedObservable.flatMapObservable(listOfProxies -> Observable.fromIterable(listOfProxies));
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

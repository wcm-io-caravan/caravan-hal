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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.damnhandy.uri.template.UriTemplate;

import io.reactivex.Observable;
import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
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

      throw new UnsupportedOperationException("The method " + invocation + " has an invalid emission type " + relatedResourceType.getName() +
          " which does not have a @" + HalApiInterface.class + " annotation.");
    }

    log.trace(invocation + " was invoked, method is annotated with @RelatedResources cur=" + relation + " and returns an Observable<"
        + relatedResourceType.getSimpleName() + ">");

    List<Link> links = filterLinksIfNamedLinkAnnotationWasUsed(invocation, contextResource.getLinks(relation));
    List<HalResource> embeddedResources = contextResource.getEmbedded(relation);

    Observable<Object> rxEmbedded = getEmbedded(invocation, relation, relatedResourceType, embeddedResources, links);
    Observable<Object> rxLinked = getLinked(invocation, relation, relatedResourceType, embeddedResources, links);

    return rxEmbedded.concatWith(rxLinked);
  }


  private Observable<Object> getEmbedded(HalApiMethodInvocation invocation, String relation, Class<?> relatedResourceType, List<HalResource> embeddedResources,
      List<Link> links) {

    log.trace(embeddedResources.size() + " embedded resources with relation " + relation + " were found in the context resource");

    return createObservableFromEmbeddedResources(relatedResourceType, embeddedResources, links, invocation);
  }

  private Observable<Object> getLinked(HalApiMethodInvocation invocation, String relation, Class<?> relatedResourceType, List<HalResource> embeddedResources,
      List<Link> links) {

    log.trace(links.size() + " links with relation " + relation + " were found in the context resource and will be fetched.");

    List<Link> relevantLinks = filterLinksToResourcesThatAreAlreadyEmbedded(links, embeddedResources);

    long numTemplatedLinks = relevantLinks.stream().filter(Link::isTemplated).count();
    Map<String, Object> variables = invocation.getTemplateVariables();

    if (variables.size() > 0) {
      // if null values were specified for all method parameters, we assume that the caller is only interested in the link templates
      if (invocation.isCalledWithOnlyNullParameters()) {
        return createObservableFromLinkTemplates(relatedResourceType, relevantLinks);
      }

      // otherwise we ignore any resolved links, and only consider link templates that are resolved with the callers values
      // (unless there are no link templates present)
      if (numTemplatedLinks != 0) {
        relevantLinks = relevantLinks.stream().filter(Link::isTemplated).collect(Collectors.toList());
      }
    }
    else {
      // if the method being called doesn't contain parameters for template variables, then link templates should be ignored
      // (unless there are only link templates present)
      if (numTemplatedLinks != relevantLinks.size()) {
        relevantLinks = relevantLinks.stream().filter(link -> !link.isTemplated()).collect(Collectors.toList());
      }
    }

    return createObservableFromLinkedHalResources(relatedResourceType, relevantLinks, variables);
  }

  private static List<Link> filterLinksToResourcesThatAreAlreadyEmbedded(List<Link> links, List<HalResource> embeddedResources) {

    Set<String> embeddedHrefs = embeddedResources.stream()
        .map(HalResource::getLink)
        .filter(Objects::nonNull)
        .map(Link::getHref)
        .collect(Collectors.toSet());

    List<Link> relevantLinks = links.stream()
        .filter(link -> !embeddedHrefs.contains(link.getHref()))
        .collect(Collectors.toList());

    return relevantLinks;
  }

  private static List<Link> filterLinksIfNamedLinkAnnotationWasUsed(HalApiMethodInvocation invocation, List<Link> links) {

    String selectedLinkName = invocation.getLinkName();
    if (selectedLinkName == null) {
      return links;
    }

    List<Link> filteredLinks = links.stream()
        .filter(link -> selectedLinkName.equals(link.getName()))
        .collect(Collectors.toList());

    return filteredLinks;
  }

  private Observable<Object> createObservableFromEmbeddedResources(Class<?> relatedResourceType, List<HalResource> embeddedResources, List<Link> links,
      HalApiMethodInvocation invocation) {

    Map<String, Link> linksByHref = links.stream()
        .collect(Collectors.toMap(Link::getHref, link -> link, (l1, l2) -> l1));

    return Observable.fromIterable(embeddedResources)
        // if a @LinkName parameter was used then only consider embedded resources with a self-link that corresponds to the filtered links
        .filter(embedded -> invocation.getLinkName() == null || (embedded.getLink() != null && linksByHref.containsKey(embedded.getLink().getHref())))
        .map(embeddedResource -> {

          // if the embedded resource is also linked, we want to make the original (possibly named) link available
          // for extraction by the ResourceLinkHandler, but otherwise we just use the self link
          Link selfLink = embeddedResource.getLink();
          String selfHref = selfLink != null ? selfLink.getHref() : null;
          Link link = linksByHref.getOrDefault(selfHref, selfLink);

          return HalApiClientProxyFactory.createProxyFromHalResource(relatedResourceType, embeddedResource, link, jsonLoader, metrics);
        });
  }

  private Observable<Object> createObservableFromLinkedHalResources(Class<?> relatedResourceType, List<Link> links, Map<String, Object> parameters) {

    // if the resources are linked, then we have to fetch those resources first
    return Observable.fromIterable(links)
        .map(link -> link.isTemplated() ? expandLinkTemplates(link, parameters) : link)
        .map(link -> HalApiClientProxyFactory.createProxyFromLink(relatedResourceType, link, jsonLoader, metrics));
  }

  private Observable<Object> createObservableFromLinkTemplates(Class<?> relatedResourceType, List<Link> links) {

    // do not expand the link templates
    return Observable.fromIterable(links)
        .map(link -> HalApiClientProxyFactory.createProxyFromLink(relatedResourceType, link, jsonLoader, metrics));
  }

  private static Link expandLinkTemplates(Link link, Map<String, Object> parameters) {

    String uri = UriTemplate.expand(link.getHref(), parameters);

    Link clonedLink = new Link(link.getModel().deepCopy());
    clonedLink.setTemplated(false);
    clonedLink.setHref(uri);
    return clonedLink;
  }

}

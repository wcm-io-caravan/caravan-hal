/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2019 wcm.io
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
package io.wcm.caravan.hal.microservices.impl.links;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.damnhandy.uri.template.UriTemplate;
import com.damnhandy.uri.template.UriTemplateBuilder;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import io.wcm.caravan.hal.microservices.api.server.LinkBuilder;
import io.wcm.caravan.hal.microservices.api.server.LinkTemplateComponentProvider;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.resource.Link;

public class LinkBuilderImpl implements LinkBuilder {

  private final String baseUrl;

  private final LinkTemplateComponentProvider componentProvider;

  private Map<String, Object> additionalParameters = new HashMap<>();

  /**
   * @param baseUrl the base path (or full URI) for which the current service bundle is registered
   * @param componentProvider
   */
  public LinkBuilderImpl(String baseUrl, LinkTemplateComponentProvider componentProvider) {

    Preconditions.checkArgument(StringUtils.isNotBlank(baseUrl), "A baseUrl must be provided");
    Preconditions.checkNotNull(componentProvider, "a " + LinkTemplateComponentProvider.class.getSimpleName() + " must be specified");

    this.baseUrl = baseUrl;

    this.componentProvider = componentProvider;
  }

  @Override
  public LinkBuilder withAdditionalParameters(Map<String, Object> parameters) {
    this.additionalParameters = parameters;
    return this;
  }

  /**
   * @param resource the resource instance for which a link should be generated
   * @return a Link instance where the href property is already set
   */
  @Override
  public Link buildLinkTo(LinkableResource resource) {

    // first build the resource path (from the context path and the resource's @Path annotation)
    String resourcePath = buildResourcePath(resource);

    // create a URI template from this path
    UriTemplateBuilder uriTemplateBuilder = UriTemplate.buildFromTemplate(resourcePath);

    // get parameter values from resource, and append query parameter names to the URI template
    Map<String, Object> parametersThatAreSet = collectAndAppendParameters(uriTemplateBuilder, resource);

    // finally build the URI template
    String uriTemplate = uriTemplateBuilder.build().getTemplate();

    // then expand the uri template partially (i.e. keep variables that are null in the resource implementation)
    String partiallyExpandedUriTemplate = UriTemplate.expandPartial(uriTemplate, parametersThatAreSet);

    // and finally construct and return the link
    return new Link(partiallyExpandedUriTemplate);
  }


  /**
   * build the resource path template (by concatenating the baseUrl of the service,
   * and the value of the linked resource's @Path annotation (that may contain path parameter variables)
   * @param resource target resource for the link
   * @return an absolute path template to the resource
   */
  private String buildResourcePath(LinkableResource resource) {

    String pathTemplate = componentProvider.getResourcePathTemplate(resource);

    String resourcePath = baseUrl;
    if (StringUtils.isNotBlank(pathTemplate)) {
      resourcePath += pathTemplate;
    }

    return resourcePath;
  }

  private Map<String, Object> collectAndAppendParameters(UriTemplateBuilder uriTemplateBuilder, LinkableResource resource) {

    // use reflection to find the names and values of all fields annotated with JAX-RS @PathParam and @QueryParam annotations
    Map<String, Object> pathParams = componentProvider.getPathParameters(resource);
    Map<String, Object> queryParams = componentProvider.getQueryParameters(resource);

    // add all parameters specified in #withAdditionalParameters that are not yet included in the template
    ArrayList<String> pathVariables = Lists.newArrayList(uriTemplateBuilder.build().getVariables());
    additionalParameters.forEach((name, value) -> {
      if (!pathVariables.contains(name) && !queryParams.containsKey(name)) {
        queryParams.put(name, value);
      }
    });

    // add all available query parameters to the URI template
    String[] queryParamNames = queryParams.keySet().stream().toArray(String[]::new);
    if (queryParamNames.length > 0) {
      uriTemplateBuilder.query(queryParamNames);
    }

    // now merge the template variables from the query and path parameters
    Map<String, Object> parameters = new HashMap<>();
    parameters.putAll(additionalParameters);
    parameters.putAll(pathParams);
    parameters.putAll(queryParams);

    // and filter only the parameters that have a non-null value
    Map<String, Object> parametersThatAreSet = parameters.entrySet().stream()
        .filter(entry -> entry.getValue() != null)
        .collect(Collectors.toMap(entry -> entry.getKey(), e -> e.getValue()));

    return parametersThatAreSet;
  }
}

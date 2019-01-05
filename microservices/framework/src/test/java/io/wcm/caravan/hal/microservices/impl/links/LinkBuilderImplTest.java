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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;

import io.wcm.caravan.hal.microservices.api.server.LinkBuilder;
import io.wcm.caravan.hal.microservices.api.server.LinkTemplateComponentProvider;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.resource.Link;

@RunWith(MockitoJUnitRunner.class)
public class LinkBuilderImplTest {

  private static final String BASE_URL = "http://server/contextPath";

  @Mock
  private LinkTemplateComponentProvider componentProvider;

  @Mock
  private LinkableResource resource;

  private Link buildLink() {

    LinkBuilder linkBuilder = new LinkBuilderImpl(BASE_URL, componentProvider);
    return linkBuilder.buildLinkTo(resource);
  }


  @Test
  public void resource_path_should_be_appended_to_context_path() throws Exception {

    String relativePath = "relative/path";

    when(componentProvider.getResourcePathTemplate(resource)).thenReturn(relativePath);

    Link link = buildLink();

    assertThat(link.getHref()).isEqualTo(BASE_URL + relativePath);
  }

  @Test
  public void empty_resource_path_should_be_allowed() throws Exception {

    when(componentProvider.getResourcePathTemplate(resource)).thenReturn("");

    Link link = buildLink();

    assertThat(link.getHref()).isEqualTo(BASE_URL);
  }

  @Test
  public void null_resource_path_should_be_allowed() throws Exception {

    when(componentProvider.getResourcePathTemplate(resource)).thenReturn(null);

    Link link = buildLink();

    assertThat(link.getHref()).isEqualTo(BASE_URL);
  }

  private void mockPathParameters(String pathTemplate, String... parameterNamesAndValues) {

    Map<String, Object> parameters = createMapFromPairs(parameterNamesAndValues);

    when(componentProvider.getResourcePathTemplate(resource)).thenReturn(pathTemplate);
    when(componentProvider.getPathParameters(resource)).thenReturn(parameters);
  }

  @Test
  public void path_parameters_should_be_expanded() throws Exception {

    mockPathParameters("/{varA}/{varB}", "varA", "valueA", "varB", "valueB");

    Link link = buildLink();

    assertThat(link.getHref()).isEqualTo(BASE_URL + "/valueA/valueB");
  }

  @Test
  public void path_parameters_should_be_encoded() throws Exception {

    mockPathParameters("/{varA}", "varA", "encode/me:?");

    Link link = buildLink();

    assertThat(link.getHref()).isEqualTo(BASE_URL + "/encode%2Fme%3A%3F");
  }

  @Test
  public void null_values_should_not_be_expanded_in_path_parameters() throws Exception {

    mockPathParameters("/{varA}/{varB}", "varA", null, "varB", "valueB");

    Link link = buildLink();

    assertThat(link.getHref()).isEqualTo(BASE_URL + "/{varA}/valueB");
  }

  @Test
  public void path_parameters_with_only_null_values_should_not_be_expanded() throws Exception {

    mockPathParameters("/{varA}/{varB}", "varA", null, "varB", null);

    Link link = buildLink();

    assertThat(link.getHref()).isEqualTo(BASE_URL + "/{varA}/{varB}");
  }

  private void mockQueryParameters(String... parameterNamesAndValues) {

    mockQueryParameters(createMapFromPairs(parameterNamesAndValues));
  }

  private void mockQueryParameters(Map<String, Object> queryParameters) {

    when(componentProvider.getQueryParameters(resource)).thenReturn(queryParameters);
  }

  @Test
  public void query_parameters_should_be_expanded() throws Exception {

    mockQueryParameters("varA", "valueA", "varB", "valueB");

    Link link = buildLink();

    assertThat(link.getHref()).isEqualTo(BASE_URL + "?varA=valueA&varB=valueB");
  }

  @Test
  public void query_parameters_should_be_encoded() throws Exception {

    mockQueryParameters("varA", "? &/%");

    Link link = buildLink();

    assertThat(link.getHref()).isEqualTo(BASE_URL + "?varA=%3F%20%26%2F%25");
  }

  @Test
  public void null_values_should_not_be_expanded_in_query_parameters() throws Exception {

    mockQueryParameters("varA", "valueA", "varB", null);

    Link link = buildLink();

    assertThat(link.getHref()).isEqualTo(BASE_URL + "?varA=valueA{&varB}");
  }

  @Test
  public void query_parameters_with_only_null_values_should_not_be_expanded() throws Exception {

    mockQueryParameters("varA", null, "varB", null);

    Link link = buildLink();

    assertThat(link.getHref()).isEqualTo(BASE_URL + "{?varA,varB}");
  }

  @Test
  public void additional_query_parameters_should_be_appended_to_other_query_parameters() throws Exception {

    Map<String, Object> additional = ImmutableMap.of("additional", "value");
    mockQueryParameters("varA", "valueA");

    Link link = new LinkBuilderImpl(BASE_URL, componentProvider)
        .withAdditionalParameters(additional)
        .buildLinkTo(resource);

    assertThat(link.getHref()).isEqualTo(BASE_URL + "?varA=valueA&additional=value");
  }

  @Test
  public void additional_query_parameters_should_be_appended_if_other_query_parameter_is_null() throws Exception {

    mockQueryParameters("varA", null);

    Link link = new LinkBuilderImpl(BASE_URL, componentProvider)
        .withAdditionalParameters(ImmutableMap.of("additional", "value"))
        .buildLinkTo(resource);

    assertThat(link.getHref()).isEqualTo(BASE_URL + "{?varA}&additional=value");
  }

  @Test
  public void additional_query_parameters_should_be_appended_if_there_are_no_other_query_parameters() throws Exception {

    Link link = new LinkBuilderImpl(BASE_URL, componentProvider)
        .withAdditionalParameters(ImmutableMap.of("additional", "value"))
        .buildLinkTo(resource);

    assertThat(link.getHref()).isEqualTo(BASE_URL + "?additional=value");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void additional_query_parameters_must_not_have_the_same_name_as_other_query_parameters() throws Exception {

    mockQueryParameters("varA", "valueA");

    new LinkBuilderImpl(BASE_URL, componentProvider)
        .withAdditionalParameters(ImmutableMap.of("varA", "value"))
        .buildLinkTo(resource);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void additional_query_parameters_must_not_have_the_same_name_as_other_path_parameters() throws Exception {

    mockPathParameters("/{varA}", "varA", null);

    new LinkBuilderImpl(BASE_URL, componentProvider)
        .withAdditionalParameters(ImmutableMap.of("varA", "value"))
        .buildLinkTo(resource);
  }

  @After
  public void tearDown() {
    // check that the link builder does not actually call any method on the target resource instance
    verifyZeroInteractions(resource);
  }

  private static Map<String, Object> createMapFromPairs(String... parameterNamesAndValues) {

    assertTrue("you must provide pairs of keys of values in parameterNamesAndValues", parameterNamesAndValues.length % 2 == 0);

    Map<String, Object> parameters = new LinkedHashMap<>();
    for (int i = 0; i < parameterNamesAndValues.length; i += 2) {
      parameters.put(parameterNamesAndValues[i], parameterNamesAndValues[i + 1]);
    }
    return parameters;
  }
}

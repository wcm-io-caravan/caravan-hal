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
package io.wcm.caravan.hal.microservices.impl.renderer;

import static io.wcm.caravan.hal.microservices.api.common.VndErrorRelations.ABOUT;
import static io.wcm.caravan.hal.microservices.api.common.VndErrorRelations.ERRORS;
import static io.wcm.caravan.hal.microservices.impl.renderer.AsyncHalResponseRendererImpl.CARAVAN_METADATA_RELATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.reactivex.Single;
import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.api.annotations.ResourceLink;
import io.wcm.caravan.hal.api.server.testing.LinkableTestResource;
import io.wcm.caravan.hal.microservices.api.client.HalApiClientException;
import io.wcm.caravan.hal.microservices.api.common.HalResponse;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.api.server.AsyncHalResourceRenderer;
import io.wcm.caravan.hal.microservices.api.server.AsyncHalResponseRenderer;
import io.wcm.caravan.hal.microservices.api.server.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

@RunWith(MockitoJUnitRunner.class)
public class AsyncHalResponseRendererImplTest {

  @Mock
  private AsyncHalResourceRenderer renderer;

  @Mock
  private RequestMetricsCollector metrics;

  private ExceptionStatusAndLoggingStrategy statusAndLogging;

  @Mock
  private LinkableTestResource resource;

  private AsyncHalResponseRenderer responseRenderer;


  @HalApiInterface
  interface LinkableResource {

    @ResourceLink
    Link createLink();
  }

  @Before
  public void setUp() {

    statusAndLogging = new ExceptionStatusAndLoggingStrategy() {

      @Override
      public Integer extractStatusCode(Throwable error) {
        if (error instanceof HalApiClientException) {
          return ((HalApiClientException)error).getStatusCode();
        }
        return null;
      }
    };

    responseRenderer = new AsyncHalResponseRendererImpl(renderer, metrics, statusAndLogging);

  }

  private HalResource mockRenderedResource() {
    HalResource hal = new HalResource();
    when(renderer.renderResource(eq(resource))).thenReturn(Single.just(hal));
    return hal;
  }

  private <T extends Exception> T mockExceptionDuringRendering(T exception) {

    when(renderer.renderResource(eq(resource))).thenReturn(Single.error(exception));
    return exception;
  }

  @Test
  public void response_should_have_status_200_if_resource_was_rendered_succesfully() throws Exception {

    mockRenderedResource();

    HalResponse response = responseRenderer.renderResponse(resource).blockingGet();

    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  public void response_should_have_hal_content_type_if_resource_was_rendered_succesfully() throws Exception {

    mockRenderedResource();

    HalResponse response = responseRenderer.renderResponse(resource).blockingGet();

    assertThat(response.getContentType()).isEqualTo("application/hal+json");
  }

  @Test
  public void response_should_contain_hal_resource_from_renderer() throws Exception {

    HalResource hal = mockRenderedResource();

    HalResponse response = responseRenderer.renderResponse(resource).blockingGet();

    assertThat(response.getBody().getModel()).isEqualTo(hal.getModel());
  }

  @Test
  public void response_should_contain_embedded_metadata_from_metrics() throws Exception {

    mockRenderedResource();

    HalResource metadata = new HalResource();
    when(metrics.createMetadataResource(any())).thenReturn(metadata);

    HalResponse response = responseRenderer.renderResponse(resource).blockingGet();

    HalResource hal = response.getBody();
    assertThat(hal.hasEmbedded(CARAVAN_METADATA_RELATION));
    assertThat(hal.getEmbeddedResource(CARAVAN_METADATA_RELATION).getModel()).isEqualTo(metadata.getModel());
  }

  @Test
  public void response_should_allow_null_values_for_max_age_from_metrics() throws Exception {

    mockRenderedResource();
    when(metrics.getOutputMaxAge()).thenReturn(null);
    HalResponse response = responseRenderer.renderResponse(resource).blockingGet();

    assertThat(response.getMaxAge()).isNull();
  }

  @Test
  public void response_should_contain_max_age_from_metrics() throws Exception {

    mockRenderedResource();
    when(metrics.getOutputMaxAge()).thenReturn(99);

    HalResponse response = responseRenderer.renderResponse(resource).blockingGet();

    assertThat(response.getMaxAge()).isEqualTo(99);
  }

  @Test
  public void error_response_should_have_status_code_500_for_runtime_exceptions() {

    mockExceptionDuringRendering(new RuntimeException("Something went wrong"));

    HalResponse response = responseRenderer.renderResponse(resource).blockingGet();

    assertThat(response.getStatus()).isEqualTo(500);
  }

  @Test
  public void error_response_should_use_status_code_from_HalApiClientException() {

    mockExceptionDuringRendering(new HalApiClientException("Something went wrong", 404, "uri"));

    HalResponse response = responseRenderer.renderResponse(resource).blockingGet();

    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  public void error_response_should_have_vnderror_content_type() {

    mockExceptionDuringRendering(new RuntimeException("Something went wrong"));

    HalResponse response = responseRenderer.renderResponse(resource).blockingGet();

    assertThat(response.getContentType()).isEqualTo("application/vnd.error+json");
  }

  @Test
  public void error_response_should_contain_message_from_exception() {

    RuntimeException ex = mockExceptionDuringRendering(new RuntimeException("Something went wrong"));

    HalResponse response = responseRenderer.renderResponse(resource).blockingGet();

    assertThat(response.getBody().getModel().path("message").asText()).isEqualTo(ex.getMessage());
  }

  @Test
  public void error_response_should_contain_embedded_resource_for_cause_of_exception() {

    RuntimeException cause = new RuntimeException("This was the root cause");
    mockExceptionDuringRendering(new RuntimeException("Something went wrong", cause));

    HalResponse response = responseRenderer.renderResponse(resource).blockingGet();

    HalResource vndError = response.getBody();
    assertThat(vndError.hasEmbedded(ERRORS)).isTrue();
    assertThat(vndError.getEmbeddedResource(ERRORS).getModel().path("message").asText()).isEqualTo(cause.getMessage());
  }

  @Test
  public void error_response_should_contain_about_link_to_resource() {

    mockExceptionDuringRendering(new RuntimeException("Something went wrong"));

    Link resourceLink = new Link("/path/to/resource");
    when(resource.createLink()).thenReturn(resourceLink);

    HalResponse response = responseRenderer.renderResponse(resource).blockingGet();

    HalResource vndError = response.getBody();
    assertThat(vndError.hasLink(ABOUT)).isTrue();
    assertThat(vndError.getLink(ABOUT)).isEqualTo(resourceLink);
  }

  @Test
  public void error_response_should_ignore_exception_when_creating_about_link() {

    mockExceptionDuringRendering(new RuntimeException("Something went wrong"));

    when(resource.createLink()).thenThrow(new RuntimeException());

    HalResponse response = responseRenderer.renderResponse(resource).blockingGet();

    HalResource vndError = response.getBody();
    assertThat(vndError.hasLink(ABOUT)).isFalse();
  }

  private RuntimeException createWrappedHalClientExceptionWithVndErrorBody(Integer status, String message) {
    HalResource vndErrorResource = new HalResource();
    vndErrorResource.getModel().put("message", message);

    HalResponse upstreamResponse = new HalResponse()
        .withStatus(status)
        .withBody(vndErrorResource);

    HalApiClientException cause = new HalApiClientException(upstreamResponse, "/some/url", null);
    RuntimeException ex = new RuntimeException(cause);
    return ex;
  }

  @Test
  public void error_response_should_contain_embedded_errors_from_upstream() {

    String upstreamMessage = "Upstream error message";
    RuntimeException ex = createWrappedHalClientExceptionWithVndErrorBody(404, upstreamMessage);

    mockExceptionDuringRendering(ex);

    HalResponse response = responseRenderer.renderResponse(resource).blockingGet();

    HalResource cause = response.getBody().getEmbeddedResource(ERRORS);
    assertThat(cause).isNotNull();

    HalResource rootCause = cause.getEmbeddedResource(ERRORS);
    assertThat(rootCause.getModel().path("message").asText()).isEqualTo(upstreamMessage);
  }

  private RuntimeException createWrappedHalClientExceptionWithoutVndErrorBody(Integer status, String message) {

    RuntimeException rootCause = new RuntimeException(message);

    HalApiClientException cause = new HalApiClientException("Failed to load resource", 500, "/some/url", rootCause);
    RuntimeException ex = new RuntimeException(cause);
    return ex;
  }

  @Test
  public void error_response_should_contain_root_cause_if_no_body_available_from_upstream() {

    String upstreamMessage = "Upstream error message";
    RuntimeException ex = createWrappedHalClientExceptionWithoutVndErrorBody(500, upstreamMessage);

    mockExceptionDuringRendering(ex);

    HalResponse response = responseRenderer.renderResponse(resource).blockingGet();

    List<HalResource> causes = response.getBody().getEmbedded(ERRORS);
    assertThat(causes).hasSize(2);
    assertThat(causes.get(1).getModel().path("message").asText()).isEqualTo(upstreamMessage);
  }
}

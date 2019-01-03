package io.wcm.caravan.hal.it.extensions;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.reactivex.Single;
import io.reactivex.subjects.SingleSubject;
import io.wcm.caravan.hal.it.TestEnvironmentConstants;
import io.wcm.caravan.hal.microservices.api.client.BinaryResourceLoader;
import io.wcm.caravan.hal.microservices.api.client.HalApiClient;
import io.wcm.caravan.hal.microservices.api.client.HalApiClientException;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.HalResponse;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.resource.HalResource;

public class HalApiClientExtension implements ParameterResolver {

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return parameterContext.getParameter().getType().equals(HalApiClient.class);
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {

    ApacheAsyncJsonResourceLoader loader = new ApacheAsyncJsonResourceLoader();

    RequestMetricsCollector metrics = RequestMetricsCollector.create();

    return HalApiClient.create(loader, loader, metrics);
  }

  private static class ApacheAsyncJsonResourceLoader implements JsonResourceLoader, BinaryResourceLoader {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final JsonFactory JSON_FACTORY = new JsonFactory(OBJECT_MAPPER);

    private static final CloseableHttpAsyncClient HTTP_CLIENT = HttpAsyncClientBuilder.create().build();

    static {
      HTTP_CLIENT.start();
    }

    @Override
    public Single<HalResponse> loadJsonResource(String uri) {

      SingleSubject<HalResponse> responseSubject = SingleSubject.create();

      HttpGet request = createRequest(uri);
      HTTP_CLIENT.execute(request, new FutureCallback<HttpResponse>() {

        @Override
        public void failed(Exception ex) {
          responseSubject.onError(new HalApiClientException("HTTP request failed", null, uri, ex));
        }

        @Override
        public void completed(HttpResponse result) {

          JsonNode json = parseJson(result);
          HalResource hal = new HalResource(json);

          String contentType = result.getEntity().getContentType().getValue();

          if (!StringUtils.equals(contentType, HalResource.CONTENT_TYPE)) {
            responseSubject.onError(new RuntimeException("Unexpected content type " + contentType));
            return;
          }

          HalResponse response = new HalResponse().withStatus(200).withContentType(contentType).withBody(hal);

          responseSubject.onSuccess(response);

          if (result instanceof CloseableHttpResponse) {
            IOUtils.closeQuietly((CloseableHttpResponse)result);
          }
        }

        @Override
        public void cancelled() {

        }
      });

      return responseSubject;
    }

    private HttpGet createRequest(String uri) {
      try {
        HttpGet request = new HttpGet();
        request.setURI(new URI(TestEnvironmentConstants.SERVER_URL + uri));
        return request;
      }
      catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    }

    private JsonNode parseJson(HttpResponse result) {
      try {
        return JSON_FACTORY.createParser(result.getEntity().getContent()).readValueAsTree();
      }
      catch (UnsupportedOperationException | IOException e) {
        e.printStackTrace();
      }
      return null;
    }

    @Override
    public Single<InputStream> loadBinaryResource(String uri) {
      throw new UnsupportedOperationException("not implemented");
    }
  }
}

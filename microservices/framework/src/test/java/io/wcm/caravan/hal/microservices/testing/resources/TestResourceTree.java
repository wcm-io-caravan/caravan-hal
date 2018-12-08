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
package io.wcm.caravan.hal.microservices.testing.resources;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.damnhandy.uri.template.UriTemplate;
import com.google.common.collect.ImmutableMap;

import io.reactivex.Single;
import io.wcm.caravan.hal.microservices.api.client.HalApiClientException;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.client.JsonResponse;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

public class TestResourceTree implements JsonResourceLoader {

  static final String LINKED_URL_TEMPLATE = "/linked/{index}";
  private static final String ENTRY_POINT_URL = "/";
  private final TestResource entryPoint;

  private final Map<String, TestResource> urlResourceMap = new HashMap<>();
  private final Set<String> urlsThatTriggerException = new HashSet<>();

  private int linkCounter;

  public TestResourceTree() {
    this.entryPoint = new TestResource(this);
    this.assignSelfLink(this.entryPoint);
  }

  void assignSelfLink(TestResource resource) {

    String url = linkCounter == 0 ? ENTRY_POINT_URL : UriTemplate.expand(LINKED_URL_TEMPLATE, ImmutableMap.of("index", linkCounter));
    linkCounter++;

    HalResource hal = resource.asHalResource();
    urlResourceMap.put(url, resource);
    hal.setLink(new Link(url));
  }

  @Override
  public Single<JsonResponse> loadJsonResource(String uri) {

    Link link = new Link(uri);
    if (link.isTemplated()) {
      return Single.error(new HalApiClientException("An unresolved link template was requested", 400, uri));
    }

    TestResource requestedResource = urlResourceMap.get(uri);
    if (requestedResource == null) {
      return Single.error(new HalApiClientException("No resource with path " + uri + " was created by this " + getClass().getSimpleName(), 404, uri));
    }
    if (urlsThatTriggerException.contains(uri)) {
      return Single
          .error(new HalApiClientException("An error loading resource with path " + uri + " as simulated by this " + getClass().getSimpleName(), 500, uri));
    }
    JsonResponse response = new JsonResponse()
        .withStatus(200)
        .withReason("Ok")
        .withBody(requestedResource.asHalResource().getModel())
        .withMaxAge(requestedResource.getMaxAge());

    return Single.just(response);
  }

  public TestResource getEntryPoint() {
    return this.entryPoint;
  }

  public TestResource createEmbedded(String relation) {
    return getEntryPoint().createEmbedded(relation);
  }

  public TestResource createLinked(String relation) {
    return getEntryPoint().createLinked(relation);
  }

  public TestResource createLinked(String relation, String name) {
    return getEntryPoint().createLinked(relation, name);
  }

  public void throwExceptionWhenResolving(TestResource resource) {
    assertThat(resource.getUrl()).isNotNull();
    urlsThatTriggerException.add(resource.getUrl());
  }

}

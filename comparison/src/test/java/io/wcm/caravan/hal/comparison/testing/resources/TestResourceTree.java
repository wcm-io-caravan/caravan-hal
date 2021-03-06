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
package io.wcm.caravan.hal.comparison.testing.resources;

import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hamcrest.Matchers;

import io.wcm.caravan.hal.comparison.HalComparisonSource;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import rx.Single;

public class TestResourceTree implements HalComparisonSource {

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

    String url = linkCounter == 0 ? ENTRY_POINT_URL : "/linked/" + linkCounter;
    linkCounter++;

    HalResource hal = resource.asHalResource();
    urlResourceMap.put(url, resource);
    hal.setLink(new Link(url));
  }

  @Override
  public String getEntryPointUrl() {
    return ENTRY_POINT_URL;
  }

  @Override
  public Single<HalResource> resolveLink(String url) {
    TestResource requestedResource = urlResourceMap.get(url);
    if (requestedResource == null) {
      return Single.error(new RuntimeException("No resource with path " + url + " was created by this " + getClass().getSimpleName()));
    }
    if (urlsThatTriggerException.contains(url)) {
      return Single.error(new RuntimeException("An error loading resource with path " + url + " as simulated by this " + getClass().getSimpleName()));
    }
    return Single.just(requestedResource.asHalResource());
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
    assertThat(resource.getUrl(), Matchers.notNullValue());
    urlsThatTriggerException.add(resource.getUrl());
  }
}

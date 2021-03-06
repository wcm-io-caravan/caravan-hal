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
package io.wcm.caravan.hal.comparison.impl.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;

import io.wcm.caravan.hal.comparison.HalComparisonContext;
import io.wcm.caravan.hal.comparison.impl.PairWithRelation;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

/**
 * An immutable object that specifies in which part of the tree the comparison is currently being executed.
 */
public class HalComparisonContextImpl implements HalComparisonContext {

  private final HalPathImpl halPath;

  private final String expectedUrl;
  private final String actualUrl;

  private final Map<HalPathImpl, HalResource> parentResources;

  /**
   * This constructor shouldn't be used except for creating the initial context for the entry point. From then on, use
   * the #withXyz methods to build a new context
   * @param expectedUrl
   * @param actualUrl
   */
  public HalComparisonContextImpl(String expectedUrl, String actualUrl) {
    this.halPath = new HalPathImpl();
    this.expectedUrl = expectedUrl;
    this.actualUrl = actualUrl;
    this.parentResources = Collections.emptyMap();
  }

  private HalComparisonContextImpl(HalPathImpl halPath, String expectedUrl, String actualUrl, Map<HalPathImpl, HalResource> parentResources) {
    this.halPath = halPath;
    this.expectedUrl = expectedUrl;
    this.actualUrl = actualUrl;
    this.parentResources = ImmutableMap.copyOf(parentResources);
  }

  @Override
  public String getExpectedUrl() {
    return this.expectedUrl;
  }

  @Override
  public String getActualUrl() {
    return this.actualUrl;
  }

  /**
   * @param relation of the linked or embedded resource that is about to be processed
   * @return a new instance with an updated {@link HalPathImpl}
   */
  public HalComparisonContextImpl withAppendedHalPath(String relation, HalResource contextResource) {

    HalPathImpl newHalPath = halPath.append(relation, null, null);
    return new HalComparisonContextImpl(newHalPath, expectedUrl, actualUrl, createNewParentResourceMap(contextResource));
  }

  private Map<HalPathImpl, HalResource> createNewParentResourceMap(HalResource contextResource) {
    Map<HalPathImpl, HalResource> newParents = new HashMap<>(parentResources);
    newParents.put(halPath, contextResource);
    return newParents;
  }

  /**
   * @param pair of resources that is about to be compared
   * @param contextResource the resource from the expected tree that embeds the resource to be compared
   * @return a new instance with an updated {@link HalPathImpl} that adds array indices if required
   */
  public HalComparisonContextImpl withHalPathOfEmbeddedResource(PairWithRelation<HalResource> pair, HalResource contextResource) {

    String relation = pair.getRelation();
    List<HalResource> originalEmbedded = contextResource.getEmbedded(relation);

    Integer originalIndex = null;
    if (originalEmbedded.size() > 1) {
      originalIndex = findOriginalIndex(pair, originalEmbedded);
    }

    HalPathImpl newHalPath = halPath.append(relation, originalIndex, null);
    return new HalComparisonContextImpl(newHalPath, expectedUrl, actualUrl, createNewParentResourceMap(contextResource));
  }


  private Integer findOriginalIndex(PairWithRelation<HalResource> pair, List<HalResource> originalEmbedded) {
    for (int i = 0; i < originalEmbedded.size(); i++) {
      ObjectNode originalModel = originalEmbedded.get(i).getModel();
      if (originalModel == pair.getExpected().getModel()) {
        return i;
      }
    }
    throw new IllegalArgumentException("The resource from the given pair is not actually embedded in the context resource");
  }

  /**
   * @param pair of links that are about to be followed and compared
   * @param contextResource the resource from the expected tree that contains the links
   * @return a new instance with an updated {@link HalPathImpl} that adds array indices if required
   */
  public HalComparisonContextImpl withHalPathOfLinkedResource(PairWithRelation<Link> pair, HalResource contextResource) {

    String relation = pair.getRelation();
    List<Link> originalLinks = contextResource.getLinks(relation);

    Integer originalIndex = null;
    String name = pair.getExpected().getName();
    if (originalLinks.size() > 1) {
      originalIndex = originalLinks.indexOf(pair.getExpected());
    }

    HalPathImpl newHalPath = halPath.append(relation, originalIndex, name);
    Map<HalPathImpl, HalResource> newParents = new HashMap<>(parentResources);
    newParents.put(halPath, contextResource);
    return new HalComparisonContextImpl(newHalPath, expectedUrl, actualUrl, createNewParentResourceMap(contextResource));
  }

  /**
   * @param indexInArray the index of the next array entry to be compared
   * @return a new instance with an updated {@link HalPathImpl} that contains array indices
   */
  public HalComparisonContextImpl withHalPathIndex(int indexInArray) {
    HalPathImpl newHalPath = halPath.replaceHalPathIndex(indexInArray);
    return new HalComparisonContextImpl(newHalPath, expectedUrl, actualUrl, parentResources);
  }

  /**
   * @param fieldName the name of the JSON property to be compared
   * @return a new instance with an updated {@link HalPathImpl}
   */
  public HalComparisonContextImpl withAppendedJsonPath(String fieldName) {
    HalPathImpl newHalPath = halPath.appendJsonPath(fieldName);
    return new HalComparisonContextImpl(newHalPath, expectedUrl, actualUrl, parentResources);
  }

  /**
   * @param indexInArray the index of the next array entry to be compared
   * @return a new instance with an updated {@link HalPathImpl} that contains array indices
   */
  public HalComparisonContextImpl withJsonPathIndex(int indexInArray) {
    HalPathImpl newHalPath = halPath.replaceJsonPathIndex(indexInArray);
    return new HalComparisonContextImpl(newHalPath, expectedUrl, actualUrl, parentResources);
  }

  /**
   * @param newUrl of the expected resource that is about to be loaded
   * @return a new instance with an updated {@link #getExpectedUrl()} value
   */
  public HalComparisonContextImpl withNewExpectedUrl(String newUrl) {
    return new HalComparisonContextImpl(halPath, newUrl, actualUrl, parentResources);
  }

  /**
   * @param newUrl of the actual resource that is about to be loaded
   * @return a new instance with an updated {@link #getActualUrl()} value
   */
  public HalComparisonContextImpl withNewActualUrl(String newUrl) {
    return new HalComparisonContextImpl(halPath, expectedUrl, newUrl, parentResources);
  }

  @Override
  public String getLastRelation() {
    return halPath.getLastRelation();
  }

  @Override
  public List<String> getAllRelations() {
    return halPath.getAllRelations();
  }

  @Override
  public String getLastProperyName() {
    return halPath.getLastProperyName();
  }

  @Override
  public List<String> getAllPropertyNames() {
    return halPath.getAllPropertyNames();
  }

  @Override
  public HalResource getParentResourceWithRelation(String relation) {
    return parentResources.entrySet().stream()
        .filter(entry -> entry.getKey().getLastRelation().equals(relation))
        .sorted(HalComparisonContextImpl::longestHalPathFirstComparator)
        .map(Entry::getValue)
        .findFirst()
        .orElse(null);
  }

  private static int longestHalPathFirstComparator(Entry<HalPathImpl, HalResource> entry1, Entry<HalPathImpl, HalResource> entry2) {
    String firstHalPath = entry1.getKey().toString();
    String secondHalPath = entry2.getKey().toString();

    return -Integer.compare(firstHalPath.length(), secondHalPath.length());
  }

  @Override
  public String toString() {
    return halPath.toString();
  }
}

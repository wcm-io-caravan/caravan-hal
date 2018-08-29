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
package io.wcm.caravan.hal.comparison.impl;

import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.hal.comparison.HalComparisonConfig;
import io.wcm.caravan.hal.comparison.impl.halpath.HalPathImpl;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

/**
 * An immutable object that specifies in which part of the tree the comparison is currently being executed.
 */
public class HalComparisonContext {

  private final HalComparisonConfig config;

  private final HalPathImpl halPath;

  private final String expectedUrl;
  private final String actualUrl;

  protected HalComparisonContext(HalComparisonConfig config, HalPathImpl halPath, String expectedUrl, String actualUrl) {
    this.config = config;
    this.halPath = halPath;
    this.expectedUrl = expectedUrl;
    this.actualUrl = actualUrl;
  }

  public HalComparisonConfig getConfig() {
    return this.config;
  }

  public HalPathImpl getHalPath() {
    return this.halPath;
  }

  public String getExpectedUrl() {
    return this.expectedUrl;
  }

  public String getActualUrl() {
    return this.actualUrl;
  }

  /**
   * @param relation of the linked or embedded resource that is about to be processed
   * @return a new instance with an updated {@link HalPathImpl}
   */
  public HalComparisonContext withAppendedHalPath(String relation) {

    HalPathImpl newHalPath = halPath.append(relation, null, null);
    return new HalComparisonContext(config, newHalPath, expectedUrl, actualUrl);
  }

  /**
   * @param pair of resources that is about to be compared
   * @param contextResource the resource from the expected tree that embeds the resource to be compared
   * @return a new instance with an updated {@link HalPathImpl} that adds array indices if required
   */
  public HalComparisonContext withHalPathOfEmbeddedResource(PairWithRelation<HalResource> pair, HalResource contextResource) {

    String relation = pair.getRelation();
    List<HalResource> originalEmbedded = contextResource.getEmbedded(relation);

    Integer originalIndex = null;
    if (originalEmbedded.size() > 1) {
      originalIndex = findOriginalIndex(pair, originalEmbedded);
    }

    HalPathImpl newHalPath = halPath.append(relation, originalIndex, null);
    return new HalComparisonContext(config, newHalPath, expectedUrl, actualUrl);
  }

  private Integer findOriginalIndex(PairWithRelation<HalResource> pair, List<HalResource> originalEmbedded) {
    for (int i = 0; i < originalEmbedded.size(); i++) {
      ObjectNode originalModel = originalEmbedded.get(i).getModel();
      if (originalModel == pair.getExpected().getModel()) {
        return i;
      }
    }
    return null;
  }

  /**
   * @param pair of links that are about to be followed and compared
   * @param contextResource the resource from the expected tree that contains the links
   * @return a new instance with an updated {@link HalPathImpl} that adds array indices if required
   */
  public HalComparisonContext withHalPathOfLinkedResource(PairWithRelation<Link> pair, HalResource contextResource) {

    String relation = pair.getRelation();
    List<Link> originalLinks = contextResource.getLinks(relation);

    Integer originalIndex = null;
    String name = null;
    if (originalLinks.size() > 1) {
      name = pair.getExpected().getName();
      originalIndex = originalLinks.indexOf(pair.getExpected());
    }

    HalPathImpl newHalPath = halPath.append(relation, originalIndex, name);
    return new HalComparisonContext(config, newHalPath, expectedUrl, actualUrl);
  }

  /**
   * @param fieldName the name of the JSON property to be compared
   * @return a new instance with an updated {@link HalPathImpl}
   */
  public HalComparisonContext withAppendedJsonPath(String fieldName) {
    HalPathImpl newHalPath = halPath.appendJsonPath(fieldName);
    return new HalComparisonContext(config, newHalPath, expectedUrl, actualUrl);
  }

  /**
   * @param indexInArray the index of the next array entry to be compared
   * @return a new instance with an updated {@link HalPathImpl} that contains array indices
   */
  public HalComparisonContext withJsonPathIndex(int indexInArray) {
    HalPathImpl newHalPath = halPath.replaceJsonPathIndex(indexInArray);
    return new HalComparisonContext(config, newHalPath, expectedUrl, actualUrl);
  }

  /**
   * @param newUrl of the expected resource that is about to be loaded
   * @return a new instance with an updated {@link #getExpectedUrl()} value
   */
  public HalComparisonContext withNewExpectedUrl(String newUrl) {
    return new HalComparisonContext(config, halPath, newUrl, actualUrl);
  }

  /**
   * @param newUrl of the actual resource that is about to be loaded
   * @return a new instance with an updated {@link #getActualUrl()} value
   */
  public HalComparisonContext withNewActualUrl(String newUrl) {
    return new HalComparisonContext(config, halPath, expectedUrl, newUrl);
  }
}

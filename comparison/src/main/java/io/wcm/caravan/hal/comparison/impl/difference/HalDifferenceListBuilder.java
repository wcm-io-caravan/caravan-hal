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
package io.wcm.caravan.hal.comparison.impl.difference;

import static io.wcm.caravan.hal.comparison.impl.util.HalJsonConversion.asJson;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;

import io.wcm.caravan.hal.comparison.HalComparisonContext;
import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.HalDifference.ChangeType;
import io.wcm.caravan.hal.comparison.HalDifference.EntityType;
import io.wcm.caravan.hal.comparison.impl.context.HalComparisonContextImpl;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

/**
 * Simplifies creating properly initialised {@link HalDifferenceImpl} instances.
 */
public class HalDifferenceListBuilder {

  private final List<HalDifference> differences = new ArrayList<>();
  private final HalComparisonContextImpl context;

  /**
   * @param context specifies in which part of the tree the comparison is currently being executed
   */
  public HalDifferenceListBuilder(HalComparisonContextImpl context) {
    this.context = context;
  }

  /**
   * @return the list of differences that were previously reported to this instance
   */
  public List<HalDifference> build() {
    return ImmutableList.copyOf(differences);
  }

  /**
   * Clear the list of differences that were previously reported to this instance
   */
  public void clearPreviouslyReported() {
    differences.clear();
  }

  /**
   * Merges the differences reported to this and another builder
   * @param other another builder
   */
  public void addAllFrom(HalDifferenceListBuilder other) {
    differences.addAll(other.build());
  }

  /**
   * Merges the differences reported to this and another builder
   * @param other list of results
   */
  public void addAll(List<HalDifference> other) {
    differences.addAll(other);
  }

  private void addDifference(HalDifference.ChangeType changeType, HalDifference.EntityType entityType,
      String description, JsonNode expectedJson, JsonNode actualJson) {

    HalDifference diff = new HalDifferenceImpl(context,
        changeType, entityType,
        expectedJson, actualJson, description);

    differences.add(diff);
  }

  private void addDifferenceWithNewContext(HalComparisonContext newContext, HalDifference.ChangeType changeType, HalDifference.EntityType entityType,
      String description, JsonNode expectedJson, JsonNode actualJson) {

    HalDifference diff = new HalDifferenceImpl(newContext,
        changeType, entityType,
        expectedJson, actualJson, description);

    differences.add(diff);
  }

  /**
   * @param description
   * @param actual the link that was added
   * @param index within the actual array
   */
  public void reportAdditionalLink(String description, Link actual, int index) {
    HalComparisonContext newContext = context.withHalPathIndex(index);
    addDifferenceWithNewContext(newContext, ChangeType.ADDITIONAL, EntityType.LINK, description, null, asJson(actual));
  }

  /**
   * @param description
   * @param expected the link that was removed
   * @param index within the expected array
   */
  public void reportMissingLink(String description, Link expected, int index) {
    HalComparisonContext newContext = context.withHalPathIndex(index);
    addDifferenceWithNewContext(newContext, ChangeType.MISSING, EntityType.LINK, description, asJson(expected), null);
  }

  /**
   * @param description
   * @param expected the link found in the ground truth resource
   * @param actual the link that was found instead
   */
  public void reportModifiedLink(String description, Link expected, Link actual) {
    addDifference(ChangeType.MODIFIED, EntityType.LINK, description, asJson(expected), asJson(actual));
  }

  /**
   * @param description
   * @param expected the links in their original order
   * @param actual the links in their new order
   */
  public void reportReorderedLinks(String description, Iterable<Link> expected, Iterable<Link> actual) {
    addDifference(ChangeType.REORDERED, EntityType.LINK, description, asJson(expected), asJson(actual));
  }

  /**
   * @param description
   * @param actual the embedded resource that was added
   * @param index within the actual array
   */
  public void reportAdditionalEmbedded(String description, HalResource actual, int index) {
    HalComparisonContext newContext = context.withHalPathIndex(index);
    addDifferenceWithNewContext(newContext, ChangeType.ADDITIONAL, EntityType.EMBEDDED, description, null, asJson(actual));
  }

  /**
   * @param description
   * @param expected the embedded resource that was removed
   * @param index within the actual array
   */
  public void reportMissingEmbedded(String description, HalResource expected, int index) {
    HalComparisonContext newContext = context.withHalPathIndex(index);
    addDifferenceWithNewContext(newContext, ChangeType.MISSING, EntityType.EMBEDDED, description, asJson(expected), null);
  }

  /**
   * @param description
   * @param expected the embedded resource in the ground truth resource
   * @param actual the embedded resource that was found instead
   */
  public void reportModifiedEmbedded(String description, HalResource expected, HalResource actual) {
    addDifference(ChangeType.MODIFIED, EntityType.EMBEDDED, description, asJson(expected), asJson(actual));
  }

  /**
   * @param description
   * @param expected the embedded resources in their original order
   * @param actual the embedded resources in their new order
   */
  public void reportReorderedEmbedded(String description, Iterable<HalResource> expected, Iterable<HalResource> actual) {
    addDifference(ChangeType.REORDERED, EntityType.EMBEDDED, description, asJson(expected), asJson(actual));
  }

  /**
   * @param description
   * @param actual the property that was added
   */
  public void reportAdditionalProperty(String description, JsonNode actual) {
    addDifference(ChangeType.ADDITIONAL, EntityType.PROPERTY, description, null, actual);
  }

  /**
   * @param description
   * @param actual the property that was added
   * @param index within the actual array
   */
  public void reportAdditionalProperty(String description, JsonNode actual, int index) {
    HalComparisonContext newContext = context.withJsonPathIndex(index);
    addDifferenceWithNewContext(newContext, ChangeType.ADDITIONAL, EntityType.PROPERTY, description, null, actual);
  }

  /**
   * @param description
   * @param expected the property that was removed
   */
  public void reportMissingProperty(String description, JsonNode expected) {
    addDifference(ChangeType.MISSING, EntityType.PROPERTY, description, expected, null);
  }

  /**
   * @param description
   * @param expected the property that was removed
   * @param index within the expected array
   */
  public void reportMissingProperty(String description, JsonNode expected, int index) {
    HalComparisonContext newContext = context.withJsonPathIndex(index);
    addDifferenceWithNewContext(newContext, ChangeType.MISSING, EntityType.PROPERTY, description, expected, null);
  }

  /**
   * @param description
   * @param expected the property from the ground truth resource
   * @param actual the property that was found instead
   */
  public void reportModifiedProperty(String description, JsonNode expected, JsonNode actual) {
    addDifference(ChangeType.MODIFIED, EntityType.PROPERTY, description, expected, actual);
  }

  /**
   * @param description
   * @param expected the properties in their original order
   * @param actual the properties in their new order
   */
  public void reportReorderedProperty(String description, JsonNode expected, JsonNode actual) {
    addDifference(ChangeType.REORDERED, EntityType.PROPERTY, description, expected, actual);
  }

}

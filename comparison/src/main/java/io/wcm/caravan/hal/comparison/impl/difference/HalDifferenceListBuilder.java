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
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

public class HalDifferenceListBuilder {

  private final List<HalDifference> differences = new ArrayList<>();
  private final HalComparisonContext context;

  public HalDifferenceListBuilder(HalComparisonContext context) {
    this.context = context;
  }

  public List<HalDifference> build() {
    return ImmutableList.copyOf(differences);
  }

  public void clearPreviouslyReported() {
    differences.clear();
  }

  public void addAllFrom(HalDifferenceListBuilder other) {
    differences.addAll(other.build());
  }


  private void addDifference(HalDifference.ChangeType changeType, HalDifference.EntityType entityType,
      String description, JsonNode expectedJson, JsonNode actualJson) {

    HalDifference diff = new HalDifferenceImpl(context,
        changeType, entityType,
        expectedJson, actualJson, description);

    differences.add(diff);
  }

  public void reportAdditionalLink(String description, Link actual) {
    addDifference(ChangeType.ADDITIONAL, EntityType.LINK, description, null, asJson(actual));
  }

  public void reportAdditionalLinks(String description, Iterable<Link> expected, Iterable<Link> actual) {
    addDifference(ChangeType.ADDITIONAL, EntityType.LINK, description, asJson(expected), asJson(actual));
  }

  public void reportMissingLink(String description, Link expected) {
    addDifference(ChangeType.MISSING, EntityType.LINK, description, asJson(expected), null);
  }

  public void reportMissingLinks(String description, Iterable<Link> expected, Iterable<Link> actual) {
    addDifference(ChangeType.MISSING, EntityType.LINK, description, asJson(expected), asJson(actual));
  }

  public void reportModifiedLink(String description, Link expected, Link actual) {
    addDifference(ChangeType.MODIFIED, EntityType.LINK, description, asJson(expected), asJson(actual));
  }

  public void reportReorderedLinks(String description, Iterable<Link> expected, Iterable<Link> actual) {
    addDifference(ChangeType.REORDERED, EntityType.LINK, description, asJson(expected), asJson(actual));
  }


  public void reportAdditionalEmbedded(String description, HalResource actual) {
    addDifference(ChangeType.ADDITIONAL, EntityType.EMBEDDED, description, null, asJson(actual));
  }

  public void reportAdditionalEmbedded(String description, Iterable<HalResource> expected, Iterable<HalResource> actual) {
    addDifference(ChangeType.ADDITIONAL, EntityType.EMBEDDED, description, asJson(expected), asJson(actual));
  }

  public void reportMissingEmbedded(String description, HalResource expected) {
    addDifference(ChangeType.MISSING, EntityType.EMBEDDED, description, asJson(expected), null);
  }

  public void reportMissingEmbedded(String description, Iterable<HalResource> expected, Iterable<HalResource> actual) {
    addDifference(ChangeType.MISSING, EntityType.EMBEDDED, description, asJson(expected), asJson(actual));
  }

  public void reportModifiedEmbedded(String description, HalResource expected, HalResource actual) {
    addDifference(ChangeType.MODIFIED, EntityType.EMBEDDED, description, asJson(expected), asJson(actual));
  }

  public void reportReorderedEmbedded(String description, Iterable<HalResource> expected, Iterable<HalResource> actual) {
    addDifference(ChangeType.REORDERED, EntityType.EMBEDDED, description, asJson(expected), asJson(actual));
  }


  public void reportAdditionalProperty(String description, JsonNode actual) {
    addDifference(ChangeType.ADDITIONAL, EntityType.PROPERTY, description, null, actual);
  }

  public void reportAdditionalProperty(String description, JsonNode expected, JsonNode actual) {
    addDifference(ChangeType.ADDITIONAL, EntityType.PROPERTY, description, expected, actual);
  }

  public void reportMissingProperty(String description, JsonNode expected) {
    addDifference(ChangeType.MISSING, EntityType.PROPERTY, description, expected, null);
  }

  public void reportMissingProperty(String description, JsonNode expected, JsonNode actual) {
    addDifference(ChangeType.MISSING, EntityType.PROPERTY, description, expected, actual);
  }

  public void reportModifiedProperty(String description, JsonNode expected, JsonNode actual) {
    addDifference(ChangeType.MODIFIED, EntityType.PROPERTY, description, expected, actual);
  }

  public void reportReorderedProperty(String description, JsonNode expected, JsonNode actual) {
    addDifference(ChangeType.REORDERED, EntityType.PROPERTY, description, expected, actual);
  }

}

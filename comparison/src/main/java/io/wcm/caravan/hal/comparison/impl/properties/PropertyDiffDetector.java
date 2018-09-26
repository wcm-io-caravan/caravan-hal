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
package io.wcm.caravan.hal.comparison.impl.properties;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.impl.context.HalComparisonContextImpl;
import io.wcm.caravan.hal.comparison.impl.difference.HalDifferenceListBuilder;
import io.wcm.caravan.hal.comparison.impl.matching.MatchingResult;
import io.wcm.caravan.hal.comparison.impl.matching.SimpleIdMatchingAlgorithm;
import io.wcm.caravan.hal.comparison.impl.util.HalJsonConversion;
import io.wcm.caravan.hal.resource.HalResource;

/**
 * Implements the comparison of a {@link HalResource} *state* (i.e. all nested JSON properties, except for _links or
 * _embedded)
 */
public class PropertyDiffDetector implements PropertyProcessing {

  @Override
  public List<HalDifference> process(HalComparisonContextImpl context, HalResource expected, HalResource actual) {

    ObjectNode expectedJson = HalJsonConversion.cloneAndStripHalProperties(expected);
    ObjectNode actualJson = HalJsonConversion.cloneAndStripHalProperties(actual);

    HalDifferenceListBuilder diffs = new HalDifferenceListBuilder(context);

    AtomicInteger nodeCounter = new AtomicInteger();
    compareObjects(context, expectedJson, actualJson, nodeCounter, diffs);

    // if there only a few differences in the individual properties, one HalDifference is emitted for each difference
    int numPropertyDifferences = diffs.build().size();
    if (numPropertyDifferences < 3 || numPropertyDifferences <= nodeCounter.get() / 2) {
      return diffs.build();
    }

    // if there are more differences, then it might be a completely different object,
    // so just a single HalDifference is emitted
    diffs.clearPreviouslyReported();

    String msg = numPropertyDifferences + " of " + nodeCounter.get() + " JSON nodes are different";
    diffs.reportModifiedProperty(msg, expectedJson, actualJson);
    return diffs.build();
  }

  private void compareObjects(HalComparisonContextImpl context, ObjectNode expectedJson, ObjectNode actualJson, AtomicInteger nodeCounter,
      HalDifferenceListBuilder diffs) {

    Set<String> comparedFieldNames = new HashSet<>();

    Iterator<String> fieldNameIt = expectedJson.fieldNames();
    while (fieldNameIt.hasNext()) {
      String fieldName = fieldNameIt.next();

      HalComparisonContextImpl newContext = context.withAppendedJsonPath(fieldName);
      JsonNode expectedValue = expectedJson.path(fieldName);
      JsonNode actualValue = actualJson.path(fieldName);

      HalDifferenceListBuilder newDiffs = new HalDifferenceListBuilder(newContext);
      compareValues(newContext, expectedValue, actualValue, nodeCounter, newDiffs);
      diffs.addAllFrom(newDiffs);

      comparedFieldNames.add(fieldName);
    }

    Iterator<String> actualFieldNameIt = actualJson.fieldNames();
    while (actualFieldNameIt.hasNext()) {
      String fieldName = actualFieldNameIt.next();
      if (!comparedFieldNames.contains(fieldName)) {
        HalComparisonContextImpl newContext = context.withAppendedJsonPath(fieldName);
        HalDifferenceListBuilder newDiffs = new HalDifferenceListBuilder(newContext);

        newDiffs.reportAdditionalProperty("An additional property " + fieldName + " was found in the actual resource", actualJson.get(fieldName));
        diffs.addAllFrom(newDiffs);
      }
    }

  }

  private void compareValues(HalComparisonContextImpl context, JsonNode expectedValue, JsonNode actualValue, AtomicInteger nodeCounter,
      HalDifferenceListBuilder diffs) {

    nodeCounter.incrementAndGet();

    if (actualValue.isMissingNode()) {
      diffs.reportMissingProperty("Expected property is missing", expectedValue);
    }
    else if (actualValue.getNodeType() != expectedValue.getNodeType()) {
      String msg = "Expected property of type " + expectedValue.getNodeType().name() + ", but found " + actualValue.getNodeType().name();
      diffs.reportModifiedProperty(msg, expectedValue, actualValue);
    }
    else if (expectedValue.isObject()) {
      compareObjects(context, (ObjectNode)expectedValue, (ObjectNode)actualValue, nodeCounter, diffs);
    }
    else if (expectedValue.isArray()) {
      List<HalDifference> diffWithMatching = compareArrayValuesWithMatching(context, expectedValue, actualValue);
      List<HalDifference> diffInplace = compareArrayValuesInplace(context, expectedValue, actualValue, nodeCounter);

      if (diffWithMatching.size() < diffInplace.size()) {
        diffs.addAll(diffWithMatching);
      }
      else {
        diffs.addAll(diffInplace);
      }
    }
    else if (!expectedValue.equals(actualValue)) {
      String msg = "Expected value '" + StringUtils.abbreviate(expectedValue.asText(), 40) + "',"
          + " but found '" + StringUtils.abbreviate(actualValue.asText(), 40) + "'";
      diffs.reportModifiedProperty(msg, expectedValue, actualValue);
    }
  }

  private List<HalDifference> compareArrayValuesInplace(HalComparisonContextImpl context, JsonNode expectedValue, JsonNode actualValue,
      AtomicInteger nodeCounter) {

    HalDifferenceListBuilder diffs = new HalDifferenceListBuilder(context);
    int numExpected = expectedValue.size();
    int numActual = actualValue.size();

    for (int i = 0; i < numExpected && i < numActual; i++) {
      HalComparisonContextImpl newContext = context.withJsonPathIndex(i);
      HalDifferenceListBuilder newDiffs = new HalDifferenceListBuilder(newContext);
      compareValues(newContext, expectedValue.get(i), actualValue.get(i), nodeCounter, newDiffs);
      diffs.addAllFrom(newDiffs);
    }

    if (numExpected != numActual) {
      if (numExpected > numActual) {
        for (int i = numActual; i < numExpected; i++) {
          reportMissingArrayElement(context, diffs, expectedValue.get(i), i);
        }
      }
      else {
        for (int i = numExpected; i < numActual; i++) {
          reportAdditionalArrayElement(context, diffs, actualValue.get(i), i);
        }
      }
    }

    return diffs.build();
  }

  private List<HalDifference> compareArrayValuesWithMatching(HalComparisonContextImpl context, JsonNode expected, JsonNode actual) {

    HalDifferenceListBuilder diffs = new HalDifferenceListBuilder(context);

    SimpleIdMatchingAlgorithm<JsonNode> algorithm = new SimpleIdMatchingAlgorithm<>(json -> json.toString());

    ArrayList<JsonNode> expectedList = Lists.newArrayList(expected);
    ArrayList<JsonNode> actualList = Lists.newArrayList(actual);

    MatchingResult<JsonNode> matchingResult = algorithm.findMatchingItems(expectedList, actualList);

    boolean reorderingRequired = matchingResult.areMatchesReordered();

    if (reorderingRequired) {
      String msg = "The array elements have a different order in the actual resource";
      diffs.reportReorderedProperty(msg, expected, actual);
    }

    for (JsonNode removed : matchingResult.getRemovedExpected()) {
      int index = expectedList.indexOf(removed);
      reportMissingArrayElement(context, diffs, removed, index);
    }

    for (JsonNode added : matchingResult.getAddedActual()) {
      int index = actualList.indexOf(added);
      reportAdditionalArrayElement(context, diffs, added, index);
    }

    return diffs.build();
  }

  private static void reportAdditionalArrayElement(HalComparisonContextImpl context, HalDifferenceListBuilder diffs, JsonNode added, int index) {
    String msg = "An additional array element " + added.toString() + " is present in the actual resource";
    diffs.reportAdditionalProperty(msg, added, index);
  }

  private static void reportMissingArrayElement(HalComparisonContextImpl context, HalDifferenceListBuilder diffs, JsonNode removed, int index) {
    String msg = "An array element " + removed.toString() + " is missing in the actual resource";
    diffs.reportMissingProperty(msg, removed, index);
  }

}

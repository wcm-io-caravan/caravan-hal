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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;

import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.impl.HalDifferenceImpl;
import io.wcm.caravan.hal.comparison.impl.context.HalComparisonContextImpl;
import io.wcm.caravan.hal.comparison.impl.util.HalStringConversion;
import io.wcm.caravan.hal.resource.HalResource;

/**
 * Implements the comparison of a {@link HalResource} *state* (i.e. all nested JSON properties, except for _links or
 * _embedded)
 */
public class PropertyDiffDetector implements PropertyProcessing {

  @Override
  public List<HalDifference> process(HalComparisonContextImpl context, HalResource expected, HalResource actual) {

    ObjectNode expectedJson = HalStringConversion.cloneAndStripHalProperties(expected.getModel());
    ObjectNode actualJson = HalStringConversion.cloneAndStripHalProperties(actual.getModel());

    AtomicInteger nodeCounter = new AtomicInteger();
    List<HalDifference> allDiffs = compareObjects(context, expectedJson, actualJson, nodeCounter);

    // if there are less than three differences in the individual properties, one HalDifference is emitted for each difference
    if (allDiffs.size() < 3) {
      return allDiffs;
    }

    // if there are more differences, then it might be a completely different object,
    // so just a single HalDifference is emitted
    String msg = allDiffs.size() + " of " + nodeCounter.get() + " JSON nodes are different";
    HalDifferenceImpl result = new HalDifferenceImpl(context, expectedJson.toString(), actualJson.toString(), msg);
    return ImmutableList.of(result);
  }

  private List<HalDifference> compareObjects(HalComparisonContextImpl context, ObjectNode expectedJson, ObjectNode actualJson, AtomicInteger nodeCounter) {

    List<HalDifference> diffs = new ArrayList<>();

    Iterator<String> fieldNameIt = expectedJson.fieldNames();
    while (fieldNameIt.hasNext()) {
      String fieldName = fieldNameIt.next();

      HalComparisonContextImpl newContext = context.withAppendedJsonPath(fieldName);
      JsonNode expectedValue = expectedJson.path(fieldName);
      JsonNode actualValue = actualJson.path(fieldName);

      diffs.addAll(compareValues(newContext, expectedValue, actualValue, nodeCounter));
    }

    return diffs;
  }

  private List<HalDifference> compareValues(HalComparisonContextImpl context, JsonNode expectedValue, JsonNode actualValue, AtomicInteger nodeCounter) {

    nodeCounter.incrementAndGet();

    List<HalDifference> results = new ArrayList<>();
    if (actualValue.isMissingNode()) {
      results.add(new HalDifferenceImpl(context, expectedValue.toString(), null, "Expected property is missing"));
    }
    else if (actualValue.getNodeType() != expectedValue.getNodeType()) {
      results.add(new HalDifferenceImpl(context, expectedValue.toString(), actualValue.toString(),
          "Expected property of type " + expectedValue.getNodeType().name() + ", but found " + actualValue.getNodeType().name()));
    }
    else if (expectedValue.isObject()) {
      results.addAll(compareObjects(context, (ObjectNode)expectedValue, (ObjectNode)actualValue, nodeCounter));
    }
    else if (expectedValue.isArray()) {
      int numExpected = expectedValue.size();
      int numActual = actualValue.size();

      if (numExpected != numActual) {
        results.add(new HalDifferenceImpl(context, expectedValue.toString(), actualValue.toString(),
            "Expected array with " + numExpected + " elements, but found " + numActual));
      }
      for (int i = 0; i < numExpected && i < numActual; i++) {
        HalComparisonContextImpl newContext = context.withJsonPathIndex(i);
        results.addAll(compareValues(newContext, expectedValue.get(i), actualValue.get(i), nodeCounter));
      }
    }
    else if (!expectedValue.equals(actualValue)) {
      results.add(new HalDifferenceImpl(context, expectedValue.toString(), actualValue.toString(),
          "Expected value '" + StringUtils.abbreviate(expectedValue.asText(), 40) + "',"
              + " but found '" + StringUtils.abbreviate(actualValue.asText(), 40) + "'"));
    }
    return results;
  }

}

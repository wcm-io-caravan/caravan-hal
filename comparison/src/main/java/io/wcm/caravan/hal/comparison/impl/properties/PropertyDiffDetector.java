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

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.impl.context.HalComparisonContextImpl;
import io.wcm.caravan.hal.comparison.impl.difference.HalDifferenceListBuilder;
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

    HalDifferenceListBuilder diffs = new HalDifferenceListBuilder(context);

    AtomicInteger nodeCounter = new AtomicInteger();
    compareObjects(context, expectedJson, actualJson, nodeCounter, diffs);

    // if there are less than three differences in the individual properties, one HalDifference is emitted for each difference
    int numPropertyDifferences = diffs.build().size();
    if (numPropertyDifferences < 3) {
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

    Iterator<String> fieldNameIt = expectedJson.fieldNames();
    while (fieldNameIt.hasNext()) {
      String fieldName = fieldNameIt.next();

      HalComparisonContextImpl newContext = context.withAppendedJsonPath(fieldName);
      JsonNode expectedValue = expectedJson.path(fieldName);
      JsonNode actualValue = actualJson.path(fieldName);

      HalDifferenceListBuilder newDiffs = new HalDifferenceListBuilder(newContext);
      compareValues(newContext, expectedValue, actualValue, nodeCounter, newDiffs);
      diffs.addAllFrom(newDiffs);
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
      int numExpected = expectedValue.size();
      int numActual = actualValue.size();

      if (numExpected != numActual) {
        String msg = "Expected array with " + numExpected + " elements, but found " + numActual;
        if (numExpected > numActual) {
          diffs.reportMissingProperty(msg, expectedValue, actualValue);
        }
        else {
          diffs.reportAdditionalProperty(msg, expectedValue, actualValue);
        }
      }
      for (int i = 0; i < numExpected && i < numActual; i++) {
        HalComparisonContextImpl newContext = context.withJsonPathIndex(i);
        HalDifferenceListBuilder newDiffs = new HalDifferenceListBuilder(newContext);
        compareValues(newContext, expectedValue.get(i), actualValue.get(i), nodeCounter, newDiffs);
        diffs.addAllFrom(newDiffs);
      }
    }
    else if (!expectedValue.equals(actualValue)) {
      String msg = "Expected value '" + StringUtils.abbreviate(expectedValue.asText(), 40) + "',"
          + " but found '" + StringUtils.abbreviate(actualValue.asText(), 40) + "'";
      diffs.reportModifiedProperty(msg, expectedValue, actualValue);
    }
  }

}

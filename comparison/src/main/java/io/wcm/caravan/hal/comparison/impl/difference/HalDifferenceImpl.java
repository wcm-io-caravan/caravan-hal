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

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;

import io.wcm.caravan.hal.comparison.HalComparisonContext;
import io.wcm.caravan.hal.comparison.HalDifference;

/**
 * An immutable implementation of the {@link HalDifference} interface
 */
public class HalDifferenceImpl implements HalDifference {

  private final HalComparisonContext context;

  private final EntityType entityType;
  private final ChangeType changeType;

  private final String description;

  private final JsonNode expectedJson;
  private final JsonNode actualJson;

  /**
   * @param context specifies in which part of the tree the difference was detected
   * @param changeType what kind of modification was detected
   * @param entityType what kind of HAL/JSON element was found to be different
   * @param expectedJson the value that was expected in the ground truth resource tree
   * @param actualJson the actually value found in the resource tree to be compared
   * @param description a human-readable explanation of the difference
   */
  public HalDifferenceImpl(HalComparisonContext context, ChangeType changeType, EntityType entityType, JsonNode expectedJson, JsonNode actualJson,
      String description) {
    this.context = context;
    this.changeType = changeType;
    this.entityType = entityType;
    this.expectedJson = expectedJson;
    this.actualJson = actualJson;
    this.description = description;
  }

  @Override
  public HalComparisonContext getHalContext() {
    return this.context;
  }

  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public JsonNode getExpectedJson() {
    return this.expectedJson;
  }

  @Override
  public JsonNode getActualJson() {
    return this.actualJson;
  }

  /*
   * overriden only to quickly inspect reported differences while debugging
   */
  @Override
  public String toString() {
    return StringUtils.defaultIfEmpty(getDescription(), "") + " @ " + getHalContext().toString();
  }

  @Override
  public ChangeType getChangeType() {
    return this.changeType;
  }

  @Override
  public EntityType getEntityType() {
    return this.entityType;
  }
}

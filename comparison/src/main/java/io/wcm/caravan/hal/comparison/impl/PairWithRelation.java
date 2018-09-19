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

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

/**
 * An immutable result class used to bundle the expected and actual values together with the relation
 * @param <T> typically {@link Link} or {@link HalResource}
 */
public class PairWithRelation<T> {

  private final String relation;
  private final T expected;
  private final T actual;

  /**
   * @param relation of the given objects to their contet resource
   * @param expected value from the ground truth resource tree
   * @param actual value from the resource tree to be compared
   */
  public PairWithRelation(String relation, T expected, T actual) {
    this.relation = relation;
    this.expected = expected;
    this.actual = actual;
  }

  public String getRelation() {
    return this.relation;
  }

  public T getExpected() {
    return this.expected;
  }

  public T getActual() {
    return this.actual;
  }
}

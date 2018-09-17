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

import java.util.List;

import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.impl.context.HalComparisonContextImpl;
import io.wcm.caravan.hal.resource.HalResource;


/**
 * Internal interface that allows to mock/replace the comparison of resource state/properties for unit-tests
 */
public interface PropertyProcessing {

  /**
   * @param context specifies the 'relational' location of the given resources (and provides access to configuration)
   * @param expected the "ground truth" resource
   * @param actual the resource to be compared against the ground truth
   * @return TODO:
   */
  List<HalDifference> process(HalComparisonContextImpl context, HalResource expected, HalResource actual);

}

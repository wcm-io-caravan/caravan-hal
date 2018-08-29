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
package io.wcm.caravan.hal.comparison.impl.embedded;

import io.wcm.caravan.hal.comparison.impl.HalComparisonContext;
import io.wcm.caravan.hal.comparison.impl.ProcessingResult;
import io.wcm.caravan.hal.resource.HalResource;

/**
 * Internal interface that allows to mock/replace the processing of embedded resources for unit-tests
 */
public interface EmbeddedProcessing {

  /**
   * Processing of embedded resources happens before the state of these resources are compared, and consists of the
   * following steps
   * <ul>
   * <li>Filter out resources that shouldn't be compared</li>
   * <li>Find corresponding pairs of embedded resources (e.g. if they have been re-ordered)</li>
   * <li>Collect differences that don't require/allow looking into the resources (e.g. resource has been removed)</li>
   * </ul>
   * @param context specifies in which part of the tree the comparison is currently being executed
   * @param expected the "ground truth" resource containing the embedded resources
   * @param actual the resource to be compared against the ground truth
   * @return a {@link ProcessingResult} instance
   */
  ProcessingResult<HalResource> process(HalComparisonContext context, HalResource expected, HalResource actual);

}

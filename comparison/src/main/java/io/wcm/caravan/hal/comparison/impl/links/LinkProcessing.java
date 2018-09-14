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
package io.wcm.caravan.hal.comparison.impl.links;

import io.wcm.caravan.hal.comparison.impl.HalComparisonContextImpl;
import io.wcm.caravan.hal.comparison.impl.ProcessingResult;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;


/**
 * Internal interface that allows to mock/replace the processing of links for unit-tests
 */
public interface LinkProcessing {

  /**
   * Processing of links happens before the linked resources are loaded and consists of the following steps
   * <ul>
   * <li>Filter out links that shouldn't be followed and compared</li>
   * <li>Find corresponding pairs of links (e.g. if they have been re-ordered)</li>
   * <li>Collect differences that don't require/allow looking into the resources (e.g. link has been removed)</li>
   * </ul>
   * @param context specifies in which part of the tree the comparison is currently being executed
   * @param expected the "ground truth" resource containing the links
   * @param actual the resource to be compared against the ground truth
   * @return a {@link ProcessingResult} instance
   */
  ProcessingResult<Link> process(HalComparisonContextImpl context, HalResource expected, HalResource actual);

}

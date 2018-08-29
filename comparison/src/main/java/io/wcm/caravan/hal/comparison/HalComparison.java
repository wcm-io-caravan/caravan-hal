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
package io.wcm.caravan.hal.comparison;

import org.osgi.annotation.versioning.ProviderType;

import io.wcm.caravan.hal.resource.HalResource;
import rx.Observable;

/**
 * An OSGi service to recursively compare two asynchronously loaded trees of HAL+JSON resources.
 */
@ProviderType
public interface HalComparison {

  /**
   * Recursively crawl and compare the linked and embedded {@link HalResource}s from two different HAL API entry points.
   * @param expected a {@link HalComparisonSource} that provides the ground truth for the comparison
   * @param actual a {@link HalComparisonSource} that provides the resources that should be compared with the expected
   *          resources
   * @param config can be implemented to limit the crawling and comparison depth (e.g. ignore specific link relations)
   * @return an {@link Observable} that emits one {@link HalDifference} object for each difference that was detected
   */
  Observable<HalDifference> compare(HalComparisonSource expected, HalComparisonSource actual, HalComparisonConfig config);
}

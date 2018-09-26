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

import org.osgi.annotation.versioning.ConsumerType;

import io.wcm.caravan.hal.resource.HalResource;
import rx.Single;

/**
 * Defines where the input data for the {@link HalComparison} service is located and how it is
 * retrieved.
 */
@ConsumerType
public interface HalComparisonSource {

  /**
   * Defines the URL of the HAL+JSON resource from where the comparison is started.
   * @return an absolute URI or path (depending on the implementation of {@link #resolveLink(String)}
   */
  String getEntryPointUrl();

  /**
   * Load the HAL+JSON resource from the given URL.
   * @param url an absolute URI or path (depending on how the links are represented in the resources being compared)
   * @return a {@link Single} that emits the {@link HalResource} when it is retrieved (or throws any Exception when it
   *         could not be loaded)
   */
  Single<HalResource> resolveLink(String url);
}

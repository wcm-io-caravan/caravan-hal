/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2015 wcm.io
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
package io.wcm.caravan.hal.docs;

import io.wcm.caravan.hal.resource.util.HalCuriAugmenter;

import java.util.Map;

/**
 * Documentation-related information about a HAL-based RESTful service.
 */
public interface HalServiceInfo {

  /**
   * Get curie link information.
   * @return a map with documented curies as key and the documentation link pattern as value.
   */
  Map<String, String> getCurieLinks();

  /**
   * Get title for link relation.
   * @param rel Link relation
   * @return Title or null if none defined.
   */
  String getLinkRelationTitle(String rel);

  /**
   * Get pre-configured curie augmenter that adds curie links to a given HAL resource.
   * @return HAL curie augmenter
   */
  HalCuriAugmenter getCurieAugmenter();

}

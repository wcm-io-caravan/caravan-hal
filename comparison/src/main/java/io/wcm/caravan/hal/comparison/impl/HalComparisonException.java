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

import io.wcm.caravan.hal.comparison.HalPath;

/**
 * Adds context information to any exception that occurs when loading external resources.
 */
public class HalComparisonException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final HalPath halPath;
  private final String resourceUrl;

  /**
   * @param halPath of the resource that fails to load
   * @param resourceUrl of the resource that contains the link to this resource
   * @param cause the original exception
   */
  public HalComparisonException(HalPath halPath, String resourceUrl, Throwable cause) {
    super("Failed to load resource with HAL path " + halPath + " that was linked from " + resourceUrl, cause);
    this.halPath = halPath;
    this.resourceUrl = resourceUrl;
  }

  public HalPath getHalPath() {
    return this.halPath;
  }

  public String getResourceUrl() {
    return this.resourceUrl;
  }
}

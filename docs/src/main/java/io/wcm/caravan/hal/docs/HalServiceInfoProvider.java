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

import org.osgi.framework.Bundle;

/**
 * Provides a {@link HalServiceInfo} for JAX-RS application bundles.
 */
public interface HalServiceInfoProvider {

  /**
   * Get {@link HalServiceInfo} for the given bundle.
   * @param bundle JAX-RS application bundle
   * @return Service info or null if not docs metadata found or not a JAX-RS application bundle
   */
  HalServiceInfo get(Bundle bundle);

}

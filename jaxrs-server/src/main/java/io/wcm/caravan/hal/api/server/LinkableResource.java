/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2017 wcm.io
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
package io.wcm.caravan.hal.api.server;

import org.osgi.annotation.versioning.ConsumerType;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

/**
 * An interface to be implemented by all resources that should be available via HTTP. If you don't implement this
 * interface in your server-side implementation of a HAL API interface, then that resource can only be embedded
 */
@ConsumerType
public interface LinkableResource {

  /**
   * Create a link to this resource, including meaningful title and name properties where appropriate. If all required
   * parameters of the resource are set, then the link will have a resolved URI as href property. If some or all
   * parameters are null, a link with a URI template is created instead
   * @return a link instance to be added to the context {@link HalResource}
   */
  Link createLink();
}

/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2019 wcm.io
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
package io.wcm.caravan.hal.microservices.orchestrator;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.UriInfo;

import org.osgi.annotation.versioning.ProviderType;

import io.reactivex.Single;
import io.wcm.caravan.hal.microservices.api.server.LinkBuilder;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;

@ProviderType
public interface CaravanJaxRsHalOrchestrator {

  void limitOutputMaxAge(int maxAge);

  <T> Single<T> getEntryPoint(Class<T> halApiInterface);

  LinkBuilder createLinkBuilder();

  void respondWith(UriInfo incomingUri, LinkableResource resourceImpl, AsyncResponse response);

}

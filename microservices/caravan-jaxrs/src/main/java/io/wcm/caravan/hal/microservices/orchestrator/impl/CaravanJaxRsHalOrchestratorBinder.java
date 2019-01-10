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
package io.wcm.caravan.hal.microservices.orchestrator.impl;

import javax.ws.rs.ext.Provider;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import io.wcm.caravan.hal.microservices.orchestrator.CaravanJaxRsHalOrchestrator;
import io.wcm.caravan.jaxrs.publisher.JaxRsComponent;

@Component
@Service(value = JaxRsComponent.class, serviceFactory = true)
@Property(name = JaxRsComponent.PROPERTY_GLOBAL_COMPONENT, value = "true")
@Provider
public class CaravanJaxRsHalOrchestratorBinder extends AbstractBinder implements JaxRsComponent {

  @Override
  protected void configure() {
    bind(CaravanJaxRsHalOrchestratorImpl.class)
        .to(CaravanJaxRsHalOrchestrator.class);
  }
}

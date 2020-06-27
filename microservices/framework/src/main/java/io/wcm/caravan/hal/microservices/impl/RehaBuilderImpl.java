/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2020 wcm.io
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
package io.wcm.caravan.hal.microservices.impl;

import java.util.ArrayList;
import java.util.List;

import io.wcm.caravan.hal.microservices.api.Reha;
import io.wcm.caravan.hal.microservices.api.RehaBuilder;
import io.wcm.caravan.hal.microservices.api.client.HalApiDeveloperException;
import io.wcm.caravan.hal.microservices.api.client.JsonResourceLoader;
import io.wcm.caravan.hal.microservices.api.common.HalApiAnnotationSupport;
import io.wcm.caravan.hal.microservices.api.common.HalApiReturnTypeSupport;
import io.wcm.caravan.hal.microservices.api.common.HalApiTypeSupport;
import io.wcm.caravan.hal.microservices.api.server.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.hal.microservices.impl.reflection.CompositeHalApiTypeSupport;
import io.wcm.caravan.hal.microservices.impl.reflection.DefaultHalApiTypeSupport;
import io.wcm.caravan.hal.microservices.impl.reflection.HalApiTypeSupportAdapter;

public class RehaBuilderImpl implements RehaBuilder {

  private final JsonResourceLoader jsonLoader;
  private final List<HalApiTypeSupport> registeredTypeSupports = new ArrayList<>();

  private ExceptionStatusAndLoggingStrategy exceptionStrategy;

  public RehaBuilderImpl(JsonResourceLoader jsonLoader) {

    this.jsonLoader = jsonLoader;

    this.registeredTypeSupports.add(new DefaultHalApiTypeSupport());
  }

  @Override
  public RehaBuilder withReturnTypeSupport(HalApiReturnTypeSupport additionalTypeSupport) {

    registeredTypeSupports.add(new HalApiTypeSupportAdapter(additionalTypeSupport));
    return this;
  }

  @Override
  public RehaBuilder withAnnotationTypeSupport(HalApiAnnotationSupport additionalTypeSupport) {

    registeredTypeSupports.add(new HalApiTypeSupportAdapter(additionalTypeSupport));
    return this;
  }

  private HalApiTypeSupport getEffectiveTypeSupport() {

    if (registeredTypeSupports.size() == 1) {
      return registeredTypeSupports.get(0);
    }

    return new CompositeHalApiTypeSupport(registeredTypeSupports);
  }

  @Override
  public RehaBuilder withExceptionStrategy(ExceptionStatusAndLoggingStrategy customStrategy) {

    if (exceptionStrategy != null) {
      throw new HalApiDeveloperException("#withExceptionStrategy can only be called once");
    }

    exceptionStrategy = customStrategy;
    return this;
  }

  @Override
  public Reha buildForRequestTo(String incomingRequestUri) {

    HalApiTypeSupport typeSupport = getEffectiveTypeSupport();

    return new RehaImpl(incomingRequestUri, jsonLoader, exceptionStrategy, typeSupport);
  }

  // deprecated

  @Override
  public RehaBuilder withTypeSupport(HalApiTypeSupport additionalTypeSupport) {

    registeredTypeSupports.add(additionalTypeSupport);
    return this;
  }

}

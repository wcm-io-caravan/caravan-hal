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
package io.wcm.caravan.hal.microservices.jaxrs.impl;

import java.util.Map;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import com.google.common.base.Preconditions;

import io.wcm.caravan.hal.microservices.api.server.LinkBuilderSupport;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;

/**
 * Implements the JAX-RS specific logic to automatically build links to JAX-RS resource implementations by scanning the
 * classes for {@link Path}, {@link QueryParam}, {@link BeanParam} and {@link PathParam} annotations
 */
public final class JaxRsLinkBuilderSupport implements LinkBuilderSupport {

  @Override
  public String getResourcePathTemplate(LinkableResource targetResource) {

    Path pathAnnotation = targetResource.getClass().getAnnotation(Path.class);
    Preconditions.checkNotNull(pathAnnotation,
        "A @Path annotation must be present on the resource implementation class (" + targetResource.getClass().getName() + ")");

    return pathAnnotation.value();
  }

  @Override
  public Map<String, Object> getPathParameters(LinkableResource targetResource) {

    return JaxRsReflectionUtils.getPathParameterMap(targetResource, targetResource.getClass());
  }

  @Override
  public Map<String, Object> getQueryParameters(LinkableResource targetResource) {

    return JaxRsReflectionUtils.getQueryParameterMap(targetResource, targetResource.getClass());
  }
}

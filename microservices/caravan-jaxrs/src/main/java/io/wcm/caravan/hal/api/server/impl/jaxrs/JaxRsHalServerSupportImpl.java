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
package io.wcm.caravan.hal.api.server.impl.jaxrs;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import io.wcm.caravan.hal.api.server.jaxrs.AsyncHalResponseHandler;
import io.wcm.caravan.hal.api.server.jaxrs.JaxRsHalServerSupport;
import io.wcm.caravan.hal.api.server.jaxrs.JaxRsLinkBuilder;
import io.wcm.caravan.hal.microservices.api.server.LinkBuilder;
import io.wcm.caravan.jaxrs.publisher.ApplicationPath;
import io.wcm.caravan.jaxrs.publisher.JaxRsComponent;

@Component(service = JaxRsHalServerSupport.class, scope = ServiceScope.BUNDLE)
public class JaxRsHalServerSupportImpl implements JaxRsComponent, JaxRsHalServerSupport {

  private String contextPath;

  @Reference
  private AsyncHalResponseHandler responseHandler;

  @Activate
  void activate(ComponentContext componentCtx) {
    contextPath = ApplicationPath.get(componentCtx.getUsingBundle());
  }

  @Override
  public String getContextPath() {
    return this.contextPath;
  }

  @Override
  public AsyncHalResponseHandler getResponseHandler() {
    return responseHandler;
  }

  @Override
  public LinkBuilder getLinkBuilder() {
    return new JaxRsLinkBuilder(contextPath);
  }

}

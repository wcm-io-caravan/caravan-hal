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
package io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.consumer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;

import io.reactivex.Single;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.ItemResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.ItemState;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.context.ExampleServiceRequestContext;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.resource.Link;

/**
 * An example for a class that uses constructor injection of the context and
 * parameters. The advantage is that you only have a single constructor and
 * final fields, the disadvantage is that the JaxRsLinkBuilder must assume that
 * the fields have the exact same name as the parameters
 */
@Path("/consumer/items/{index}")
public class ConsumerItemResourceImpl implements ItemResource, LinkableResource {

  private final ExampleServiceRequestContext context;

  private final Integer index;
  private final Integer delayMs;

  public ConsumerItemResourceImpl(@Context ExampleServiceRequestContext context, @PathParam("index") Integer index,
      @QueryParam("delayMs") Integer delayMs) {
    this.context = context;
    this.index = index;
    this.delayMs = delayMs;
  }

  @Override
  public Single<ItemState> getProperties() {

    return context.getUpstreamEntryPoint()
        .getCollectionExamples()
        .flatMap(examples -> examples.getItemWithIndex(index, delayMs))
        .flatMap(ItemResource::getProperties);
  }

  @Override
  public Link createLink() {

    String title;
    if (index != null) {
      title = "The item with index " + index;
    }
    else {
      title = "A link template to load an item with a specific index";
    }

    return context.buildLinkTo(this).setTitle(title);
  }

  @GET
  public void get(@Suspended AsyncResponse response) {
    context.respondWith(this, response);
  }

}
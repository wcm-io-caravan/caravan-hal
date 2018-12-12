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
package io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.errors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;

import io.reactivex.Maybe;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.TitledState;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.errors.ErrorResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.context.ExampleServiceRequestContext;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.resource.Link;

@Path("/errors/serverSide")
public class ServerSideErrorResourceImpl implements ErrorResource, LinkableResource {

  private final ExampleServiceRequestContext context;

  private final Integer statusCode;
  private final String message;
  private final Boolean withCause;

  public ServerSideErrorResourceImpl(@Context ExampleServiceRequestContext context,
      @QueryParam("statusCode") Integer statusCode,
      @QueryParam("message") String message,
      @QueryParam("withCause") Boolean withCause) {
    this.context = context;

    this.statusCode = statusCode;
    this.message = message;
    this.withCause = withCause;
  }

  @Override
  public Maybe<TitledState> getState() {

    Exception exception;
    if (withCause != null && withCause.booleanValue()) {
      exception = new WebApplicationException(new RuntimeException(message), statusCode);
    }
    else {
      exception = new WebApplicationException(message, statusCode);
    }

    return Maybe.error(exception);
  }

  @Override
  public Link createLink() {

    return context.buildLinkTo(this)
        .setTitle("Simulate a server-side error with the given status code and message");
  }

  @GET
  public void get(@Suspended AsyncResponse response) {
    context.respondWith(this, response);
  }

}
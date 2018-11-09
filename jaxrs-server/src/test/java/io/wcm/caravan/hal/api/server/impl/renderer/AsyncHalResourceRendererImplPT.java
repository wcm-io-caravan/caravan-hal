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
package io.wcm.caravan.hal.api.server.impl.renderer;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Stopwatch;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.wcm.caravan.hal.api.server.jaxrs.JaxRsLinkBuilder;
import io.wcm.caravan.hal.api.server.testing.LinkableTestResource;
import io.wcm.caravan.hal.api.server.testing.TestResource;
import io.wcm.caravan.hal.api.server.testing.TestState;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;


public class AsyncHalResourceRendererImplPT {

  private JaxRsLinkBuilder linkBuilder = new JaxRsLinkBuilder("/test");

  static HalResource render(Object resourceImplInstance) {

    AsyncHalResourceRendererImpl renderer = new AsyncHalResourceRendererImpl();
    Single<HalResource> rxResource = renderer.renderLinkedOrEmbeddedResource(resourceImplInstance);

    return rxResource.toObservable().blockingFirst();
  }

  @Ignore
  @Test
  public void testLinkedResourcesnPerformance() {

    List<TestResource> itemResources = IntStream.range(0, 100)
        .mapToObj(ItemResource::new)
        .collect(Collectors.toList());

    TestResource collectionResource = new TestResource() {

      @Override
      public Observable<TestResource> getLinked() {
        return Observable.fromIterable(itemResources);
      }
    };

    IntStream.range(0, 30).forEach(i -> {
      Stopwatch sw = Stopwatch.createStarted();
      HalResource hal = render(collectionResource);
      System.out.println(sw.elapsed(TimeUnit.MILLISECONDS) + "ms / " + hal.toString());
    });
  }

  @Path("items")
  private final class ItemResource implements LinkableTestResource {

    @PathParam("index")
    private final int index;
    @QueryParam("text")
    private final String text = "Das ist doch nur ein Test";

    private ItemResource(int i) {
      this.index = i;
    }

    @Override
    public Maybe<TestState> getState() {

      return Maybe.just(new TestState(text));
    }

    @Override
    public Link createLink() {
      return linkBuilder.buildLinkTo(this);
    }
  }

}

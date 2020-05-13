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

import javax.ws.rs.BeanParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.base.Stopwatch;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.api.annotations.RelatedResource;
import io.wcm.caravan.hal.api.annotations.ResourceState;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.api.server.AsyncHalResourceRenderer;
import io.wcm.caravan.hal.microservices.api.server.LinkBuilder;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.microservices.jaxrs.impl.JaxRsLinkBuilderFactory;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;


public class AsyncHalResourceRendererImplPT {

  private LinkBuilder linkBuilder = JaxRsLinkBuilderFactory.createLinkBuilder("/test");

  static HalResource render(LinkableResource resourceImplInstance) {

    RequestMetricsCollector metrics = Mockito.mock(RequestMetricsCollector.class);

    AsyncHalResourceRenderer renderer = AsyncHalResourceRenderer.create(metrics);
    Single<HalResource> rxResource = renderer.renderResource(resourceImplInstance);

    return rxResource.toObservable().blockingFirst();
  }

  @Ignore // this test was useful when optimizing performance of JaxRsLinkBuilderSupport, but doesn't need to be run on each build
  @Test
  public void test_JaxRsLinkBuilderSupport_performance() {

    List<ItemResourceImpl> itemResources = IntStream.range(0, 1000)
        .mapToObj(ItemResourceImpl::new)
        .collect(Collectors.toList());

    CollectionResourceImpl collectionResource = new CollectionResourceImpl(itemResources);

    IntStream.range(0, 100).forEach(i -> {
      Stopwatch sw = Stopwatch.createStarted();
      render(collectionResource);
      System.out.println(sw.elapsed(TimeUnit.MILLISECONDS) + "ms to render resource with " + itemResources.size() + " links");
    });

  }

  @HalApiInterface
  public interface CollectionResource {

    @RelatedResource(relation = TestRelations.LINKED)
    Observable<ItemResource> getLinked();
  }

  @HalApiInterface
  interface ItemResource {

    @ResourceState
    Maybe<TestState> getState();
  }

  @Path("/items")
  public final class CollectionResourceImpl implements CollectionResource, LinkableResource {

    private final List<ItemResourceImpl> itemResources;

    private CollectionResourceImpl(List<ItemResourceImpl> itemResources) {
      this.itemResources = itemResources;
    }

    @Override
    public Observable<ItemResource> getLinked() {
      return Observable.fromIterable(itemResources);
    }

    @Override
    public Link createLink() {
      return linkBuilder.buildLinkTo(this);
    }
  }

  private final class ParameterDto {

    @QueryParam("a")
    private String a;

    @QueryParam("b")
    private String b;

    ParameterDto(String a, String b) {
      this.a = a;
      this.b = b;
    }
  }

  @Path("/items/{index}")
  private final class ItemResourceImpl implements ItemResource, LinkableResource {

    @PathParam("index")
    private final int index;

    @QueryParam("text")
    private final String text = "test";

    @BeanParam
    private final ParameterDto dto;

    private ItemResourceImpl(int i) {
      this.index = i;
      this.dto = new ParameterDto("a" + i, "b" + i);
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

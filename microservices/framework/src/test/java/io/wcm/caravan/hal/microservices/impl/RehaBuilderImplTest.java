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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import io.reactivex.rxjava3.core.Observable;
import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.api.annotations.RelatedResource;
import io.wcm.caravan.hal.api.relations.StandardRelations;
import io.wcm.caravan.hal.microservices.api.Reha;
import io.wcm.caravan.hal.microservices.api.RehaBuilder;
import io.wcm.caravan.hal.microservices.api.common.HalApiReturnTypeSupport;
import io.wcm.caravan.hal.microservices.api.common.HalResponse;
import io.wcm.caravan.hal.microservices.api.server.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.hal.microservices.api.server.HalApiServerException;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.microservices.testing.LinkableTestResource;
import io.wcm.caravan.hal.microservices.testing.resources.TestResourceTree;
import io.wcm.caravan.hal.resource.Link;

@ExtendWith(MockitoExtension.class)
public class RehaBuilderImplTest {

  private static final String UPSTREAM_ENTRY_POINT_URI = "/";
  private static final String NON_EXISTING_PATH = "/does/not/exist";
  private static final String INCOMING_REQUEST_URI = "/incoming";

  private final TestResourceTree upstreamResourceTree = new TestResourceTree();

  private Reha createRehaWithCustomExceptionStrategy() {

    return RehaBuilder.withoutResourceLoader()
        .withExceptionStrategy(new CustomExceptionStrategy())
        .buildForRequestTo(INCOMING_REQUEST_URI);
  }


  @Test
  public void withExceptionStrategy_should_apply_custom_exception_strategy() {

    Reha reha = createRehaWithCustomExceptionStrategy();

    NotImplementedException ex = new NotImplementedException("Foo");

    HalResponse response = reha.renderResponse(new FailingResourceImpl(ex));

    assertThat(response.getStatus()).isEqualTo(501);
  }


  @Test
  public void withExceptionStrategy_should_not_disable_default_exception_strategy() {

    Reha reha = createRehaWithCustomExceptionStrategy();

    HalApiServerException ex = new HalApiServerException(404, "Not Found");

    HalResponse response = reha.renderResponse(new FailingResourceImpl(ex));

    assertThat(response.getStatus()).isEqualTo(404);
  }

  private Reha createRehaWithStreamReturnTypeSupport() {

    return RehaBuilder.withResourceLoader(upstreamResourceTree)
        .withReturnTypeSupport(new StreamSupport())
        .buildForRequestTo(INCOMING_REQUEST_URI);
  }

  @HalApiInterface
  public interface ResourceWithStreamOfLinks extends LinkableResource {

    @RelatedResource(relation = StandardRelations.ITEM)
    Stream<LinkableTestResource> getLinks();
  }

  @Test
  public void withReturnTypeSupport_should_enable_rendering_of_resources_with_custom_return_types() {

    Reha reha = createRehaWithStreamReturnTypeSupport();

    ResourceWithStreamOfLinks resourceImpl = new ResourceWithStreamOfLinks() {

      @Override
      public Stream<LinkableTestResource> getLinks() {
        return Stream.of(new LinkableTestResource() {

          @Override
          public Link createLink() {
            return new Link("/linked");
          }
        });
      }

      @Override
      public Link createLink() {
        return new Link(INCOMING_REQUEST_URI);
      }

    };

    HalResponse response = reha.renderResponse(resourceImpl);
    assertThat(response.getStatus()).isEqualTo(200);

    List<Link> links = response.getBody().getLinks(StandardRelations.ITEM);
    assertThat(links).hasSize(1);
  }

  @Test
  public void withReturnTypeSupport_should_enable_fetching_of_resources_with_custom_return_types() {

    upstreamResourceTree.createLinked(StandardRelations.ITEM);
    upstreamResourceTree.createLinked(StandardRelations.ITEM);

    Reha reha = createRehaWithStreamReturnTypeSupport();

    ResourceWithStreamOfLinks entryPoint = reha.getEntryPoint(UPSTREAM_ENTRY_POINT_URI, ResourceWithStreamOfLinks.class);

    List<LinkableTestResource> linked = entryPoint.getLinks().collect(Collectors.toList());

    assertThat(linked).hasSize(2);
  }

  private static final class CustomExceptionStrategy implements ExceptionStatusAndLoggingStrategy {

    @Override
    public Integer extractStatusCode(Throwable error) {
      if (error instanceof NotImplementedException) {
        return 501;
      }
      return null;
    }
  }

  private static final class FailingResourceImpl implements LinkableTestResource {

    private final RuntimeException ex;

    private FailingResourceImpl(RuntimeException ex) {
      this.ex = ex;
    }

    @Override
    public Link createLink() {
      throw this.ex;
    }
  }

  private static final class StreamSupport implements HalApiReturnTypeSupport {

    @SuppressWarnings("unchecked")
    @Override
    public <T> Function<Observable, T> convertFromObservable(Class<T> targetType) {
      if (targetType.isAssignableFrom(Stream.class)) {
        return obs -> {
          List<?> list = (List<?>)obs.toList().blockingGet();
          return (T)list.stream();
        };
      }
      return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Function<? super Object, Observable<?>> convertToObservable(Class<?> sourceType) {
      if (Stream.class.isAssignableFrom(sourceType)) {
        return o -> Observable.fromStream((Stream)o);
      }
      return null;
    }

  }

}

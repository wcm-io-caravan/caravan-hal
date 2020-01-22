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
package io.wcm.caravan.hal.microservices.impl.renderer;

import static io.wcm.caravan.hal.microservices.impl.renderer.AsyncHalResourceRendererTestUtil.createTestState;
import static io.wcm.caravan.hal.microservices.impl.renderer.AsyncHalResourceRendererTestUtil.render;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.api.annotations.ResourceState;
import io.wcm.caravan.hal.microservices.testing.TestState;
import io.wcm.caravan.hal.resource.HalResource;

public class RenderResourceStateTest {

  @HalApiInterface
  public interface TestResourceWithMaybeState {

    @ResourceState
    Maybe<TestState> getState();
  }

  @Test
  public void maybe_resource_state_should_be_rendered() {

    TestState state = createTestState();

    TestResourceWithMaybeState resourceImpl = new TestResourceWithMaybeState() {

      @Override
      public Maybe<TestState> getState() {

        return Maybe.just(state);
      }

    };

    HalResource hal = render(resourceImpl);

    assertThat(hal.adaptTo(TestState.class)).isEqualToComparingFieldByField(state);
  }


  @HalApiInterface
  public interface TestResourceWithSingleState {

    @ResourceState
    Single<TestState> getState();
  }

  @Test
  public void single_resource_state_should_be_rendered() {

    TestState state = createTestState();

    TestResourceWithSingleState resourceImpl = new TestResourceWithSingleState() {

      @Override
      public Single<TestState> getState() {

        return Single.just(state);
      }

    };

    HalResource hal = render(resourceImpl);

    assertThat(hal.adaptTo(TestState.class)).isEqualToComparingFieldByField(state);
  }

  @HalApiInterface
  public interface TestResourceWithObservableState {

    @ResourceState
    Observable<TestState> getState();
  }

  @Test
  public void observable_resource_state_should_be_rendered() {

    TestState state = createTestState();

    TestResourceWithObservableState resourceImpl = new TestResourceWithObservableState() {

      @Override
      public Observable<TestState> getState() {

        return Observable.just(state);
      }

    };

    HalResource hal = render(resourceImpl);

    assertThat(hal.adaptTo(TestState.class)).isEqualToComparingFieldByField(state);
  }


  @HalApiInterface
  public interface TestResourceWithPublisherState {

    @ResourceState
    Publisher<TestState> getState();
  }

  @Test
  public void publisher_resource_state_should_be_rendered() {

    TestState state = createTestState();

    TestResourceWithPublisherState resourceImpl = new TestResourceWithPublisherState() {

      @Override
      public Publisher<TestState> getState() {

        return Flowable.just(state);
      }

    };

    HalResource hal = render(resourceImpl);

    assertThat(hal.adaptTo(TestState.class)).isEqualToComparingFieldByField(state);
  }


  public interface ResourceWithoutAnnotation {

    @ResourceState
    Maybe<TestState> getState();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void should_throw_exception_if_no_HalApiInterface_annotation_can_be_found() {

    ResourceWithoutAnnotation resourceImpl = new ResourceWithoutAnnotation() {

      @Override
      public Maybe<TestState> getState() {
        return Maybe.empty();
      }
    };

    render(resourceImpl);
  }

  @HalApiInterface
  interface ResourceWithNonPublicInterface {

    @ResourceState
    Maybe<TestState> getState();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void should_throw_exception_if_HalApiInterface_is_not_public() {

    ResourceWithNonPublicInterface resourceImpl = new ResourceWithNonPublicInterface() {

      @Override
      public Maybe<TestState> getState() {
        return Maybe.empty();
      }
    };

    render(resourceImpl);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void should_throw_exception_if_ResourceState_method_returns_null() {

    TestResourceWithMaybeState resourceImpl = new TestResourceWithMaybeState() {

      @Override
      public Maybe<TestState> getState() {
        return null;
      }

    };

    render(resourceImpl);
  }

  @Test(expected = RuntimeException.class)
  public void should_throw_runtime_exception_if_ResourceState_method_throws_exception() {

    TestResourceWithMaybeState resourceImpl = new TestResourceWithMaybeState() {

      @Override
      public Maybe<TestState> getState() {
        throw new NotImplementedException("not implemented");
      }

    };

    render(resourceImpl);
  }

}

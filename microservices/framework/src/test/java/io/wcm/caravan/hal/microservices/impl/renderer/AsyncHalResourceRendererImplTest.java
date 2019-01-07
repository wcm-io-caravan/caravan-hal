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
package io.wcm.caravan.hal.microservices.impl.renderer;

import static io.wcm.caravan.hal.api.relations.StandardRelations.ITEM;
import static io.wcm.caravan.hal.api.server.testing.TestRelations.LINKED;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.api.annotations.RelatedResource;
import io.wcm.caravan.hal.api.annotations.ResourceState;
import io.wcm.caravan.hal.api.annotations.TemplateVariable;
import io.wcm.caravan.hal.api.server.testing.LinkableTestResource;
import io.wcm.caravan.hal.api.server.testing.TestResource;
import io.wcm.caravan.hal.api.server.testing.TestState;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.api.server.EmbeddableResource;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.microservices.impl.metadata.ResponseMetadataGenerator;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;


public class AsyncHalResourceRendererImplTest {

  static HalResource render(Object resourceImplInstance) {

    RequestMetricsCollector metrics = new ResponseMetadataGenerator();
    AsyncHalResourceRendererImpl renderer = new AsyncHalResourceRendererImpl(metrics);

    Single<HalResource> rxResource;
    if (resourceImplInstance instanceof LinkableResource) {
      rxResource = renderer.renderResource((LinkableResource)resourceImplInstance);
    }
    else {
      rxResource = renderer.renderLinkedOrEmbeddedResource(resourceImplInstance);
    }

    return rxResource.toObservable().blockingFirst();
  }

  static TestState createTestState() {
    return new TestState("Das ist doch nur ein Test");
  }

  @Test
  public void self_link_should_be_rendered() {

    Link link = new Link("/foo/bar").setTitle("Title of the self link");

    LinkableTestResource resourceImpl = new LinkableTestResource() {

      @Override
      public Link createLink() {
        return link;
      }
    };

    HalResource hal = render(resourceImpl);

    assertThat(hal.getLink().getModel()).isEqualTo(link.getModel());
  }

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

  @HalApiInterface
  public interface TestResourceWithSingleLink {

    @RelatedResource(relation = LINKED)
    Single<TestResource> getLinked();
  }

  @Test
  public void single_link_should_be_rendered() {

    Link testLink = new Link("/test/url").setTitle("Test title").setName("Test name");

    TestResourceWithSingleLink resourceImpl = new TestResourceWithSingleLink() {

      @Override
      public Single<TestResource> getLinked() {
        return Single.just(new LinkableTestResource() {

          @Override
          public Link createLink() {
            return testLink;
          }

        });
      }

    };

    HalResource hal = render(resourceImpl);

    assertThat(hal.getEmbedded(LINKED)).isEmpty();
    assertThat(hal.getLinks(LINKED)).containsExactly(testLink);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void returning_null_in_createLink_should_throw_exception() {

    TestResourceWithSingleLink resourceImpl = new TestResourceWithSingleLink() {

      @Override
      public Single<TestResource> getLinked() {
        return Single.just(new LinkableTestResource() {

          @Override
          public Link createLink() {
            return null;
          }

        });
      }

    };

    render(resourceImpl);
  }

  @HalApiInterface
  public interface TestResourceWithMaybeLink {

    @RelatedResource(relation = LINKED)
    Maybe<TestResource> getLinked();
  }

  @Test
  public void maybe_link_should_be_rendered() {

    Link testLink = new Link("/test/url").setTitle("Test title").setName("Test name");

    TestResourceWithMaybeLink resourceImpl = new TestResourceWithMaybeLink() {

      @Override
      public Maybe<TestResource> getLinked() {
        return Maybe.just(new LinkableTestResource() {

          @Override
          public Link createLink() {
            return testLink;
          }

        });
      }

    };

    HalResource hal = render(resourceImpl);

    assertThat(hal.getEmbedded(LINKED)).isEmpty();
    assertThat(hal.getLinks(LINKED)).containsExactly(testLink);
  }

  @Test
  public void maybe_link_should_be_ignored_if_absent() {

    TestResourceWithMaybeLink resourceImpl = new TestResourceWithMaybeLink() {

      @Override
      public Maybe<TestResource> getLinked() {
        return Maybe.empty();
      }

    };

    HalResource hal = render(resourceImpl);
    assertThat(hal.getEmbedded(LINKED)).isEmpty();
    assertThat(hal.getLinks(LINKED)).isEmpty();
  }

  @HalApiInterface
  public interface TestResourceWithObservableLinks {

    @RelatedResource(relation = LINKED)
    Observable<TestResource> getLinked();
  }


  @Test
  public void multiple_links_should_be_rendered_in_original_order() {

    List<Link> links = Observable.range(0, 10)
        .map(i -> new Link("/test/" + i).setName(Integer.toString(i)))
        .toList().blockingGet();

    TestResourceWithObservableLinks resourceImpl = new TestResourceWithObservableLinks() {

      @Override
      public Observable<TestResource> getLinked() {
        return Observable.fromIterable(links)
            .map(link -> new LinkableTestResource() {

              @Override
              public Link createLink() {
                return link;
              }

            });
      }

    };

    HalResource hal = render(resourceImpl);
    assertThat(hal.getEmbedded(LINKED)).isEmpty();

    List<Link> actualLinks = hal.getLinks(LINKED);
    assertThat(actualLinks).hasSameSizeAs(links);
    for (int i = 0; i < actualLinks.size(); i++) {
      assertThat(actualLinks.get(i)).isEqualTo(links.get(i));
    }
  }

  @HalApiInterface
  public interface TestResourceWithSingleLinkTemplate {

    @RelatedResource(relation = LINKED)
    Single<TestResource> getLinkedWithNumber(@TemplateVariable("number") Integer number);
  }

  @Test
  public void single_link_template_should_be_rendered() {

    Link testLink = new Link("/test/{number}");

    TestResourceWithSingleLinkTemplate resourceImpl = new TestResourceWithSingleLinkTemplate() {

      @Override
      public Single<TestResource> getLinkedWithNumber(Integer number) {
        return Single.just(new LinkableTestResource() {

          @Override
          public Link createLink() {
            return testLink;
          }

        });
      }

    };

    HalResource hal = render(resourceImpl);

    assertThat(hal.getEmbedded(LINKED)).isEmpty();
    assertThat(hal.getLinks(LINKED)).containsExactly(testLink);
  }

  @HalApiInterface
  public interface TestResourceWithSingleExternalLink {

    @RelatedResource(relation = LINKED)
    Single<LinkableResource> getExternal();
  }

  @Test
  public void single_external_link_should_be_rendered() {

    Link externalLink = new Link("http://external.url");

    TestResourceWithSingleExternalLink resourceImpl = new TestResourceWithSingleExternalLink() {

      @Override
      public Single<LinkableResource> getExternal() {
        return Single.just(new LinkableResource() {

          @Override
          public Link createLink() {
            return externalLink;
          }
        });
      }
    };

    HalResource hal = render(resourceImpl);

    assertThat(hal.getLinks(LINKED)).containsExactly(externalLink);
  }


  @HalApiInterface
  public interface TestResourceWithInvalidEmissionType {

    @RelatedResource(relation = LINKED)
    Maybe<TestState> getLinked();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void single_link_with_invalid_emission_type_should_throw_error() {

    Link externalLink = new Link("http://external.url");

    TestResourceWithInvalidEmissionType resourceImpl = new TestResourceWithInvalidEmissionType() {

      @Override
      public Maybe<TestState> getLinked() {
        return Maybe.empty();
      }
    };

    HalResource hal = render(resourceImpl);

    assertThat(hal.getLinks(LINKED)).containsExactly(externalLink);
  }

  @HalApiInterface
  public interface TestResourceWithObservableEmbedded {

    @RelatedResource(relation = ITEM)
    Observable<TestResource> getItems();
  }

  static class EmbeddedTestResource implements TestResource, EmbeddableResource {

    protected final TestState state;

    EmbeddedTestResource(TestState state) {
      this.state = state;
    }

    @Override
    public Maybe<TestState> getState() {
      return Maybe.just(state);
    }

    @Override
    public boolean isEmbedded() {
      return true;
    }
  }

  @Test
  public void multiple_embedded_resources_should_be_rendered_in_original_order() {

    List<TestState> states = Observable.range(0, 10)
        .map(i -> new TestState(i))
        .toList().blockingGet();

    TestResourceWithObservableEmbedded resourceImpl = new TestResourceWithObservableEmbedded() {

      @Override
      public Observable<TestResource> getItems() {

        return Observable.fromIterable(states)
            .map(EmbeddedTestResource::new);
      }
    };

    HalResource hal = render(resourceImpl);

    List<HalResource> actualEmbedded = hal.getEmbedded(ITEM);
    assertThat(actualEmbedded).hasSameSizeAs(states);
    for (int i = 0; i < actualEmbedded.size(); i++) {
      assertThat(actualEmbedded.get(i).adaptTo(TestState.class)).isEqualToComparingFieldByField(states.get(i));
    }

    assertThat(hal.getLinks(ITEM)).isEmpty();

  }

  static class LinkedEmbeddableTestResource extends EmbeddedTestResource implements LinkableResource {

    LinkedEmbeddableTestResource(TestState state) {
      super(state);
    }

    @Override
    public boolean isEmbedded() {
      return false;
    }

    @Override
    public Link createLink() {
      return new Link("/" + state.number);
    }

  }

  @Test
  public void embeddable_resources_should_only_be_linked_if_isEmbedded_returns_false() {

    List<TestState> states = Observable.range(0, 10)
        .map(i -> new TestState(i))
        .toList().blockingGet();

    TestResourceWithObservableEmbedded resourceImpl = new TestResourceWithObservableEmbedded() {

      @Override
      public Observable<TestResource> getItems() {

        return Observable.fromIterable(states)
            .map(LinkedEmbeddableTestResource::new);
      }
    };

    HalResource hal = render(resourceImpl);
    assertThat(hal.getEmbedded(ITEM)).isEmpty();
    assertThat(hal.getLinks(ITEM)).hasSameSizeAs(states);
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
  public void should_throw_exception_if_no_HalApiInterface_is_not_public() {

    ResourceWithNonPublicInterface resourceImpl = new ResourceWithNonPublicInterface() {

      @Override
      public Maybe<TestState> getState() {
        return Maybe.empty();
      }
    };

    render(resourceImpl);
  }

  @HalApiInterface
  public interface ResourceWithNonReactiveReturnType {

    @ResourceState
    TestState getState();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void should_throw_exception_if_ResourceState_method_does_not_have_reactive_return_type() {

    ResourceWithNonReactiveReturnType resourceImpl = new ResourceWithNonReactiveReturnType() {

      @Override
      public TestState getState() {
        return createTestState();
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

  @HalApiInterface
  public interface ResourceWithInvalidRelatedMethod {

    @RelatedResource(relation = ITEM)
    Observable<TestState> getRelated();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void should_throw_exception_if_RelatedResource_return_type_does_not_emit_interface() {

    ResourceWithInvalidRelatedMethod resourceImpl = new ResourceWithInvalidRelatedMethod() {

      @Override
      public Observable<TestState> getRelated() {
        return Observable.empty();
      }
    };

    render(resourceImpl);
  }
}

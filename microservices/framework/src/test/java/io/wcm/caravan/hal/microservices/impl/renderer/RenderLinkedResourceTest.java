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

import static io.wcm.caravan.hal.microservices.impl.renderer.AsyncHalResourceRendererTestUtil.createSingleExternalLinkedResource;
import static io.wcm.caravan.hal.microservices.impl.renderer.AsyncHalResourceRendererTestUtil.render;
import static io.wcm.caravan.hal.microservices.testing.TestRelations.LINKED;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.api.annotations.RelatedResource;
import io.wcm.caravan.hal.api.annotations.TemplateVariable;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.microservices.testing.LinkableTestResource;
import io.wcm.caravan.hal.microservices.testing.TestResource;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

public class RenderLinkedResourceTest {


  @HalApiInterface
  public interface TestResourceWithSingleLink {

    @RelatedResource(relation = LINKED)
    Single<TestResource> getLinked();
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
  public interface TestResourceWithPublisherLinks {

    @RelatedResource(relation = LINKED)
    Publisher<TestResource> getLinked();
  }

  @Test
  public void publisher_link_should_be_rendered() {

    Link testLink = new Link("/test/url").setTitle("Test title").setName("Test name");

    TestResourceWithPublisherLinks resourceImpl = new TestResourceWithPublisherLinks() {

      @Override
      public Publisher<TestResource> getLinked() {
        return Flowable.just(new LinkableTestResource() {

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
        return createSingleExternalLinkedResource(externalLink);
      }
    };

    HalResource hal = render(resourceImpl);

    assertThat(hal.getLinks(LINKED)).containsExactly(externalLink);
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

}
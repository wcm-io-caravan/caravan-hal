/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2015 wcm.io
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
package io.wcm.caravan.hal.docs.impl.augmenter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import io.wcm.caravan.commons.stream.Streams;
import io.wcm.caravan.hal.docs.HalDocsAugmenter;
import io.wcm.caravan.hal.docs.impl.model.LinkRelation;
import io.wcm.caravan.hal.docs.impl.model.Service;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.HalResourceFactory;
import io.wcm.caravan.hal.resource.Link;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;


public class HalDocsAugmenterImplTest {

  private static final String DOCS_PATH = "/docs";

  private HalDocsAugmenter underTest;
  private HalResource hal;

  @Before
  public void setUp() {
    Service service = new Service();
    LinkRelation rel1 = new LinkRelation();
    rel1.setRel("ex:rel1");
    rel1.setDescriptionMarkup("rel1-title");
    service.addLinkRelation(rel1);

    LinkRelation rel2 = new LinkRelation();
    rel2.setRel("in:rel2");
    rel2.setDescriptionMarkup("rel2-title");
    service.addLinkRelation(rel2);

    LinkRelation rel3 = new LinkRelation();
    rel3.setRel("cust:rel3");
    rel3.setDescriptionMarkup("rel3-title");
    service.addLinkRelation(rel3);

    underTest = new HalDocsAugmenterImpl(service, DOCS_PATH);

    hal = HalResourceFactory.createResource("/resource")
        .setLink("ex:external-link", HalResourceFactory.createLink("/external-link"))
        .addLinks("in:children", HalResourceFactory.createLink("/child-1"), HalResourceFactory.createLink("/child-2"))
        .addLinks("no-curie", HalResourceFactory.createLink("/no-curi-1"));
  }

  @Test
  public void shouldAddAllCuriesForCurieNames() {
    underTest.augment(hal);
    List<Link> curies = hal.getLinks("curies");
    assertEquals(2, curies.size());
    assertEquals("ex", curies.get(0).getName());
    assertEquals("/docs/ex:{rel}", curies.get(0).getHref());
  }

  @Test
  public void shouldNotAddCuriForMissingCurieName() {
    underTest.augment(hal);
    Streams.of(hal.getLinks("curies"))
    .map(link -> link.getName())
    .filter(name -> StringUtils.equals(name, "cust"))
    .forEach(name -> fail("cust is no CURI for this HAL"));
  }

  @Test
  public void shouldNotOverrideExistingCuri() {
    hal.addLinks("curies", HalResourceFactory.createLink("https://example.com/doc/other/{rel}").setName("ex"));
    underTest.augment(hal);
    List<Link> curies = hal.getLinks("curies");
    assertEquals("https://example.com/doc/other/{rel}", curies.get(0).getHref());
  }

  @Test
  public void shouldOnlyAddCuriLinkOnce() {
    hal.addLinks("ex:external-link2", HalResourceFactory.createLink("/external-link2"));
    underTest.augment(hal);
    List<Link> curies = hal.getLinks("curies");
    assertEquals(2, curies.size());
  }

  @Test
  public void shouldAddCuriForLinksInEmbeddedResource() {
    HalResource item = HalResourceFactory.createResource("/item")
        .addLinks("cust:item", HalResourceFactory.createLink("/item-link"));
    hal.addEmbedded("item", item);

    underTest.augment(hal);

    List<Link> curies = hal.getLinks("curies");
    assertEquals(3, curies.size());
    assertEquals("cust", curies.get(2).getName());
  }

}

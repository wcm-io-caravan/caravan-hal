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
import static org.junit.Assert.assertNull;
import io.wcm.caravan.hal.docs.impl.model.LinkRelation;
import io.wcm.caravan.hal.docs.impl.model.Service;

import org.junit.Before;
import org.junit.Test;

public class DocsMetadataTest {

  private static final String DOCS_PATH = "/docs";

  private DocsMetadata underTest;

  @Before
  public void setUp() {
    Service service = new Service();

    LinkRelation rel1 = new LinkRelation();
    rel1.setRel("simple");
    rel1.setDescriptionMarkup("Simple description");
    service.addLinkRelation(rel1);

    LinkRelation rel2 = new LinkRelation();
    rel2.setRel("ns1:rel2");
    rel2.setDescriptionMarkup("<p>Description for rel 2.</p>");
    service.addLinkRelation(rel2);

    LinkRelation rel3 = new LinkRelation();
    rel3.setRel("ns1:rel3");
    rel3.setDescriptionMarkup("<p>Description for rel 3. There is even more text here.</p>");
    service.addLinkRelation(rel3);

    LinkRelation rel4 = new LinkRelation();
    rel4.setRel("ns2:rel4");
    rel4.setDescriptionMarkup("<p>Description contains < invalid markup &. Will it still work?");
    service.addLinkRelation(rel4);

    LinkRelation rel5 = new LinkRelation();
    rel5.setRel("ns2:rel5");
    service.addLinkRelation(rel5);

    underTest = new DocsMetadata(service, DOCS_PATH);
  }

  @Test
  public void testGetCurieLinks() {
    assertEquals(DOCS_PATH + "/ns1:{rel}", underTest.getCurieLink("ns1"));
    assertEquals(DOCS_PATH + "/ns2:{rel}", underTest.getCurieLink("ns2"));
  }

  @Test
  public void testGetLinkRelationTitle() {
    assertEquals("Simple description", underTest.getLinkRelationTitle("simple"));
    assertEquals("Description for rel 2.", underTest.getLinkRelationTitle("ns1:rel2"));
    assertEquals("Description for rel 3.", underTest.getLinkRelationTitle("ns1:rel3"));
    assertEquals("Description contains < invalid markup &.", underTest.getLinkRelationTitle("ns2:rel4"));
    assertNull(underTest.getLinkRelationTitle("ns2:rel5"));
  }

}

/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2016 wcm.io
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
package io.wcm.caravan.hal.resource;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class LinkTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private ObjectNode model;
  private HalResource hal;

  @Before
  public void setUp() throws JsonParseException, JsonMappingException, IOException {
    model = OBJECT_MAPPER.readValue(getClass().getResourceAsStream("/jackson_hal_resource_model.json"), ObjectNode.class);
    hal = new HalResource(model);
  }

  @Test
  public void remove_succeedsForSelfLink() {
    Link linkToRemove = hal.getLink();
    linkToRemove.remove();

    Assert.assertNull("self links is null?", hal.getLink());
  }

  @Test
  public void remove_succeedsForSingleLink() {
    Link linkToRemove = hal.getLink("parent");
    linkToRemove.remove();

    Assert.assertFalse("parent link is removed?", hal.hasLink("parent"));
  }

  @Test
  public void remove_succeedsForFirstLinkInAnArray() {

    Link linkToRemove = hal.getLinks("children").get(0);
    linkToRemove.remove();

    List<Link> remainingLinks = hal.getLinks("children");
    Assert.assertEquals("one children link is left?", 1, remainingLinks.size());
    Assert.assertNotEquals("children link that's left is not the one that was just removed?", linkToRemove.getHref(), remainingLinks.get(0).getHref());
  }

  @Test
  public void remove_succeedsForSecondLinkInAnArray() {
    Link linkToRemove = hal.getLinks("children").get(1);
    linkToRemove.remove();

    List<Link> remainingLinks = hal.getLinks("children");
    Assert.assertEquals("one children link is left?", 1, remainingLinks.size());
    Assert.assertNotEquals("children link that's left is not the one that was just removed?", linkToRemove.getHref(), remainingLinks.get(0).getHref());

  }

  @Test(expected = IllegalStateException.class)
  public void remove_failsForDetachedLinks() {
    Link linkToRemove = HalResourceFactory.createLink("/some/uri");
    linkToRemove.remove();
  }

  @Test(expected = IllegalStateException.class)
  public void remove_failsWhenRemovedTwice() {
    Link linkToRemove = hal.getLink("parent");
    linkToRemove.remove();
    linkToRemove.remove();
  }

  @Test(expected = IllegalStateException.class)
  public void remove_failsIfLinkHasBeenRemovedByOtherMeans() {
    Link linkToRemove = hal.getLink("parent");
    hal.removeLinks();
    linkToRemove.remove();
  }
}

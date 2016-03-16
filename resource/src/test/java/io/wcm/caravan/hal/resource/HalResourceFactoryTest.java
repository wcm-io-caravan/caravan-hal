/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2014 - 2015 wcm.io
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SuppressWarnings("deprecation")
public class HalResourceFactoryTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  public void getStateAsObject_shouldConvertJsonToAnyObject() throws Exception {
    ObjectNode model = OBJECT_MAPPER.readValue(getClass().getResourceAsStream("/jackson_hal_resource_model.json"), ObjectNode.class);
    HalResource hal = new HalResource(model);
    TestObject state = hal.adaptTo(TestObject.class);
    assertEquals("value1", state.property1);
    assertEquals("value2", state.property2);
  }

  @Test
  public void convert_shouldConvertObjectToJson() throws Exception {
    TestObject state = new TestObject();
    state.property1 = "value1";
    state.property2 = "value2";
    ObjectNode json = new HalResource(state).getModel();
    assertEquals("value1", json.get("property1").asText(null));
  }

  @Test
  public void createResourceString_shouldSetHrefForSelfLink() {
    HalResource hal = HalResourceFactory.createResource("/");
    assertEquals("/", hal.getLink().getHref());
  }

  @Test
  public void createResourceString_shouldSetNoSelfLinkIfHrefIsNull() {
    HalResource hal = HalResourceFactory.createResource(null);
    assertFalse(hal.getModel().has("_link"));
  }

  @Test
  public void createResourceObjectString_shouldSetStateAndHrefForSelfLink() {
    TestObject model = new TestObject();
    model.property1 = "value1";
    HalResource hal = HalResourceFactory.createResource(model, "/");
    assertEquals("value1", hal.getModel().get("property1").asText(null));
    assertEquals("/", hal.getLink().getHref());
  }

  @Test
  public void createResourceObjectNodeString_shouldSetStateAndHrefForSelfLink() {
    ObjectNode model = OBJECT_MAPPER.createObjectNode().put("att", "value");
    HalResource hal = new HalResource(model, "/");
    assertEquals("value", hal.getModel().get("att").asText(null));
    assertEquals("/", hal.getLink().getHref());
  }

  @Test
  public void createResourcejsonNodeString_shouldSetStateAndHrefForSelfLink() {
    JsonNode model = OBJECT_MAPPER.createObjectNode().put("att", "value");
    HalResource hal = HalResourceFactory.createResource(model, "/");
    assertEquals("value", hal.getModel().get("att").asText(null));
    assertEquals("/", hal.getLink().getHref());
  }

  private static class TestObject {

    public String property1;
    public String property2;

  }

}

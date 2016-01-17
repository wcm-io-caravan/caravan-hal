/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2014 wcm.io
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.osgi.annotation.versioning.ProviderType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableListMultimap.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;

/**
 * Bean representation of a HAL resource.
 */
@ProviderType
public final class HalResource implements HalObject {

  /**
   * The mime content type
   */
  public static final String CONTENT_TYPE = "application/hal+json";

  private final ObjectNode model;

  /**
   * @param model JSON model
   * @deprecated Use {@link HalResource#HalResource(JsonNode)} instead
   */
  @Deprecated
  public HalResource(ObjectNode model) {
    this.model = model;
  }

  /**
   * Create an empty HAL resource, with no object state or links
   */
  public HalResource() {
    this(JsonNodeFactory.instance.objectNode());
  }

  /**
   * Create a HAL resource with empty state that only contains a self link with the given URI
   * @param uri the URI under which this resource can be retrieved
   */
  public HalResource(String uri) {
    this();
    setLink(HalResourceFactory.createLink(uri));
  }

  /**
   * Helping constructor checking if the {@code model} is an {@link ObjectNode}. Throws an
   * {@link IllegalArgumentException} if not.
   * @param model JSON model
   */
  public HalResource(JsonNode model) {
    Preconditions.checkArgument(model instanceof ObjectNode, "Model is not an ObjectNode");
    this.model = (ObjectNode)model;
  }

  @Override
  public ObjectNode getModel() {
    return model;
  }

  /**
   * @param <T> return type
   * @param type a class that matches the structure of this resource's model
   * @return a new instance of the given class, populated with the properties of this resource's model
   */
  public <T> T adaptTo(Class<T> type) {
    return HalResourceFactory.getStateAsObject(this, type);
  }

  /**
   * @param relation Link relation
   * @return True if has link for the given relation
   */
  public boolean hasLink(String relation) {
    return hasResource(HalResourceType.LINKS, relation);
  }

  /**
   * @param relation Embedded resource relation
   * @return True if has embedded resource for the given relation
   */
  public boolean hasEmbedded(String relation) {
    return hasResource(HalResourceType.EMBEDDED, relation);
  }

  private boolean hasResource(HalResourceType type, String relation) {
    return !model.at("/" + type + "/" + relation).isMissingNode();
  }

  /**
   * @return Self link for the resource. Can be null
   */
  public Link getLink() {
    return getLink("self");
  }

  /**
   * @return All links
   */
  public ListMultimap<String, Link> getLinks() {
    return getResources(Link.class, HalResourceType.LINKS);
  }

  /**
   * @return All embedded resources
   */
  public ListMultimap<String, HalResource> getEmbedded() {
    return getResources(HalResource.class, HalResourceType.EMBEDDED);
  }

  private <X extends HalObject> ListMultimap<String, X> getResources(Class<X> clazz, HalResourceType type) {
    if (!model.has(type.toString())) {
      return ImmutableListMultimap.of();
    }
    Builder<String, X> resources = ImmutableListMultimap.builder();
    model.get(type.toString())
    .fieldNames()
    .forEachRemaining(field -> resources.putAll(field, getResources(clazz, type, field)));
    return resources.build();
  }

  /**
   * @param relation Link relation
   * @return Link for the given relation
   */
  public Link getLink(String relation) {
    return hasLink(relation) ? getLinks(relation).get(0) : null;
  }

  /**
   * @param relation Link relation
   * @return All links for the given relation
   */
  public List<Link> getLinks(String relation) {
    return getResources(Link.class, HalResourceType.LINKS, relation);
  }

  /**
   * recursively collects links within this resource and all embedded resources
   * @param rel the relation your interested in
   * @return a list of all links
   */
  public List<Link> collectLinks(String rel) {
    return collectResources(Link.class, HalResourceType.LINKS, rel);
  }

  private <X extends HalObject> List<X> collectResources(Class<X> clazz, HalResourceType type, String relation) {
    ImmutableList.Builder<X> builder = ImmutableList.<X>builder().addAll(getResources(clazz, type, relation));
    getEmbedded().values().stream()
    .map(embedded -> embedded.collectResources(clazz, type, relation))
    .forEach(embeddedResources -> builder.addAll(embeddedResources));
    return builder.build();
  }

  /**
   * @param relation Embedded resource relation
   * @return Embedded resources for the given relation
   */
  public HalResource getEmbeddedResource(String relation) {
    return hasEmbedded(relation) ? getEmbedded(relation).get(0) : null;
  }

  /**
   * @param relation Embedded resource relation
   * @return All embedded resources for the given relation
   */
  public List<HalResource> getEmbedded(String relation) {
    return getResources(HalResource.class, HalResourceType.EMBEDDED, relation);
  }

  /**
   * recursively collects embedded resources of a specific rel
   * @param rel the relation your interested in
   * @return a list of all embedded resources
   */
  public List<HalResource> collectEmbedded(String rel) {
    return collectResources(HalResource.class, HalResourceType.EMBEDDED, rel);
  }

  private <X extends HalObject> List<X> getResources(Class<X> clazz, HalResourceType type, String relation) {
    if (!hasResource(type, relation)) {
      return ImmutableList.of();
    }
    JsonNode resources = model.at("/" + type + "/" + relation);
    try {
      Constructor<X> constructor = clazz.getConstructor(ObjectNode.class);
      if (resources instanceof ObjectNode) {
        return ImmutableList.of(constructor.newInstance(resources));
      }
      else {
        ImmutableList.Builder<X> result = ImmutableList.builder();
        for (JsonNode resource : resources) {
          if (resource instanceof ObjectNode) {
            result.add(constructor.newInstance(resource));
          }
        }
        return result.build();
      }
    }
    catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Sets link for the {@code self} relation. Overwrites existing one.
   * @param link Link to set
   * @return HAL resource
   */
  public HalResource setLink(Link link) {
    return setLink("self", link);
  }

  /**
   * Sets link for the given relation. Overwrites existing one. If {@code link} is {@code null} it gets ignored.
   * @param relation Link relation
   * @param link Link to add
   * @return HAL resource
   */
  public HalResource setLink(String relation, Link link) {
    if (link == null) {
      return this;
    }
    return addResources(HalResourceType.LINKS, relation, false, new Link[] {
        link
    });
  }

  /**
   * Adds links for the given relation
   * @param relation Link relation
   * @param links Links to add
   * @return HAL resource
   */
  public HalResource addLinks(String relation, Link... links) {
    return addResources(HalResourceType.LINKS, relation, true, links);
  }

  /**
   * Adds links for the given relation
   * @param relation Link relation
   * @param links Links to add
   * @return HAL resource
   */
  public HalResource addLinks(String relation, Iterable<Link> links) {
    return addLinks(relation, Iterables.toArray(links, Link.class));
  }

  /**
   * Embed resource for the given relation. Overwrites existing one.
   * @param relation Embedded resource relation
   * @param resource Resource to embed
   * @return HAL resource
   */
  public HalResource setEmbedded(String relation, HalResource resource) {
    if (resource == null) {
      return this;
    }
    return addResources(HalResourceType.EMBEDDED, relation, false, new HalResource[] {
        resource
    });
  }

  /**
   * Embed resources for the given relation
   * @param relation Embedded resource relation
   * @param resources Resources to embed
   * @return HAL resource
   */
  public HalResource addEmbedded(String relation, HalResource... resources) {
    return addResources(HalResourceType.EMBEDDED, relation, true, resources);
  }

  /**
   * Embed resources for the given relation
   * @param relation Embedded resource relation
   * @param resources Resources to embed
   * @return HAL resource
   */
  public HalResource addEmbedded(String relation, Iterable<HalResource> resources) {
    return addEmbedded(relation, Iterables.toArray(resources, HalResource.class));
  }

  private <X extends HalObject> HalResource addResources(HalResourceType type, String relation, boolean asArray, X[] newResources) {
    if (newResources.length == 0) {
      return this;
    }
    ObjectNode resources = model.has(type.toString()) ? (ObjectNode)model.get(type.toString()) : model.putObject(type.toString());

    if (asArray) {
      ArrayNode container = getArrayNodeContainer(type, relation, resources);
      Arrays.stream(newResources).forEach(link -> container.add(link.getModel()));
    }
    else {
      resources.set(relation, newResources[0].getModel());
    }
    return this;
  }

  private ArrayNode getArrayNodeContainer(HalResourceType type, String relation, ObjectNode resources) {
    if (hasResource(type, relation)) {
      if (resources.get(relation).isArray()) {
        return (ArrayNode)resources.get(relation);
      }
      else {
        JsonNode temp = resources.get(relation);
        return resources.putArray(relation).add(temp);
      }
    }
    else {
      return resources.putArray(relation);
    }
  }

  /**
   * Removes all links for the given relation.
   * @param relation Link relation
   * @return HAL resource
   */
  public HalResource removeLinks(String relation) {
    return removeResource(HalResourceType.LINKS, relation);
  }

  /**
   * Removes all embedded resources for the given relation.
   * @param relation Embedded resource relation
   * @return HAL resource
   */
  public HalResource removeEmbedded(String relation) {
    return removeResource(HalResourceType.EMBEDDED, relation);
  }

  private HalResource removeResource(HalResourceType type, String relation) {
    if (hasResource(type, relation)) {
      ((ObjectNode)model.get(type.toString())).remove(relation);
    }
    return this;
  }

  /**
   * Removes one link for the given relation and index.
   * @param relation Link relation
   * @param index Array index
   * @return HAL resource
   */
  public HalResource removeLink(String relation, int index) {
    return removeResource(HalResourceType.LINKS, relation, index);
  }


  /**
   * Remove the link with the given relation and href
   * @param relation Link relation
   * @param href to identify the link to remove
   * @return this HAL resource
   */
  public HalResource removeLinkWithHref(String relation, String href) {

    List<Link> links = getLinks(relation);
    for (int i = 0; i < links.size(); i++) {
      if (href.equals(links.get(i).getHref())) {
        return removeLink(relation, i);
      }
    }

    return this;
  }

  /**
   * Removes one embedded resource for the given relation and index.
   * @param relation Embedded resource relation
   * @param index Array index
   * @return HAL resource
   */
  public HalResource removeEmbedded(String relation, int index) {
    return removeResource(HalResourceType.EMBEDDED, relation, index);
  }

  private HalResource removeResource(HalResourceType type, String relation, int index) {
    if (hasResource(type, relation)) {
      JsonNode resources = model.at("/" + type + "/" + relation);
      if (resources instanceof ObjectNode || resources.size() <= 1) {
        ((ObjectNode)model.get(type.toString())).remove(relation);
      }
      else {
        ((ArrayNode)resources).remove(index);
      }
    }
    return this;
  }

  /**
   * Removes all links.
   * @return HAL resource
   */
  public HalResource removeLinks() {
    return removeResources(HalResourceType.LINKS);
  }

  /**
   * Removes all embedded resources.
   * @return HAL resource
   */
  public HalResource removeEmbedded() {
    return removeResources(HalResourceType.EMBEDDED);
  }

  /**
   * Changes the rel of embedded resources
   * @param relToRename the rel that you want to change
   * @param newRel the new rel for all embedded items
   * @return HAL resource
   */
  public HalResource renameEmbedded(String relToRename, String newRel) {
    List<HalResource> resources = getEmbedded(relToRename);
    return removeEmbedded(relToRename).addEmbedded(newRel, resources);
  }

  private HalResource removeResources(HalResourceType type) {
    model.remove(type.toString());
    return this;
  }

  /**
   * Adds state to the resource.
   * @param state Resource state
   * @return HAL resource
   */
  public HalResource addState(ObjectNode state) {
    state.fields().forEachRemaining(entry -> model.set(entry.getKey(), entry.getValue()));
    return this;
  }

  /**
   * @return JSON field names for the state object
   */
  public List<String> getStateFieldNames() {
    Iterable<String> iterable = () -> model.fieldNames();
    return StreamSupport.stream(iterable.spliterator(), false)
        .filter(field -> !"_links".equals(field) && !"_embedded".equals(field))
        .collect(Collectors.toList());
  }

  /**
   * Removes all state attributes
   * @return HAL resource
   */
  public HalResource removeState() {
    getStateFieldNames().forEach(field -> model.remove(field));
    return this;
  }

}

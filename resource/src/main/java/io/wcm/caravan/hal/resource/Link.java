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

import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.osgi.annotation.versioning.ProviderType;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ListMultimap;

/**
 * Bean representation of a HAL link.
 */
@ProviderType
public final class Link implements HalObject {

  private final ObjectNode model;

  private HalResource context;

  /**
   * @param model JSON model
   */
  public Link(ObjectNode model) {
    this.model = model;
  }

  @Override
  public ObjectNode getModel() {
    return model;
  }

  /**
   * @return the type
   */
  public String getType() {
    return model.path("type").asText(null);
  }

  /**
   * @param type the type to set
   * @return Link
   */
  public Link setType(String type) {
    model.put("type", type);
    return this;
  }

  /**
   * @return the deprecation
   */
  public String getDeprecation() {
    return model.path("deprecation").asText(null);
  }

  /**
   * @param deprecation the deprecation to set
   * @return Link
   */
  public Link setDeprecation(String deprecation) {
    model.put("deprecation", deprecation);
    return this;
  }

  /**
   * @return the name
   */
  public String getName() {
    return model.path("name").asText(null);
  }

  /**
   * @param name the name to set
   * @return Link
   */
  public Link setName(String name) {
    model.put("name", name);
    return this;
  }

  /**
   * @return the profile
   */
  public String getProfile() {
    return model.path("profile").asText(null);
  }

  /**
   * @param profile the profile to set
   * @return Link
   */
  public Link setProfile(String profile) {
    model.put("profile", profile);
    return this;
  }

  /**
   * @return the title
   */
  public String getTitle() {
    return model.path("title").asText(null);
  }

  /**
   * @param title the title to set
   * @return Link
   */
  public Link setTitle(String title) {
    model.put("title", title);
    return this;
  }

  /**
   * @return the hreflang
   */
  public String getHreflang() {
    return model.path("hreflang").asText(null);
  }

  /**
   * @param hreflang the hreflang to set
   * @return Link
   */
  public Link setHreflang(String hreflang) {
    model.put("hreflang", hreflang);
    return this;
  }

  /**
   * @return the href
   */
  public String getHref() {
    return model.path("href").asText(null);
  }

  /**
   * @param href the href to set
   * @return Link
   */
  public Link setHref(String href) {
    model.put("href", href);
    return this;
  }

  /**
   * @return is templated
   */
  public boolean isTemplated() {
    return model.path("templated").asBoolean();
  }

  /**
   * @param templated the templated to set
   * @return Link
   */
  public Link setTemplated(boolean templated) {
    model.put("templated", templated);
    return this;
  }

  /**
   * Removes this link from its context resource's JSON representation
   * @throws IllegalStateException if this link was never added to a resource, or has already been removed
   */
  public void remove() {

    if (context == null) {
      throw new IllegalStateException("link with href=" + getHref() + " can not be removed, because it's not part of a HAL resource tree");
    }

    // iterate over all links grouped by relation (because for removal we need to know the relation)
    ListMultimap<String, Link> allLinks = context.getLinks();
    for (String relation : allLinks.keySet()) {
      List<Link> links = allLinks.get(relation);

      // use an indexed for-loop, because we need to know the index to properly remove the link
      for (int i = 0; i < links.size(); i++) {
        if (links.get(i).getModel() == model) {
          context.removeLink(relation, i);
          context = null;
          return;
        }
      }
    }

    throw new IllegalStateException("the last known context resource of link with href=" + getHref() + " no longer contains this link");
  }

  /**
   * @param contextResource the HAL resource that contains this link
   */
  void setContext(HalResource contextResource) {
    context = contextResource;
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this, false);
  }

  @Override
  public boolean equals(Object obj) {
    return EqualsBuilder.reflectionEquals(this, obj);
  }


}

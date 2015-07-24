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
package io.wcm.caravan.hal.docs.impl.serviceinfo;

import io.wcm.caravan.commons.stream.Streams;
import io.wcm.caravan.hal.docs.HalServiceInfo;
import io.wcm.caravan.hal.docs.impl.model.LinkRelation;
import io.wcm.caravan.hal.docs.impl.model.Service;
import io.wcm.caravan.hal.resource.HalResourceFactory;
import io.wcm.caravan.hal.resource.util.HalCuriAugmenter;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;

import com.google.common.collect.ImmutableMap;

/**
 * Default implementation of {@link HalServiceInfo}.
 */
public class HalServiceInfoImpl implements HalServiceInfo {

  private final Map<String, String> curieLinks;
  private final Map<String, String> linkRelationTitles;
  private final HalCuriAugmenter curieAugmenter;

  /**
   * @param serviceModel Service model
   * @param docsPath Documenation URI base path
   */
  public HalServiceInfoImpl(Service serviceModel, String docsPath) {
    curieLinks = buildCurieLinkMap(serviceModel, docsPath);
    linkRelationTitles = buildLinkRelationTitlesMap(serviceModel);

    curieAugmenter = new HalCuriAugmenter();
    Streams.of(curieLinks.entrySet())
    .map(entry -> HalResourceFactory.createLink(entry.getValue()).setName(entry.getKey()).setTitle("Documentation link"))
    .forEach(curieAugmenter::register);
  }

  private static Map<String, String> buildCurieLinkMap(Service serviceModel, String docsPath) {
    Map<String, String> map = new HashMap<>();
    for (LinkRelation rel : serviceModel.getLinkRelations()) {
      if (StringUtils.contains(rel.getRel(), ":")) {
        String curie = StringUtils.substringBefore(rel.getRel(), ":");
        map.put(curie, docsPath + "/" + curie + ":{rel}");
      }
    }
    return ImmutableMap.copyOf(map);
  }

  private static Map<String, String> buildLinkRelationTitlesMap(Service serviceModel) {
    Map<String, String> map = new HashMap<>();
    for (LinkRelation rel : serviceModel.getLinkRelations()) {
      String title = buildLinkRelationTitle(rel.getDescriptionMarkup());
      if (title != null) {
        map.put(rel.getRel(), title);
      }
    }
    return ImmutableMap.copyOf(map);
  }

  /**
   * Get relation link title anlogous to javadoc method tile: Strip out all HTML tags and use only
   * the first sentence from the description.
   * @param descriptionMarkup Description markup.
   * @return Title or null if none defined
   */
  private static String buildLinkRelationTitle(String descriptionMarkup) {
    if (StringUtils.isBlank(descriptionMarkup)) {
      return null;
    }
    String text = Jsoup.parse(descriptionMarkup).text();
    if (StringUtils.isBlank(text)) {
      return null;
    }
    if (StringUtils.contains(text, ".")) {
      return StringUtils.substringBefore(text, ".") + ".";
    }
    else {
      return StringUtils.trim(text);
    }
  }

  @Override
  public Map<String, String> getCurieLinks() {
    return curieLinks;
  }

  @Override
  public String getLinkRelationTitle(String rel) {
    return linkRelationTitles.get(rel);
  }

  @Override
  public HalCuriAugmenter getCurieAugmenter() {
    return curieAugmenter;
  }

}

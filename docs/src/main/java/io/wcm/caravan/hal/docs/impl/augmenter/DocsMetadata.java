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

import io.wcm.caravan.commons.stream.Streams;
import io.wcm.caravan.hal.docs.impl.model.LinkRelation;
import io.wcm.caravan.hal.docs.impl.model.Service;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableMap;

/**
 * Curie and link relation title metadata extracted from service info metadata.
 */
class DocsMetadata {

  private final Map<String, String> curieLinks;
  private final Map<String, String> linkRelationTitles;

  /**
   * @param serviceModel Service model
   * @param docsPath Documenation URI base path
   */
  public DocsMetadata(Service serviceModel, String docsPath) {
    curieLinks = buildCurieLinkMap(serviceModel, docsPath);
    linkRelationTitles = buildLinkRelationTitlesMap(serviceModel);
  }

  private static Map<String, String> buildCurieLinkMap(Service serviceModel, String docsPath) {
    Map<String, String> map = new HashMap<>();

    Streams.of(serviceModel.getLinkRelations())
    .map(rel -> CurieUtil.getCurieName(rel.getRel()))
    .filter(StringUtils::isNotEmpty)
    .forEach(curie -> map.put(curie, CurieUtil.toDocTemplate(docsPath, curie)));

    return ImmutableMap.copyOf(map);
  }

  private static Map<String, String> buildLinkRelationTitlesMap(Service serviceModel) {
    Map<String, String> map = new HashMap<>();

    for (LinkRelation rel : serviceModel.getLinkRelations()) {
      String title = rel.getShortDescription();
      if (title != null) {
        map.put(rel.getRel(), title);
      }
    }

    return ImmutableMap.copyOf(map);
  }

  public String getCurieLink(String curie) {
    return curieLinks.get(curie);
  }

  public String getLinkRelationTitle(String rel) {
    return linkRelationTitles.get(rel);
  }

}

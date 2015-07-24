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

import static io.wcm.caravan.hal.docs.impl.augmenter.CurieUtil.LINK_RELATION_CURIES;
import io.wcm.caravan.commons.stream.Collectors;
import io.wcm.caravan.commons.stream.Streams;
import io.wcm.caravan.hal.docs.HalDocsAugmenter;
import io.wcm.caravan.hal.docs.impl.model.Service;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.HalResourceFactory;
import io.wcm.caravan.hal.resource.Link;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Sets;

/**
 * Default implementation of {@link HalDocsAugmenter}.
 */
class HalDocsAugmenterImpl implements HalDocsAugmenter {

  private final DocsMetadata metadata;

  public HalDocsAugmenterImpl(Service serviceModel, String docsPath) {
    metadata = new DocsMetadata(serviceModel, docsPath);
  }

  @Override
  public void augment(HalResource resource) {
    Set<String> existingCurieNames = getExistingCurieNames(resource);
    Set<Link> curieLinks = getCurieLinks(resource, existingCurieNames);
    resource.addLinks(LINK_RELATION_CURIES, curieLinks);
  }

  private Set<String> getExistingCurieNames(HalResource hal) {
    if (!hal.hasLink(LINK_RELATION_CURIES)) {
      return Collections.emptySet();
    }
    return Streams.of(hal.getLink(LINK_RELATION_CURIES))
        .map(link -> link.getName())
        .collect(Collectors.toSet());
  }

  private Set<Link> getCurieLinks(HalResource hal, Set<String> existingCurieNames) {
    Set<Link> curiLinks = Sets.newLinkedHashSet();
    curiLinks.addAll(getCurieLinksForCurrentHalResource(hal, existingCurieNames));
    curiLinks.addAll(getCurieLinksForEmbeddedResources(hal, existingCurieNames));
    return curiLinks;
  }

  private List<Link> getCurieLinksForCurrentHalResource(HalResource hal, Set<String> existingCurieNames) {
    return Streams.of(hal.getLinks().keySet())
        // get CURI name for relation
        .map(CurieUtil::getCurieName)
        // filter CURIE being empty or exist in HAL resource
        .filter(curieName -> StringUtils.isNotEmpty(curieName) && !existingCurieNames.contains(curieName))
        // get link for CURI name
        .map(this::toCurieLink)
        // filter non existing links
        .filter(link -> link != null)
        .collect(Collectors.toList());
  }

  private List<Link> getCurieLinksForEmbeddedResources(HalResource hal, Set<String> existingCurieNames) {
    return Streams.of(hal.getEmbedded().values())
        .flatMap(embeddedResource -> Streams.of(getCurieLinks(embeddedResource, existingCurieNames)))
        .collect(Collectors.toList());
  }

  private Link toCurieLink(String curieName) {
    String docLink = metadata.getCurieLink(curieName);
    if (docLink == null) {
      return null;
    }
    else {
      return HalResourceFactory.createLink(docLink).setName(curieName).setTitle("Documentation link");
    }
  }

}

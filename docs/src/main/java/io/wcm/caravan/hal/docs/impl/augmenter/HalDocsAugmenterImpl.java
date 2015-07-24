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

import io.wcm.caravan.hal.docs.HalDocsAugmenter;
import io.wcm.caravan.hal.docs.impl.model.Service;
import io.wcm.caravan.hal.resource.HalResource;

/**
 * Default implementation of {@link HalDocsAugmenter}.
 */
class HalDocsAugmenterImpl implements HalDocsAugmenter {

  private final CurieAugmenter curieAugmenter;
  private final LinkRelationTitleAugmenter linkRelationTitleAugmenter;

  public HalDocsAugmenterImpl(Service serviceModel, String docsPath) {
    DocsMetadata metadata = new DocsMetadata(serviceModel, docsPath);
    curieAugmenter = new CurieAugmenter(metadata);
    linkRelationTitleAugmenter = new LinkRelationTitleAugmenter(metadata);
  }

  @Override
  public void augment(HalResource resource) {
    curieAugmenter.augment(resource);
    linkRelationTitleAugmenter.augment(resource);
  }

}

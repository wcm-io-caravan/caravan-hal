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
import io.wcm.caravan.hal.docs.HalDocsAugmenterFactory;
import io.wcm.caravan.hal.docs.impl.DocsPath;
import io.wcm.caravan.hal.docs.impl.reader.ServiceModelReader;
import io.wcm.caravan.hal.resource.HalResource;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Bundle;

/**
 * Reads service information from model metadata stored in bundle with hal-docs-maven-plugin.
 */
@Component
@Service(value = HalDocsAugmenterFactory.class, serviceFactory = true)
public class HalDocsAugmenterFactoryImpl implements HalDocsAugmenterFactory {

  private static final HalDocsAugmenter NOOP_AUGMENTER = new HalDocsAugmenter() {
    @Override
    public void augment(HalResource resource) {
      // do nothing
    }
  };

  @Override
  public HalDocsAugmenter create(Bundle bundle) {
    if (bundle != null) {
      String docsPath = DocsPath.get(bundle);
      if (docsPath != null) {
        io.wcm.caravan.hal.docs.impl.model.Service serviceModel = ServiceModelReader.read(bundle);
        if (serviceModel != null) {
          return new HalDocsAugmenterImpl(serviceModel, docsPath);
        }
      }
    }
    return NOOP_AUGMENTER;
  }

}

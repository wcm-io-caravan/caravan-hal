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
package io.wcm.caravan.hal.docs.impl.reader;

import io.wcm.caravan.hal.docs.impl.model.Service;

import java.io.InputStream;
import java.net.URL;

import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reader for service model files.
 */
public final class ServiceModelReader {

  /**
   * Classpath prefix where HAL documentation files are stored.
   */
  public static final String DOCS_CLASSPATH_PREFIX = "HAL-DOCS-INF";

  /**
   * Filename with serialized model information for HAL documentation.
   */
  public static final String SERVICE_DOC_FILE = "serviceDoc.json";

  private static final Logger log = LoggerFactory.getLogger(ServiceModelReader.class);

  private ServiceModelReader() {
    // static methods only
  }

  /**
   * Reads service model from bundle classpath.
   * @param bundle Bundle
   * @return Service model or null if not found or not parseable.
   */
  public static Service read(Bundle bundle) {
    String resourcePath = DOCS_CLASSPATH_PREFIX + "/" + SERVICE_DOC_FILE;
    URL bundleResource = bundle.getResource(resourcePath);
    if (bundleResource == null) {
      return null;
    }
    try (InputStream is = bundleResource.openStream()) {
      return new ServiceJson().read(is);
    }
    catch (Throwable ex) {
      log.error("Unable to parse JSON file " + resourcePath + " from bundle " + bundle.getSymbolicName());
      return null;
    }
  }

}

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
package io.wcm.caravan.hal.docs.impl;

import io.wcm.caravan.hal.docs.impl.reader.ServiceModelReader;
import io.wcm.caravan.jaxrs.publisher.ApplicationPath;

import org.osgi.framework.Bundle;

/**
 * Build the documentation base URI.
 */
public final class DocsPath {

  /**
   * Prefix for documentation URIs
   */
  static final String DOCS_URI_PREFIX = "/docs/api";

  private DocsPath() {
    // static methods only
  }

  /**
   * Gets documentation URI path.
   * @param bundle Bundle
   * @return Path or null if not a JAX-RS application bundle or no haldocs metadata exist.
   */
  public static String get(Bundle bundle) {
    String applicationPath = ApplicationPath.get(bundle);
    if (applicationPath == null) {
      return null;
    }
    if (!hasHalDocs(bundle)) {
      return null;
    }
    return get(applicationPath);
  }

  /**
   * Gets documentation URI path
   * @param applicationPath JAX-RS application path
   * @return Path
   */
  public static String get(String applicationPath) {
    return DOCS_URI_PREFIX + applicationPath;
  }

  /**
   * Checks if the given bundle has documentation metadata stored as resource.
   * @param bundle Bundle
   * @return true if metadata found
   */
  public static boolean hasHalDocs(Bundle bundle) {
    return bundle.getResource(ServiceModelReader.DOCS_CLASSPATH_PREFIX + "/" + ServiceModelReader.SERVICE_DOC_FILE) != null;
  }

}

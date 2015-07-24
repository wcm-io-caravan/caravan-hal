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

import org.apache.commons.lang3.StringUtils;

/**
 * Curie and link relation title metadata extracted from service info metadata.
 */
final class CurieUtil {

  /**
   * HAL specific separator for CURI names and relation.
   */
  private static final String LINK_RELATION_SEPARATOR = ":";

  /**
   * Doc URI template suffix for curie links
   */
  private static final String CURIE_DOC_TEMPLATE_SUFFIX = ":{rel}";

  /**
   * HAL specific relation for CURI links.
   */
  static final String LINK_RELATION_CURIES = "curies";


  private CurieUtil() {
    // static methods only
  }

  public static String getCurieName(String rel) {
    if (StringUtils.contains(rel, LINK_RELATION_SEPARATOR)) {
      return StringUtils.substringBefore(rel, LINK_RELATION_SEPARATOR);
    }
    return rel;
  }

  public static String toDocTemplate(String docsPath, String curie) {
    return docsPath + "/" + curie + CURIE_DOC_TEMPLATE_SUFFIX;
  }

}

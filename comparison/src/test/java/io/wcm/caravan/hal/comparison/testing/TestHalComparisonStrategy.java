/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2018 wcm.io
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
package io.wcm.caravan.hal.comparison.testing;

import java.util.HashSet;
import java.util.Set;

import io.wcm.caravan.hal.comparison.HalComparisonStrategy;
import io.wcm.caravan.hal.comparison.HalPath;

public class TestHalComparisonStrategy implements HalComparisonStrategy {

  private final Set<String> embeddedRelationsToIgnore = new HashSet<>();

  private final Set<String> linkRelationsToIgnore = new HashSet<>();

  public HalComparisonStrategy addEmbeddedRelationToIgnore(String relation) {
    embeddedRelationsToIgnore.add(relation);
    return this;
  }

  public HalComparisonStrategy addLinkRelationToIgnore(String relation) {
    linkRelationsToIgnore.add(relation);
    return this;
  }

  @Override
  public boolean ignoreEmbeddedAt(HalPath halPath) {
    return embeddedRelationsToIgnore.contains(halPath.getLastRelation());
  }

  @Override
  public boolean ignoreLinkTo(HalPath halPath) {
    return linkRelationsToIgnore.contains(halPath.getLastRelation());
  }

}

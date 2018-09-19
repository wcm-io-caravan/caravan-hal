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
package io.wcm.caravan.hal.comparison.impl;

import java.util.List;

import com.google.common.collect.ImmutableList;

import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.impl.embedded.EmbeddedProcessing;
import io.wcm.caravan.hal.comparison.impl.links.LinkProcessing;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import rx.Observable;

/**
 * Immutable result class used by the {@link EmbeddedProcessing} and {@link LinkProcessing} interfaces, that combines
 * the two result of the pre-processing for embedded resources and links:
 * <ul>
 * <li>Filter out links/resources that shouldn't be compared, and find corresponding pairs</li>
 * <li>Collect differences that don't require/allow looking into the resources (e.g. resource has been removed)</li>
 * </ul>
 * @param <T> usually {@link Link} or {@link HalResource}
 */
public class ProcessingResult<T> {

  private final List<PairWithRelation<T>> pairsToCompare;
  private final List<HalDifference> differences;

  /**
   * @param pairsToCompare the pairs of corresponding items that should be compared with each other (and have not been
   *          filtered out during pre-processing)
   * @param differences the list of differences that were already discovered during pre-processing
   */
  public ProcessingResult(List<PairWithRelation<T>> pairsToCompare, List<HalDifference> differences) {
    this.pairsToCompare = ImmutableList.copyOf(pairsToCompare);
    this.differences = ImmutableList.copyOf(differences);
  }

  /**
   * @return the pairs of corresponding items that should be compared with each other (and have not been filtered out
   *         during pre-processing)
   */
  public Observable<PairWithRelation<T>> getPairsToCompare() {
    return Observable.from(pairsToCompare);
  }

  /**
   * @return the list of differences that were already discovered during pre-processing
   */
  public Observable<HalDifference> getDifferences() {
    return Observable.from(differences);
  }
}

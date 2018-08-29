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

import org.osgi.service.component.annotations.Component;

import io.wcm.caravan.hal.comparison.HalComparison;
import io.wcm.caravan.hal.comparison.HalComparisonConfig;
import io.wcm.caravan.hal.comparison.HalComparisonSource;
import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.impl.embedded.EmbeddedProcessing;
import io.wcm.caravan.hal.comparison.impl.embedded.EmbeddedProcessingImpl;
import io.wcm.caravan.hal.comparison.impl.halpath.HalPathImpl;
import io.wcm.caravan.hal.comparison.impl.links.LinkProcessing;
import io.wcm.caravan.hal.comparison.impl.links.LinkProcessingImpl;
import io.wcm.caravan.hal.comparison.impl.properties.PropertyDiffDetector;
import io.wcm.caravan.hal.comparison.impl.properties.PropertyProcessing;
import io.wcm.caravan.hal.resource.HalResource;
import rx.Observable;
import rx.Single;

/**
 * Implementation of the {@link HalComparison} OSGi service that glues the other implementation classes together,
 * loads the API entry points and starts the recursive comparison.
 * @see HalComparisonRecursionImpl
 * @see PropertyDiffDetector
 * @see EmbeddedProcessingImpl
 * @see LinkProcessingImpl
 */
@Component(service = HalComparison.class)
public class HalComparisonImpl implements HalComparison {

  @Override
  public Observable<HalDifference> compare(HalComparisonSource expected, HalComparisonSource actual, HalComparisonConfig config) {

    HalComparisonRecursionImpl recursion = wireImplementationClasses(expected, actual);

    HalComparisonContext context = createContextForEntryPoint(expected, actual, config);

    return loadEntryPointsAndStartRecursion(recursion, context, expected, actual);
  }

  private HalComparisonRecursionImpl wireImplementationClasses(HalComparisonSource expected, HalComparisonSource actual) {

    PropertyProcessing propertyProcessing = new PropertyDiffDetector();
    EmbeddedProcessing embeddedProcessing = new EmbeddedProcessingImpl();
    LinkProcessing linkProcessing = new LinkProcessingImpl();

    return new HalComparisonRecursionImpl(expected, actual, propertyProcessing, embeddedProcessing, linkProcessing);
  }

  private HalComparisonContext createContextForEntryPoint(HalComparisonSource expected, HalComparisonSource actual, HalComparisonConfig config) {

    String expectedUrl = expected.getEntryPointUrl();
    String actualUrl = actual.getEntryPointUrl();

    return new HalComparisonContext(config, new HalPathImpl(), expectedUrl, actualUrl);
  }

  private Observable<HalDifference> loadEntryPointsAndStartRecursion(HalComparisonRecursionImpl recursion, HalComparisonContext context,
      HalComparisonSource expected, HalComparisonSource actual) {

    Single<HalResource> expectedEntryPoint = expected.resolveLink(context.getExpectedUrl());
    Single<HalResource> actualEntryPoint = actual.resolveLink(context.getActualUrl());

    // wait until both entry points are loaded before starting the comparison
    return expectedEntryPoint.zipWith(actualEntryPoint, (e, a) -> recursion.compareRecursively(context, e, a))
        // flatten the Single<Observable<HalDifference>> returned by zipWith
        .flatMapObservable(r -> r);
  }
}

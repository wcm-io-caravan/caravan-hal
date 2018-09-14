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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.wcm.caravan.hal.comparison.HalComparisonSource;
import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.HalComparisonContext;
import io.wcm.caravan.hal.comparison.impl.context.HalComparisonContextImpl;
import io.wcm.caravan.hal.comparison.impl.embedded.EmbeddedProcessing;
import io.wcm.caravan.hal.comparison.impl.links.LinkProcessing;
import io.wcm.caravan.hal.comparison.impl.properties.PropertyProcessing;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import rx.Observable;

/**
 * Implements the main recursion logic and asynchronous loading of resources (but delegates the actual comparison via
 * the {@link PropertyProcessing}, {@link EmbeddedProcessing} and {@link LinkProcessing} interfaces)
 */
public class HalComparisonRecursionImpl {

  private final HalComparisonSource expectedSource;
  private final HalComparisonSource actualSource;

  private final PropertyProcessing propertyProcessing;
  private final EmbeddedProcessing embeddedProcessing;
  private final LinkProcessing linkProcessing;

  private final Set<String> expectedUrlsToIgnore = Collections.synchronizedSet(new HashSet<>());

  HalComparisonRecursionImpl(HalComparisonSource expectedSource, HalComparisonSource actualSource,
      PropertyProcessing propertyProcessing, EmbeddedProcessing embeddedProcessing, LinkProcessing linkProcessing) {

    this.expectedSource = expectedSource;
    this.actualSource = actualSource;

    this.propertyProcessing = propertyProcessing;
    this.embeddedProcessing = embeddedProcessing;
    this.linkProcessing = linkProcessing;

    this.expectedUrlsToIgnore.add(expectedSource.getEntryPointUrl());
  }

  /**
   * This method is initially called by the {@link HalComparisonImpl} with the API entry point resources, and then again
   * for each embedded and linked resource that is to be processed (as decided via the {@link EmbeddedProcessing} and
   * {@link LinkProcessing} interfaces)
   * @param context specifies in which part of the tree the comparison is currently being executed
   * @param expected the "ground truth" resource
   * @param actual the resource to be compared against the ground truth
   * @return an {@link Observable} that emits one {@link HalDifference} object for each difference that was detected in
   *         the given resources (and its linked/embedded resources)
   */
  public Observable<HalDifference> compareRecursively(HalComparisonContextImpl context, HalResource expected, HalResource actual) {

    // don't follow links to any resources later that are already embedded (and have a self link) in the current resource
    expected.collectLinks("self").stream()
        .map(Link::getHref)
        .forEach(url -> expectedUrlsToIgnore.add(url));

    return collectLocalDifferences(context, expected, actual)
        .concatWith(collectEmbeddedDifferences(context, expected, actual))
        .concatWith(collectLinkedDifferences(context, expected, actual));
  }

  Observable<HalDifference> collectLocalDifferences(HalComparisonContextImpl context, HalResource expected, HalResource actual) {

    List<HalDifference> diffs = propertyProcessing.process(context, expected, actual);

    return Observable.from(diffs);
  }

  Observable<HalDifference> collectEmbeddedDifferences(HalComparisonContextImpl context, HalResource expected, HalResource actual) {

    ProcessingResult<HalResource> processingResult = embeddedProcessing.process(context, expected, actual);

    Observable<HalDifference> diffsFromRecursion = processingResult.getPairsToCompare()
        .concatMap(pair -> recurseWithEmbeddedResourcePair(context, expected, pair));

    return processingResult.getDifferences().concatWith(diffsFromRecursion);
  }

  private Observable<HalDifference> recurseWithEmbeddedResourcePair(HalComparisonContextImpl context, HalResource expected,
      PairWithRelation<HalResource> pair) {

    HalComparisonContextImpl newContext = context.withHalPathOfEmbeddedResource(pair, expected);

    return compareRecursively(newContext, pair.getExpected(), pair.getActual());
  }

  Observable<HalDifference> collectLinkedDifferences(HalComparisonContextImpl context, HalResource expected, HalResource actual) {

    ProcessingResult<Link> processingResult = linkProcessing.process(context, expected, actual);

    Observable<HalDifference> diffsFromRecursion = processingResult.getPairsToCompare()
        .filter(pair -> !expectedUrlsToIgnore.contains(pair.getExpected().getHref()))
        .concatMap(pair -> recurseWithLinkedResourcePair(context, expected, pair));

    return processingResult.getDifferences().concatWith(diffsFromRecursion);
  }

  private Observable<HalDifference> recurseWithLinkedResourcePair(HalComparisonContextImpl context, HalResource parentOfExpected, PairWithRelation<Link> pair) {

    String expectedUrl = pair.getExpected().getHref();
    String actualUrl = pair.getActual().getHref();

    expectedUrlsToIgnore.add(expectedUrl);

    HalComparisonContextImpl newContext = context
        .withHalPathOfLinkedResource(pair, parentOfExpected)
        .withNewExpectedUrl(expectedUrl)
        .withNewActualUrl(actualUrl);

    Observable<HalResource> expectedObs = resolveLinkAndAddContextToErrors(
        expectedSource, expectedUrl, context.getExpectedUrl(), newContext);

    Observable<HalResource> actualObs = resolveLinkAndAddContextToErrors(
        actualSource, actualUrl, context.getActualUrl(), newContext);

    return expectedObs
        // wait for both resources to be retrieved before they can be compared
        .zipWith(actualObs, (expectedResource, actualResource) -> compareRecursively(newContext, expectedResource, actualResource))
        // then flatten the Observable<Observable<HalDifference>> returned by zipWith
        .flatMap(diffs -> diffs);

  }

  private Observable<HalResource> resolveLinkAndAddContextToErrors(HalComparisonSource source, String resourceUrl, String contextUrl,
      HalComparisonContext halContext) {
    return source.resolveLink(resourceUrl)
        // we want to wrap any exceptions thrown when resolving this link and add more context information
        // (e.g. where the link that could not be resolved can be found)
        // this seems to be only possible with Observable's #onErrorResumeNext,
        // not Single#onErrorReturn (which leads to a CompositeException arriving at the subscriber
        // that makes it a lot harder to understand the causal chain)
        .toObservable()
        .onErrorResumeNext(ex -> {
          throw new HalComparisonException(halContext, contextUrl, ex);
        });
  }

}

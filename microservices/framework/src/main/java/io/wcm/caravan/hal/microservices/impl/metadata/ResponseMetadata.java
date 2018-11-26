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
package io.wcm.caravan.hal.microservices.impl.metadata;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

import io.wcm.caravan.hal.api.annotations.StandardRelations;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

public class ResponseMetadata implements RequestMetricsCollector {

  private final Stopwatch overalResponseTImeStopwatch = Stopwatch.createStarted();

  private final List<TimeMeasurement> inputMaxAgeSeconds = Collections.synchronizedList(new ArrayList<>());
  private final List<TimeMeasurement> inputResponseTimes = Collections.synchronizedList(new ArrayList<>());
  private final List<TimeMeasurement> methodInvocationTimes = Collections.synchronizedList(new ArrayList<>());

  private final List<Link> sourceLinks = Collections.synchronizedList(new ArrayList<>());

  private Integer maxAgeOverride;

  @Override
  public void onResponseRetrieved(String resourceUri, String resourceTitle, int maxAgeSeconds, long responseTimeMicros) {
    inputMaxAgeSeconds.add(new TimeMeasurement(resourceUri, maxAgeSeconds / 1.f, TimeUnit.SECONDS));
    inputResponseTimes.add(new TimeMeasurement(resourceUri, responseTimeMicros / 1000.f, TimeUnit.MILLISECONDS));

    Link link = new Link(resourceUri);
    link.setTitle(resourceTitle);
    sourceLinks.add(link);
  }

  @Override
  public void onMethodInvocationFinished(String methodDescription, long invocationDurationMicros) {
    // TODO: Auto-generated method stub
  }


  /**
   * @return the min max-age value of all responses that have been retrieved, or 365 days if no responses have been
   *         fetched,
   *         or none of them had a max-age header
   */
  @Override
  public int getOutputMaxAge() {

    if (maxAgeOverride != null) {
      return maxAgeOverride;
    }

    return inputMaxAgeSeconds.stream()
        // find the max-age values of all requested resources
        .mapToInt(triple -> Math.round(triple.getTime()))
        .filter(maxAge -> maxAge >= 0)
        // get the minimum max age time
        .min()
        // if no resources have been requested then consider the resource to be immutable)
        .orElse((int)TimeUnit.DAYS.toSeconds(365));
  }


  List<TimeMeasurement> getSortedInputMaxAgeSeconds() {
    return TimeMeasurement.LONGEST_TIME_FIRST.sortedCopy(inputMaxAgeSeconds);
  }

  List<TimeMeasurement> getSortedInputResponseTimes() {
    return TimeMeasurement.LONGEST_TIME_FIRST.sortedCopy(inputResponseTimes);
  }

  List<TimeMeasurement> getSortedMethodInvocationTimes() {
    return TimeMeasurement.LONGEST_TIME_FIRST.sortedCopy(methodInvocationTimes);
  }

  List<TimeMeasurement> getGroupedAndSortedInvocationTimes() {

    List<TimeMeasurement> invocationTimes = getSortedMethodInvocationTimes();

    List<TimeMeasurement> groupedInvocationTimes = new ArrayList<>();
    invocationTimes.stream()
        .collect(Collectors.groupingBy(TimeMeasurement::getText))
        .forEach((text, measurements) -> {
          double totalTime = measurements.stream().mapToDouble(TimeMeasurement::getTime).sum();
          long invocations = measurements.stream().count();

          groupedInvocationTimes.add(new TimeMeasurement(invocations + "x " + text, (float)totalTime, measurements.get(0).getUnit()));
        });

    groupedInvocationTimes.sort(TimeMeasurement.LONGEST_TIME_FIRST);

    return groupedInvocationTimes;
  }

  float getOverallResponseTimeMillis() {
    return overalResponseTImeStopwatch.elapsed(TimeUnit.MICROSECONDS) / 1000.f;
  }

  float getSumOfResponseTimeMillis() {
    return (float)inputResponseTimes.stream()
        .mapToDouble(m -> m.getTime())
        .sum();
  }

  float getSumOfProxyInvocationMillis() {
    return (float)methodInvocationTimes.stream()
        .mapToDouble(m -> m.getTime())
        .sum();
  }

  float getMaximumInvocationMillis() {
    return (float)methodInvocationTimes.stream()
        .mapToDouble(m -> m.getTime())
        .max().orElse(0);
  }

  float getRemainingCalculationMillis() {
    return getOverallResponseTimeMillis() - getSumOfProxyInvocationMillis() - getMaximumInvocationMillis();
  }

  List<Link> getSourceLinks() {
    return ImmutableList.copyOf(sourceLinks);
  }

  /**
   * @param resourceImpl the resource implementation that was used to generate the resource
   * @return a new {@link HalResource} instance with detailed information about timing and caching for all upstream
   *         resources accessed while handling the current request
   */
  @Override
  public HalResource createMetadataResource(LinkableResource resourceImpl) {
    Stopwatch stopwatch = Stopwatch.createStarted();

    HalResource metadataResource = new HalResource();

    metadataResource.getModel().put("title", "Detailed information about the performance and input data for this request");
    metadataResource.getModel().put("class", resourceImpl.getClass().getName());

    HalResource linksResource = new HalResource();
    linksResource.getModel().put("title", "Links to all SDL resources that were retrieved to generate this resource");
    linksResource.getModel().put("developerHint", "If you see lots of untitled resources here then free feel to add a title "
        + "to the self link in that resource in the upstream service.");
    linksResource.addLinks(StandardRelations.VIA, getSourceLinks());
    metadataResource.addEmbedded("sdl:sourceLinks", linksResource);

    HalResource responseTimeResource = createTimingResource(getSortedInputResponseTimes());
    responseTimeResource.getModel().put("title", "The individual response & parse times of all retrieved SDL resources");
    responseTimeResource.getModel().put("developerHint", "Response times > ~20ms usually indicate that the resource was not found in cache"
        + " - a reload of this resource should then be much faser. "
        + "If you see many individual requests here then check if the upstream "
        + "service also provides a way to fetch this data all at once. ");
    metadataResource.addEmbedded("metrics:responseTimes", responseTimeResource);

    HalResource invocationTimeResource = createTimingResource(getGroupedAndSortedInvocationTimes());
    invocationTimeResource.getModel().put("title", "A breakdown of the time spent in HalApiClient proxy methods");
    invocationTimeResource.getModel().put("developerHint",
        "If a lot of time is spent in a method that is invoked very often, then you should check if you can "
            + "use Observable#cache() and share the same observable in different methods of this resource. "
            + "If this resource has embedded resource than you might need to pass that observable to the constructor of the embedded resource. ");
    metadataResource.addEmbedded("metrics:invocationTimes", invocationTimeResource);

    HalResource maxAgeResource = createTimingResource(getSortedInputMaxAgeSeconds());
    maxAgeResource.getModel().put("title", "The max-age cache header values of all retrieved SDL resources");
    maxAgeResource.getModel().put("developerHint", "If the max-age in this response's cache headers is lower then you expected, "
        + "then check the resources at the very bottom of the list, because they will determine the overall max-age time.");
    metadataResource.addEmbedded("metrics:maxAge", maxAgeResource);


    // and also include the overall max-age of the response

    metadataResource.getModel().put("maxAge", getOutputMaxAge() + "s");

    metadataResource.getModel().put("sumOfProxyInvocationTime", getSumOfProxyInvocationMillis() + "ms");
    metadataResource.getModel().put("sumOfResponseAndParseTimes", getSumOfResponseTimeMillis() + "ms");
    metadataResource.getModel().put("metadataGenerationTime", stopwatch.elapsed(TimeUnit.MILLISECONDS) + "ms");
    metadataResource.getModel().put("overallServerSideResponseTime", getOverallResponseTimeMillis() + "ms");

    metadataResource.getModel().put("threadCount", ManagementFactory.getThreadMXBean().getThreadCount());

    return metadataResource;
  }

  private static HalResource createTimingResource(List<TimeMeasurement> map) {

    ObjectNode model = JsonNodeFactory.instance.objectNode();
    ArrayNode individualMetrics = model.putArray("timeMeasurements");

    map.stream()
        .map(measurement -> measurement.getTime() + " " + measurement.getUnit().toString() + " - " + measurement.getText())
        .forEach(title -> individualMetrics.add(title));

    return new HalResource(model);
  }

  /**
   * Composition of a time value with unit and description
   */
  public static class TimeMeasurement {

    static final Ordering<TimeMeasurement> LONGEST_TIME_FIRST = Ordering.natural().onResultOf(TimeMeasurement::getTime).reverse();

    private final String text;
    private final Float time;
    private final TimeUnit unit;

    TimeMeasurement(String text, Float time, TimeUnit unit) {
      this.text = text;
      this.time = time;
      this.unit = unit;
    }

    public String getText() {
      return this.text;
    }

    public Float getTime() {
      return this.time;
    }

    public TimeUnit getUnit() {
      return this.unit;
    }
  }

}

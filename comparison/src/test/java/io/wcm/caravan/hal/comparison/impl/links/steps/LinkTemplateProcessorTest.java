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
package io.wcm.caravan.hal.comparison.impl.links.steps;

import static io.wcm.caravan.hal.comparison.testing.StandardRelations.ITEM;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.impl.HalComparisonContext;
import io.wcm.caravan.hal.comparison.impl.TestHalComparisonContext;
import io.wcm.caravan.hal.resource.Link;


public class LinkTemplateProcessorTest {

  private LinkTemplateProcessor processor;
  private HalComparisonContext context;

  @Before
  public void setUp() {
    processor = new LinkTemplateProcessor();

    // the LinkProcessingImpl will already add the relation to the contet before calling its steps, so we do the same here
    context = new TestHalComparisonContext().withAppendedHalPath(ITEM);
  }

  private List<Link> createLinks(String... paths) {
    return Stream.of(paths)
        .map(Link::new)
        .collect(Collectors.toList());
  }

  @Test
  public void resolved_links_should_be_preserved() throws Exception {

    List<Link> expected = createLinks("/path1");
    List<Link> actual = createLinks("/path2");

    List<HalDifference> diff = processor.apply(context, ITEM, expected, actual);

    // no comparison errors should be triggered, and the links shouldn't have been filtered
    assertThat(diff, empty());
    assertThat(expected, hasSize(1));
    assertThat(actual, hasSize(1));
  }

  @Test
  public void identical_templates_should_be_filtered() throws Exception {

    List<Link> expected = createLinks("/{?queryParam}");
    List<Link> actual = createLinks("/{?queryParam}");

    List<HalDifference> diff = processor.apply(context, ITEM, expected, actual);

    // no comparison errors should be triggered
    assertThat(diff, empty());
    // but the links should be removed from the list, as they can't be followed
    assertThat(expected, empty());
    assertThat(actual, empty());
  }

  @Test
  public void path_and_query_parameters_should_be_considered_equal() throws Exception {

    List<Link> expected = createLinks("/{?param}");
    List<Link> actual = createLinks("/{param}");

    List<HalDifference> diff = processor.apply(context, ITEM, expected, actual);

    // no comparison errors should be triggered
    assertThat(diff, empty());
    // but the links should be removed from the list, as they can't be followed
    assertThat(expected, empty());
    assertThat(actual, empty());
  }

  @Test
  public void resolved_link_instead_of_template_should_be_detected() throws Exception {

    List<Link> expected = createLinks("/{?queryParam}");
    List<Link> actual = createLinks("/");

    List<HalDifference> diff = processor.apply(context, ITEM, expected, actual);

    // a comparison error should be triggered
    assertThat(diff, hasSize(1));
    assertEquals("/item", diff.get(0).getHalPath().toString());
    // and the links should be removed from the list, as they can't be followed
    assertThat(expected, empty());
    assertThat(actual, empty());
  }

  @Test
  public void template_instead_of_resolved_link_should_be_detected() throws Exception {

    List<Link> expected = createLinks("/");
    List<Link> actual = createLinks("/{?queryParam}");

    List<HalDifference> diff = processor.apply(context, ITEM, expected, actual);

    // a comparison error should be triggered
    assertThat(diff, hasSize(1));
    assertEquals("/item", diff.get(0).getHalPath().toString());
    // and the links should be removed from the list, as they can't be followed
    assertThat(expected, empty());
    assertThat(actual, empty());
  }

  @Test
  public void missing_template_parameter_should_be_detected() throws Exception {

    List<Link> expected = createLinks("/{?param1,param2}");
    List<Link> actual = createLinks("/{?param1}");

    List<HalDifference> diff = processor.apply(context, ITEM, expected, actual);

    // a comparison error should be triggered
    assertThat(diff, hasSize(1));
    assertEquals("/item", diff.get(0).getHalPath().toString());
    // and the links should be removed from the list, as they can't be followed
    assertThat(expected, empty());
    assertThat(actual, empty());
  }

  @Test
  public void additional_template_parameter_should_be_detected() throws Exception {

    List<Link> expected = createLinks("/{?param1}");
    List<Link> actual = createLinks("/{?param1,param2}");

    List<HalDifference> diff = processor.apply(context, ITEM, expected, actual);

    // a comparison error should be triggered
    assertThat(diff, hasSize(1));
    assertEquals("/item", diff.get(0).getHalPath().toString());
    // and the links should be removed from the list, as they can't be followed
    assertThat(expected, empty());
    assertThat(actual, empty());
  }

  @Test
  public void different_template_parameter_should_be_detected() throws Exception {

    List<Link> expected = createLinks("/{?queryParam}");
    List<Link> actual = createLinks("/{?otherParam}");

    List<HalDifference> diff = processor.apply(context, ITEM, expected, actual);

    // a comparison error should be triggered
    assertThat(diff, hasSize(1));
    assertEquals("/item", diff.get(0).getHalPath().toString());
    // and the links should be removed from the list, as they can't be followed
    assertThat(expected, empty());
    assertThat(actual, empty());
  }

  @Test
  public void additional_link_should_be_preserved() throws Exception {

    List<Link> expected = createLinks("/path1");
    List<Link> actual = createLinks("/path2", "/additional");

    List<HalDifference> diff = processor.apply(context, ITEM, expected, actual);

    // no comparison errors should be triggered, and the links shouldn't have been filtered
    assertThat(diff, empty());
    assertThat(expected, hasSize(1));
    assertThat(actual, hasSize(2));
  }

  @Test
  public void missing_link_should_be_preserved() throws Exception {

    List<Link> expected = createLinks("/path1", "/missing");
    List<Link> actual = createLinks("/path2");

    List<HalDifference> diff = processor.apply(context, ITEM, expected, actual);

    // no comparison errors should be triggered, and the links shouldn't have been filtered
    assertThat(diff, empty());
    assertThat(expected, hasSize(2));
    assertThat(actual, hasSize(1));
  }

  @Test
  public void additional_template_should_be_filtered() throws Exception {

    List<Link> expected = createLinks("/path1");
    List<Link> actual = createLinks("/path2", "/{?queryParam}");

    List<HalDifference> diff = processor.apply(context, ITEM, expected, actual);

    // no comparison errors should be triggered, but the link template should be filtered
    assertThat(diff, empty());
    assertThat(expected, hasSize(1));
    assertThat(actual, hasSize(1));
  }

  @Test
  public void missing_template_should_be_filtered() throws Exception {

    List<Link> expected = createLinks("/path1", "/{?queryParam}");
    List<Link> actual = createLinks("/path2");

    List<HalDifference> diff = processor.apply(context, ITEM, expected, actual);

    // no comparison errors should be triggered, but the link template should be filtered
    assertThat(diff, empty());
    assertThat(expected, hasSize(1));
    assertThat(actual, hasSize(1));
  }
}

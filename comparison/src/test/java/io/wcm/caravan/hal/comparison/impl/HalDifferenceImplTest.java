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

import static io.wcm.caravan.hal.comparison.testing.StandardRelations.ITEM;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import io.wcm.caravan.hal.comparison.impl.context.HalComparisonContextImpl;
import io.wcm.caravan.hal.comparison.impl.context.HalPathImpl;
import io.wcm.caravan.hal.comparison.testing.TestHalComparisonStrategy;


public class HalDifferenceImplTest {

  private HalPathImpl halPath;
  private HalComparisonContextImpl context;

  @Before
  public void setUp() {
    halPath = new HalPathImpl().append(ITEM, null, null);
    context = new HalComparisonContextImpl(new TestHalComparisonStrategy(), halPath, "/", "/");
  }

  @Test
  public void string_representation_contains_description() throws Exception {

    HalDifferenceImpl diff = new HalDifferenceImpl(context, null, null, "this is the description");
    assertThat(diff.toString(), containsString(diff.getDescription()));
  }

  @Test
  public void string_representation_contains_hal_path() throws Exception {

    HalDifferenceImpl diff = new HalDifferenceImpl(context, null, null, "this is the description");
    assertThat(diff.toString(), containsString(diff.getHalContext().toString()));
  }
}

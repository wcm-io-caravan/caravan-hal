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

import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;

import io.wcm.caravan.hal.comparison.testing.resources.TestResource;
import io.wcm.caravan.hal.resource.HalResource;

/**
 * A mockito matcher that will detect if two HalResource instances are pointing to the same Jackson ObjectNode.
 * Required because methods such as {@link HalResource#getEmbedded(String)} do not guarantee to return the same
 * HalResource instances
 */
public class SameHalResourceMatcher implements ArgumentMatcher<HalResource> {

  private final HalResource expected;

  public SameHalResourceMatcher(HalResource expected) {
    this.expected = expected;
  }

  @Override
  public boolean matches(HalResource other) {
    return expected.getModel() == other.getModel();
  }

  public static HalResource sameHal(HalResource expected) {
    return ArgumentMatchers.argThat(new SameHalResourceMatcher(expected));
  }

  public static HalResource sameHal(TestResource expected) {
    return ArgumentMatchers.argThat(new SameHalResourceMatcher(expected.asHalResource()));
  }
}

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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.List;

import io.wcm.caravan.hal.comparison.HalDifference;
import io.wcm.caravan.hal.comparison.HalDifference.ChangeType;
import io.wcm.caravan.hal.comparison.HalDifference.EntityType;

public final class HalDifferenceAssertions {

  private HalDifferenceAssertions() {
    // utility class
  }

  public static HalDifference assertOnlyOneDifference(List<HalDifference> diffs, ChangeType changeType, EntityType entityType, String halPath) {
    assertThat(diffs, hasSize(1));

    HalDifference diff = diffs.get(0);
    assertDifference(diff, changeType, entityType, halPath);

    return diff;
  }

  public static void assertDifference(HalDifference diff, ChangeType changeType, EntityType entityType, String halPath) {
    assertThat(diff.getChangeType(), equalTo(changeType));
    assertThat(diff.getEntityType(), equalTo(entityType));
    assertThat(diff.getHalContext().toString(), equalTo(halPath));
  }

}

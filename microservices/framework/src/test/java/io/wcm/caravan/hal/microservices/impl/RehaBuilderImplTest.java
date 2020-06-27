/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2020 wcm.io
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
package io.wcm.caravan.hal.microservices.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import io.wcm.caravan.hal.microservices.api.Reha;
import io.wcm.caravan.hal.microservices.api.RehaBuilder;
import io.wcm.caravan.hal.microservices.api.common.HalResponse;
import io.wcm.caravan.hal.microservices.api.server.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.hal.microservices.api.server.HalApiServerException;
import io.wcm.caravan.hal.microservices.testing.LinkableTestResource;
import io.wcm.caravan.hal.resource.Link;

@ExtendWith(MockitoExtension.class)
public class RehaBuilderImplTest {

  private static final String UPSTREAM_ENTRY_POINT_URI = "/";
  private static final String NON_EXISTING_PATH = "/does/not/exist";
  private static final String INCOMING_REQUEST_URI = "/incoming";

  private Reha createRehaWithCustomExceptionStrategy() {
    return RehaBuilder.withoutResourceLoader()
        .withExceptionStrategy(new CustomExceptionStrategy())
        .buildForRequestTo(INCOMING_REQUEST_URI);
  }

  @Test
  public void withExceptionStrategy_should_apply_custom_exception_strategy() {

    Reha reha = createRehaWithCustomExceptionStrategy();

    NotImplementedException ex = new NotImplementedException("Foo");

    HalResponse response = reha.renderResponse(new FailingResourceImpl(ex));

    assertThat(response.getStatus()).isEqualTo(501);
  }


  @Test
  public void withExceptionStrategy_should_not_disable_default_exception_strategy() {

    Reha reha = createRehaWithCustomExceptionStrategy();

    HalApiServerException ex = new HalApiServerException(404, "Not Found");

    HalResponse response = reha.renderResponse(new FailingResourceImpl(ex));

    assertThat(response.getStatus()).isEqualTo(404);
  }

  private static final class CustomExceptionStrategy implements ExceptionStatusAndLoggingStrategy {

    @Override
    public Integer extractStatusCode(Throwable error) {
      if (error instanceof NotImplementedException) {
        return 501;
      }
      return null;
    }
  }

  private static final class FailingResourceImpl implements LinkableTestResource {

    private final RuntimeException ex;

    private FailingResourceImpl(RuntimeException ex) {
      this.ex = ex;
    }

    @Override
    public Link createLink() {
      throw this.ex;
    }
  }

}

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
package io.wcm.caravan.hal.microservices.jaxrs;

import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Path;

import org.junit.Test;

import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.microservices.jaxrs.JaxRsLinkBuilderTestWithAnnotatedBeanParamTest.TwoPathParametersBean;
import io.wcm.caravan.hal.microservices.jaxrs.JaxRsLinkBuilderTestWithAnnotatedBeanParamTest.TwoQueryParametersBean;
import io.wcm.caravan.hal.resource.Link;

public class JaxRsLinkBuilderTestWithBeanConstructorParamTest extends JaxRsLinkBuilderTest {


  @Path(RESOURCE_PATH)
  private static class TestResourceWithTwoQueryParameters extends LinkableResourceAdapter {

    private final TwoQueryParametersBean parameters;

    public TestResourceWithTwoQueryParameters(@BeanParam TwoQueryParametersBean parameters) {
      this.parameters = parameters;
    }
  }

  @Override
  LinkableResource createResourceWithTwoQueryParameters(String valueOfA, String valueOfB) {

    TwoQueryParametersBean parameters = new TwoQueryParametersBean(valueOfA, valueOfB);
    return (new TestResourceWithTwoQueryParameters(parameters));
  }

  @Path(RESOURCE_PATH_TEMPLATE)
  private static class TestResourceWithTwoPathParameters extends LinkableResourceAdapter {

    private final TwoPathParametersBean parameters;

    public TestResourceWithTwoPathParameters(@BeanParam TwoPathParametersBean parameters) {
      this.parameters = parameters;
    }
  }

  @Override
  LinkableResource createResourceWithTwoPathParameters(String valueOfA, String valueOfB) {
    TwoPathParametersBean parameters = new TwoPathParametersBean(valueOfA, valueOfB);
    return new TestResourceWithTwoPathParameters(parameters);
  }


  @Test
  public void should_insert_variables_for_query_parameters_if_bean_param_is_null() throws Exception {

    Link link = buildLinkTo(new TestResourceWithTwoQueryParameters(null));

    assertThat(link.isTemplated());
    assertThat(link.getHref()).endsWith("{?" + QUERY_PARAM_A + "," + QUERY_PARAM_B + "}");
  }

}

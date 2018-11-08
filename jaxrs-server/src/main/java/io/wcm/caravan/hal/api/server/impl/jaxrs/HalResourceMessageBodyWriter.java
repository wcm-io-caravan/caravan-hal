/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2017 wcm.io
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
package io.wcm.caravan.hal.api.server.impl.jaxrs;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.jaxrs.publisher.JaxRsComponent;

/**
 * A MessageBodyWriter that allows JAX-RS service functions to write {@link HalResource} objects to the response
 */
@Component
@Service(value = {
    JaxRsComponent.class, HalResourceMessageBodyWriter.class
}, serviceFactory = true)
@Property(name = JaxRsComponent.PROPERTY_GLOBAL_COMPONENT, value = "true")
@Provider
@Produces({ HalResource.CONTENT_TYPE, MediaType.WILDCARD })
public class HalResourceMessageBodyWriter implements MessageBodyWriter<HalResource>, JaxRsComponent {

  private ObjectMapper objectMapper = new ObjectMapper();
  private JsonFactory jsonFactory = new JsonFactory(objectMapper);


  @Override
  public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return HalResource.class.isAssignableFrom(type);
  }

  @Override
  public long getSize(HalResource t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return -1;
  }

  @Override
  public void writeTo(HalResource t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
      MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {

    JsonGenerator generator = jsonFactory.createGenerator(entityStream);

    generator.writeTree(t.getModel());
  }

}

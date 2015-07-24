/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2015 wcm.io
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
package io.wcm.caravan.hal.docs.impl.reader;

import io.wcm.caravan.hal.docs.impl.model.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Manages serialization and deserialization of service JSON files.
 */
public final class ServiceJson {

  private final ObjectMapper objectMapper = new ObjectMapper()
      .setSerializationInclusion(Include.NON_EMPTY);

  /**
   * Constructor
   */
  public ServiceJson() {
    // ensure only field serialization is used for jackson
    objectMapper.setVisibilityChecker(objectMapper.getSerializationConfig().getDefaultVisibilityChecker()
        .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
        .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
        .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
        .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
  }

  /**
   * Write model to stream.
   * @param model Model
   * @param os Stream
   * @throws IOException
   */
  public void write(Service model, OutputStream os) throws IOException {
    objectMapper.writerWithDefaultPrettyPrinter().writeValue(os, model);
  }

  /**
   * Read model from stream
   * @param is stream
   * @return Model
   * @throws IOException
   */
  public Service read(InputStream is) throws IOException {
    return objectMapper.readValue(is, Service.class);
  }

}

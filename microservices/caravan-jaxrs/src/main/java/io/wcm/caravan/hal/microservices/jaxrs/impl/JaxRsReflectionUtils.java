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
package io.wcm.caravan.hal.microservices.jaxrs.impl;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.tuple.Pair;

public class JaxRsReflectionUtils {

  public static Map<String, Object> getPathParameterMap(Object resourceImpl, Class clazz) {

    Map<String, Object> parameterMap = new LinkedHashMap<>();

    findFieldsWithPathParamAnnotationsIn(resourceImpl, clazz)
        .forEach(pair -> parameterMap.put(pair.getKey(), pair.getValue()));

    getPathParamNamesFromConstructors(clazz)
        .map(name -> Pair.of(name, getFieldValue(name, resourceImpl)))
        .forEach(pair -> parameterMap.put(pair.getKey(), pair.getValue()));

    findFieldsThatContainOtherParamsIn(resourceImpl, clazz)
        .map(beanParamPair -> getPathParameterMap(beanParamPair.getRight(), beanParamPair.getLeft()))
        .forEach(beanParamMap -> parameterMap.putAll(beanParamMap));

    return parameterMap;
  }


  public static Map<String, Object> getQueryParameterMap(Object resourceImpl, Class clazz) {

    Map<String, Object> parameterMap = new LinkedHashMap<>();

    findFieldsWithQueryParamAnnotationIn(resourceImpl, clazz)
        .forEach(pair -> parameterMap.put(pair.getKey(), pair.getValue()));

    getQueryParamNamesFromConstructors(clazz)
        .map(name -> Pair.of(name, getFieldValue(name, resourceImpl)))
        .forEach(pair -> parameterMap.put(pair.getKey(), pair.getValue()));

    findFieldsThatContainOtherParamsIn(resourceImpl, clazz)
        .map(beanParamPair -> getQueryParameterMap(beanParamPair.getRight(), beanParamPair.getLeft()))
        .forEach(beanParamMap -> parameterMap.putAll(beanParamMap));

    return parameterMap;
  }

  private static Stream<Pair<Class, Object>> findFieldsThatContainOtherParamsIn(Object resourceImpl, Class clazz) {
    return findFieldsDefinedInClass(clazz).stream()
        .filter(field -> {
          return FieldUtils.getAllFieldsList(field.getType()).stream()
              .anyMatch(nestedField -> nestedField.getAnnotation(QueryParam.class) != null || nestedField.getAnnotation(PathParam.class) != null);
        })
        .map(field -> Pair.of(field.getType(), getFieldValue(field, resourceImpl)));
  }

  private static Stream<Pair<String, Object>> findFieldsWithPathParamAnnotationsIn(Object resourceImpl, Class clazz) {
    return findFieldsDefinedInClass(clazz).stream()
        .filter(field -> field.getAnnotation(PathParam.class) != null)
        .map(field -> Pair.of(field.getAnnotation(PathParam.class).value(), getFieldValue(field, resourceImpl)));
  }

  private static Stream<Pair<String, Object>> findFieldsWithQueryParamAnnotationIn(Object resourceImpl, Class clazz) {
    return findFieldsDefinedInClass(clazz).stream()
        .filter(field -> field.getAnnotation(QueryParam.class) != null)
        .map(field -> Pair.of(field.getAnnotation(QueryParam.class).value(), getFieldValue(field, resourceImpl)));
  }

  private static Stream<String> getPathParamNamesFromConstructors(Class<?> clazz) {

    return Stream.of(clazz.getConstructors())
        .filter(c -> c.getParameterCount() > 0)
        .flatMap(c -> Stream.of(c.getParameters()))
        .map(p -> {
          PathParam qp = p.getAnnotation(PathParam.class);
          if (qp == null) {
            return null;
          }
          return qp.value();
        })
        .filter(Objects::nonNull)
        .distinct();
  }

  private static Stream<String> getQueryParamNamesFromConstructors(Class<?> clazz) {

    return Stream.of(clazz.getConstructors())
        .filter(c -> c.getParameterCount() > 0)
        .flatMap(c -> Stream.of(c.getParameters()))
        .map(p -> {
          QueryParam qp = p.getAnnotation(QueryParam.class);
          if (qp == null) {
            return null;
          }
          return qp.value();
        })
        .filter(Objects::nonNull)
        .distinct();
  }

  private static List<Field> findFieldsDefinedInClass(Class clazz) {

    return FieldUtils.getAllFieldsList(clazz);
  }

  private static Object getFieldValue(String name, Object instance) {
    if (instance == null) {
      return null;
    }
    Field field = FieldUtils.getField(instance.getClass(), name, true);
    return getFieldValue(field, instance);
  }

  private static Object getFieldValue(Field field, Object instance) {
    if (instance == null) {
      return null;
    }
    try {
      return FieldUtils.readField(field, instance, true);
    }
    catch (IllegalArgumentException | IllegalAccessException ex) {
      throw new RuntimeException("Failed to access field " + field.getName() + " of class " + instance.getClass().getName() + " through reflection", ex);
    }
  }

}

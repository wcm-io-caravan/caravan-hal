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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;

public class FastJaxRsReflectionUtils {

  private static final Cache<Class, ParameterMapProvider> PATH_PARAM_CACHE = CacheBuilder.newBuilder().maximumSize(100).build();
  private static final Cache<Class, ParameterMapProvider> QUERY_PARAM_CACHE = CacheBuilder.newBuilder().maximumSize(100).build();

  private static final class BeanFieldValueProvider implements ParamValueProvider {

    private final String name;
    private final Field beanField;


    private BeanFieldValueProvider(String name, Field beanField) {
      this.name = name;
      this.beanField = beanField;
    }

    @Override
    public String getParamName() {
      return name;
    }

    @Override
    public Object getParamValue(Object resourceInstance) {
      Object beanValue = getFieldValue(beanField, resourceInstance);

      return getFieldValue(name, beanValue);
    }
  }

  private static final class InstanceFieldValueProvider implements ParamValueProvider {

    private final String name;
    private final Field field;

    private InstanceFieldValueProvider(String name, Class clazz) {
      this.name = name;
      this.field = getField(clazz, name);
    }

    private InstanceFieldValueProvider(String name, Field field) {
      this.name = name;
      this.field = field;
    }

    @Override
    public String getParamName() {
      return name;
    }

    @Override
    public Object getParamValue(Object resourceInstance) {
      return getFieldValue(field, resourceInstance);
    }
  }

  private static class ParameterMapProvider {

    private final Collection<ParamValueProvider> namedValueProviders;

    ParameterMapProvider(Collection<ParamValueProvider>... namedValueProviders) {
      this.namedValueProviders = Stream.of(namedValueProviders).flatMap(collection -> collection.stream()).collect(Collectors.toList());
    }

    Collection<ParamValueProvider> getNamedValueProviders() {
      return namedValueProviders;
    }

    Map<String, Object> getParameterMap(Object resourceImpl) {

      Map<String, Object> parameterMap = new HashMap<>();

      namedValueProviders.forEach(provider -> parameterMap.put(provider.getParamName(), provider.getParamValue(resourceImpl)));

      return parameterMap;
    }
  }

  private interface ParamValueProvider {

    String getParamName();

    Object getParamValue(Object resourceInstance);
  }

  public static Map<String, Object> getPathParameterMap(Object resourceImpl, Class clazz) {

    return getOrCreatePathParamProvider(clazz)
        .getParameterMap(resourceImpl);
  }


  public static Map<String, Object> getQueryParameterMap(Object resourceImpl, Class clazz) {

    return getOrCreateQueryParamProvider(clazz)
        .getParameterMap(resourceImpl);
  }

  private static ParameterMapProvider getOrCreatePathParamProvider(Class clazz) {
    try {
      return PATH_PARAM_CACHE.get(clazz, () -> createPathParamProvider(clazz));
    }
    catch (ExecutionException | UncheckedExecutionException ex) {
      throw new RuntimeException("Failed to inspect fields corresponding to @PathParams in " + clazz, ex);
    }
  }


  private static ParameterMapProvider createPathParamProvider(Class clazz) {
    Collection<ParamValueProvider> constructorValueProviders = createParamValueProvider(clazz, getPathParamNamesFromConstructors(clazz));
    Collection<ParamValueProvider> annotatedFieldValueProviders = createParamValueProvider(findFieldsWithPathParamAnnotationsIn(clazz));

    Collection<ParamValueProvider> beanProviders = findFieldsThatContainOtherParamsIn(clazz)
        .flatMap(beanField -> {
          ParameterMapProvider providers = createPathParamProvider(beanField.getType());
          return providers.getNamedValueProviders().stream()
              .map(provider -> new BeanFieldValueProvider(provider.getParamName(), beanField));
        })
        .collect(Collectors.toList());

    return new ParameterMapProvider(constructorValueProviders, annotatedFieldValueProviders, beanProviders);
  }

  private static ParameterMapProvider getOrCreateQueryParamProvider(Class clazz) {
    try {
      return QUERY_PARAM_CACHE.get(clazz, () -> createQueryParamProvider(clazz));
    }
    catch (ExecutionException | UncheckedExecutionException ex) {
      throw new RuntimeException("Failed to inspect fields corresponding to @PathParams in " + clazz, ex);
    }
  }

  private static ParameterMapProvider createQueryParamProvider(Class clazz) {
    Collection<ParamValueProvider> constructorValueProviders = createParamValueProvider(clazz, getQueryParamNamesFromConstructors(clazz));
    Collection<ParamValueProvider> annotatedFieldValueProviders = createParamValueProvider(findFieldsWithQueryParamAnnotationIn(clazz));

    Collection<ParamValueProvider> beanProviders = findFieldsThatContainOtherParamsIn(clazz)
        .flatMap(beanField -> {
          ParameterMapProvider providers = createQueryParamProvider(beanField.getType());
          return providers.getNamedValueProviders().stream()
              .map(provider -> new BeanFieldValueProvider(provider.getParamName(), beanField));
        })
        .collect(Collectors.toList());


    return new ParameterMapProvider(constructorValueProviders, annotatedFieldValueProviders, beanProviders);
  }

  private static Collection<ParamValueProvider> createParamValueProvider(Class clazz, Stream<String> fieldNames) {
    return fieldNames
        .map(name -> new InstanceFieldValueProvider(name, clazz))
        .collect(Collectors.toList());
  }

  private static Collection<ParamValueProvider> createParamValueProvider(Stream<Pair<String, Field>> namedFields) {
    return namedFields
        .map(pair -> new InstanceFieldValueProvider(pair.getKey(), pair.getValue()))
        .collect(Collectors.toList());
  }


  private static Stream<Field> findFieldsThatContainOtherParamsIn(Class clazz) {
    return findFieldsDefinedInClass(clazz).stream()
        .filter(field -> {
          return FieldUtils.getAllFieldsList(field.getType()).stream()
              .anyMatch(nestedField -> nestedField.getAnnotation(QueryParam.class) != null || nestedField.getAnnotation(PathParam.class) != null);
        });
  }

  private static Stream<Pair<String, Field>> findFieldsWithPathParamAnnotationsIn(Class clazz) {
    return findFieldsDefinedInClass(clazz).stream()
        .filter(field -> field.getAnnotation(PathParam.class) != null)
        .map(field -> Pair.of(field.getAnnotation(PathParam.class).value(), field));
  }

  private static Stream<Pair<String, Field>> findFieldsWithQueryParamAnnotationIn(Class clazz) {
    return findFieldsDefinedInClass(clazz).stream()
        .filter(field -> field.getAnnotation(QueryParam.class) != null)
        .map(field -> Pair.of(field.getAnnotation(QueryParam.class).value(), field));
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
    Class clazz = instance.getClass();
    Field field = getField(clazz, name);
    return getFieldValue(field, instance);
  }

  private static Field getField(Class clazz, String name) {
    return FieldUtils.getField(clazz, name, true);
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

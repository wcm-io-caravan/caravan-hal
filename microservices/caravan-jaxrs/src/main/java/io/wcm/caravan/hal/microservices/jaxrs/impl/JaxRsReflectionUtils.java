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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
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

public class JaxRsReflectionUtils {

  private static final Cache<Class, PathParamFinder> PATH_PARAM_FINDER_CACHE = CacheBuilder.newBuilder().maximumSize(100).build();
  private static final Cache<Class, QueryParamFinder> QUERY_PARAM_FINDER_CACHE = CacheBuilder.newBuilder().maximumSize(100).build();

  public static Map<String, Object> getPathParameterMap(Object resourceImpl, Class clazz) {
    try {
      PathParamFinder finder = PATH_PARAM_FINDER_CACHE.get(clazz, () -> new PathParamFinder(clazz));
      return finder.getParameterMap(resourceImpl);
    }
    catch (ExecutionException | UncheckedExecutionException ex) {
      throw new RuntimeException("Failed to inspect fields corresponding to @PathParams in " + clazz, ex);
    }
  }


  public static Map<String, Object> getQueryParameterMap(Object resourceImpl, Class clazz) {
    try {
      QueryParamFinder finder = QUERY_PARAM_FINDER_CACHE.get(clazz, () -> new QueryParamFinder(clazz));
      return finder.getParameterMap(resourceImpl);
    }
    catch (ExecutionException | UncheckedExecutionException ex) {
      throw new RuntimeException("Failed to inspect fields corresponding to @QueryParams in " + clazz, ex);
    }
  }


  private abstract static class AnnotatedParameterFinder<T extends Annotation> {

    private final Class<T> annotationClass;

    private final Collection<ParamValueProvider> namedValueProviders;

    AnnotatedParameterFinder(Class clazz, Class<T> annotationClass) {

      this.annotationClass = annotationClass;

      this.namedValueProviders = Stream
          .of(new AnnotatedFieldValueProviderFactory(), new AnnotatedConstructorValueProviderFactory(), new AnnotatedBeanParamValueProviderFactory())
          .flatMap(f -> f.getValueProviders(this, clazz))
          .collect(Collectors.toList());
    }

    Map<String, Object> getParameterMap(Object resourceImpl) {

      Map<String, Object> parameterMap = new HashMap<>();

      namedValueProviders.forEach(provider -> parameterMap.put(provider.getParamName(), provider.getParamValue(resourceImpl)));

      return parameterMap;
    }

    private String getParameterNameFromAnnotation(AnnotatedElement p) {
      T paramAnnotation = p.getAnnotation(annotationClass);
      if (paramAnnotation == null) {
        return null;
      }
      return getParameterNameFromAnnotation(paramAnnotation);
    }

    protected abstract AnnotatedParameterFinder<T> createFinderForBeanParamOfType(Class<?> type);

    protected abstract String getParameterNameFromAnnotation(T paramAnnotation);

  }

  private static class QueryParamFinder extends AnnotatedParameterFinder<QueryParam> {

    QueryParamFinder(Class clazz) {
      super(clazz, QueryParam.class);
    }

    @Override
    protected AnnotatedParameterFinder<QueryParam> createFinderForBeanParamOfType(Class<?> type) {
      return new QueryParamFinder(type);
    }

    @Override
    protected String getParameterNameFromAnnotation(QueryParam paramAnnotation) {
      return paramAnnotation.value();
    }

  }

  static class PathParamFinder extends AnnotatedParameterFinder<PathParam> {

    PathParamFinder(Class clazz) {
      super(clazz, PathParam.class);
    }

    @Override
    protected AnnotatedParameterFinder<PathParam> createFinderForBeanParamOfType(Class<?> type) {
      return new PathParamFinder(type);
    }

    @Override
    protected String getParameterNameFromAnnotation(PathParam paramAnnotation) {
      return paramAnnotation.value();
    }
  }

  private interface ParamValueProvider {

    String getParamName();

    Object getParamValue(Object resourceInstance);
  }

  private interface ParamValueProviderFactory {

    Stream<ParamValueProvider> getValueProviders(AnnotatedParameterFinder<?> finder, Class clazz);
  }

  static class AnnotatedFieldValueProviderFactory implements ParamValueProviderFactory {

    @Override
    public Stream<ParamValueProvider> getValueProviders(AnnotatedParameterFinder<?> finder, Class clazz) {

      return findFieldsDefinedInClass(clazz).stream()
          .map(field -> Pair.of(finder.getParameterNameFromAnnotation(field), field))
          .filter(pair -> pair.getKey() != null)
          .map(pair -> new ParamValueProvider() {

            @Override
            public String getParamName() {
              return pair.getKey();
            }

            @Override
            public Object getParamValue(Object resourceInstance) {
              return getFieldValue(pair.getValue(), resourceInstance);
            }

          });
    }
  }

  static class AnnotatedConstructorValueProviderFactory implements ParamValueProviderFactory {

    @Override
    public Stream<ParamValueProvider> getValueProviders(AnnotatedParameterFinder<?> finder, Class clazz) {

      return Stream.of(clazz.getConstructors())
          .filter(constructor -> constructor.getParameterCount() > 0)
          .flatMap(constructor -> Stream.of(constructor.getParameters()))
          .map(constructorParam -> finder.getParameterNameFromAnnotation(constructorParam))
          .filter(Objects::nonNull)
          .distinct()
          .map(name -> new ParamValueProvider() {

            private Field field = getField(clazz, name);

            @Override
            public String getParamName() {
              return name;
            }

            @Override
            public Object getParamValue(Object resourceInstance) {
              return getFieldValue(field, resourceInstance);
            }

          });
    }
  }

  static class AnnotatedBeanParamValueProviderFactory implements ParamValueProviderFactory {

    @Override
    public Stream<ParamValueProvider> getValueProviders(AnnotatedParameterFinder<?> finder, Class clazz) {

      return findFieldsThatContainOtherParamsIn(clazz)
          .flatMap(beanField -> {

            Collection<ParamValueProvider> beanProviders = finder.createFinderForBeanParamOfType(beanField.getType()).namedValueProviders;

            return beanProviders.stream()
                .map(provider -> new ParamValueProvider() {

                  @Override
                  public String getParamName() {
                    return provider.getParamName();
                  }

                  @Override
                  public Object getParamValue(Object resourceInstance) {
                    Object beanValue = getFieldValue(beanField, resourceInstance);

                    return getFieldValue(provider.getParamName(), beanValue);
                  }

                });
          });
    }
  }

  private static Stream<Field> findFieldsThatContainOtherParamsIn(Class clazz) {

    return findFieldsDefinedInClass(clazz).stream()
        .filter(field -> {
          return FieldUtils.getAllFieldsList(field.getType()).stream()
              .anyMatch(nestedField -> nestedField.getAnnotation(QueryParam.class) != null || nestedField.getAnnotation(PathParam.class) != null);
        });
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

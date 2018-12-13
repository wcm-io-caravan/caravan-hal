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
package io.wcm.caravan.hal.microservices.impl.reflection;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.reflect.FieldUtils;

import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.api.annotations.RelatedResource;
import io.wcm.caravan.hal.api.annotations.ResourceState;
import io.wcm.caravan.hal.api.annotations.TemplateVariables;

/**
 * Utility methods to inspect method signatures
 */
public final class HalApiReflectionUtils {

  private HalApiReflectionUtils() {
    // static methods only
  }

  static Comparator<Method> methodRelationComparator = (method1, method2) -> {
    String curi1 = method1.getAnnotation(RelatedResource.class).relation();
    String curi2 = method2.getAnnotation(RelatedResource.class).relation();

    // make sure that all links with custom link relations are displayed first
    if (curi1.contains(":") && !curi2.contains(":")) {
      return -1;
    }
    // make sure that all links with standard relations are displayed last
    if (curi2.contains(":") && !curi1.contains(":")) {
      return 1;
    }
    // otherwise the links should be sorted alphabetically
    return curi1.compareTo(curi2);
  };

  static Set<Class<?>> collectInterfaces(Class clazz) {

    Set<Class<?>> interfaces = new HashSet<>();

    Consumer<Class<?>> addInterfaces = new Consumer<Class<?>>() {

      @Override
      public void accept(Class<?> c) {
        for (Class interfaze : c.getInterfaces()) {
          interfaces.add(interfaze);
          accept(interfaze);
        }

      }
    };

    Class c = clazz;
    do {
      addInterfaces.accept(c);
      c = c.getSuperclass();
    }
    while (c != null);

    return interfaces;
  }

  /**
   * Checks which of the interfaces implemented by the given implementation instance is the one which is annotated with
   * {@link HalApiInterface}
   * @param resourceImplInstance an instance of a class implementing a HAL API interface
   * @return the interface that is annotated with {@link HalApiInterface}
   */
  public static Class<?> findHalApiInterface(Object resourceImplInstance) {

    return collectInterfaces(resourceImplInstance.getClass()).stream()
        .filter(interfaze -> interfaze.getAnnotation(HalApiInterface.class) != null)
        .findFirst()
        .orElseThrow(
            () -> new RuntimeException("None of the interfaces implemented by the given class " + resourceImplInstance.getClass().getName() + " has a @"
                + HalApiInterface.class.getSimpleName() + " annotation"));
  }

  public static boolean isHalApiInterface(Class<?> relatedResourceType) {

    if (!relatedResourceType.isInterface()) {
      return false;
    }
    if (relatedResourceType.getAnnotation(HalApiInterface.class) != null) {
      return true;
    }

    return Stream.of(relatedResourceType.getInterfaces())
        .anyMatch(i -> i.getAnnotation(HalApiInterface.class) != null);
  }

  public static Optional<Method> findResourceStateMethod(Class<?> apiInterface) {
    return Stream.of(apiInterface.getMethods())
        .filter(method -> method.getAnnotation(ResourceState.class) != null)
        .findFirst();
  }

  public static List<Method> getSortedRelatedResourceMethods(Class<?> apiInterface) {

    return Stream.of(apiInterface.getMethods())
        .filter(method -> method.getAnnotation(RelatedResource.class) != null)
        .sorted((m1, m2) -> HalApiReflectionUtils.methodRelationComparator.compare(m1, m2))
        .collect(Collectors.toList());
  }

  public static Map<String, Object> getTemplateVariablesFrom(Object dto, Class dtoClass) {

    if (dtoClass.isInterface()) {
      return getPublicGetterValuesAsMap(dto, dtoClass);
    }

    return getFieldValuesAsMap(dto, dtoClass);
  }

  private static Map<String, Object> getPublicGetterValuesAsMap(Object instance, Class dtoClass) {
    try {
      Map<String, Object> map = new LinkedHashMap<>();
      for (PropertyDescriptor property : Introspector.getBeanInfo(dtoClass).getPropertyDescriptors()) {
        Object value = instance != null ? property.getReadMethod().invoke(instance, new Object[0]) : null;
        map.put(property.getName(), value);
      }
      return map;
    }
    catch (IntrospectionException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
      throw new RuntimeException("Failed to extract template variables from class " + dtoClass.getName() + " through reflection", ex);
    }
  }

  private static Map<String, Object> getFieldValuesAsMap(Object instance, Class dtoClass) {

    Map<String, Object> map = new LinkedHashMap<>();

    for (Field field : FieldUtils.getAllFields(dtoClass)) {
      Object value = instance != null ? getFieldValue(field, instance) : null;
      map.put(field.getName(), value);
    }

    return map;
  }

  private static Object getFieldValue(Field field, Object instance) {
    try {
      return FieldUtils.readField(field, instance, true);
    }
    catch (IllegalArgumentException | IllegalAccessException ex) {
      throw new IllegalArgumentException("Failed to read value of field " + field.getName() + " from class " + instance.getClass().getSimpleName()
          + ". Make sure that all fields in your classes used as parameters annotated with @" + TemplateVariables.class.getSimpleName() + " are public");
    }
  }

  public static String getClassAndMethodName(Object instance, Method method) {
    return instance.getClass().getSimpleName() + "#" + method.getName();
  }

}

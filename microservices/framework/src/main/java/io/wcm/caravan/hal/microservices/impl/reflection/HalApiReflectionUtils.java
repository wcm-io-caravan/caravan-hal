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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.reflect.FieldUtils;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import io.reactivex.Observable;
import io.reactivex.Single;
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

  public static Single<?> getResourceStateObservable(Class<?> apiInterface, Object instance) {

    // find the first method annotated with @ResourceState
    Observable<Object> rxResourceState = Observable.fromArray(apiInterface.getMethods())
        .filter(method -> method.getAnnotation(ResourceState.class) != null)
        .take(1)
        // invoke the method to get the state object and re-throw any exceptions that might be thrown
        .flatMap(method -> RxJavaReflectionUtils.invokeMethodAndReturnObservable(instance, method));

    // use an empty JSON object if no method is annotated with @ResourceState (or if the instance returned null)
    return rxResourceState
        .single(JsonNodeFactory.instance.objectNode());
  }

  public static Observable<Method> getSortedRelatedResourceMethods(Class<?> apiInterface) {

    return Observable.fromArray(apiInterface.getMethods())
        .filter(method -> method.getAnnotation(RelatedResource.class) != null)
        .sorted((m1, m2) -> HalApiReflectionUtils.methodRelationComparator.compare(m1, m2));
  }

  public static Map<String, Object> getTemplateVariablesFrom(Object dto, Class dtoClass) {

    return getFieldValuesAsMap(dto, dtoClass);
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
          + ". Make sure that all fields in your classes annotated with @" + TemplateVariables.class.getSimpleName() + " are public");
    }
  }
}

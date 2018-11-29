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
package io.wcm.caravan.hal.microservices.impl.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

import org.reactivestreams.Publisher;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.api.server.AsyncHalResourceRenderer;
import io.wcm.caravan.hal.microservices.impl.metadata.EmissionStopwatch;

public class RxJavaReflectionUtils {

  public static Observable<?> invokeMethodAndReturnObservable(Object resourceImplInstance, Method method, RequestMetricsCollector metrics) {

    Stopwatch stopwatch = Stopwatch.createStarted();

    String fullMethodName = HalApiReflectionUtils.getClassAndMethodName(resourceImplInstance, method);


    Object[] args = new Object[method.getParameterCount()];

    try {
      Object returnValue = method.invoke(resourceImplInstance, args);

      Observable<?> rxReturnValue;
      if (returnValue == null) {
        rxReturnValue = Observable.empty();
      }
      else if (hasReactiveReturnType(method)) {
        rxReturnValue = convertToObservable(returnValue)
            .compose(EmissionStopwatch.collectMetrics("processing " + getObservableEmissionType(method).getSimpleName() + " emissions from " + fullMethodName,
                metrics));
      }
      else {
        rxReturnValue = Observable.just(returnValue);
      }

      return rxReturnValue;
    }
    catch (InvocationTargetException ex) {
      throw new RuntimeException("Failed to invoke method " + fullMethodName, ex.getTargetException());
    }
    catch (IllegalAccessException | IllegalArgumentException ex) {
      throw new RuntimeException("Failed to invoke method " + fullMethodName, ex);
    }
    finally {

      metrics.onMethodInvocationFinished(AsyncHalResourceRenderer.class,
          "calling " + fullMethodName,
          stopwatch.elapsed(TimeUnit.MICROSECONDS));
    }
  }

  /**
   * @param method a method that returns a Observable
   * @return the type of the emitted results
   */
  public static Class<?> getObservableEmissionType(Method method) {
    Type returnType = method.getGenericReturnType();

    Preconditions.checkArgument(returnType instanceof ParameterizedType,
        "return types must be Observable/Single/Maybe<Class>, but " + method + " has a return type " + returnType.getTypeName());

    ParameterizedType observableType = (ParameterizedType)returnType;

    Type resourceType = observableType.getActualTypeArguments()[0];

    Preconditions.checkArgument(resourceType instanceof Class,
        "return types must be Observable/Single/Maybe of Class type, but found " + resourceType.getTypeName());

    return (Class)resourceType;
  }

  /**
   * @param method the method to check
   * @return true if this method returns a {@link Observable}
   */
  public static boolean hasReactiveReturnType(Method method) {

    Class returnType = method.getReturnType();

    return Observable.class.isAssignableFrom(returnType) || Single.class.isAssignableFrom(returnType) || Maybe.class.isAssignableFrom(returnType);
  }

  public static Object convertReactiveType(Object reactiveInstance, Class<?> targetType) {

    Observable<?> observable = convertToObservable(reactiveInstance);

    return convertObservableTo(observable, targetType);
  }

  private static Object convertObservableTo(Observable<?> observable, Class<?> targetType) {

    Preconditions.checkNotNull(targetType, "A target type must be provided");

    if (targetType.isAssignableFrom(Observable.class)) {
      return observable;
    }
    if (targetType.isAssignableFrom(Single.class)) {
      return observable.singleOrError();
    }
    if (targetType.isAssignableFrom(Maybe.class)) {
      return observable.singleElement();
    }
    if (targetType.isAssignableFrom(Publisher.class)) {
      return observable.toFlowable(BackpressureStrategy.BUFFER);
    }

    throw new UnsupportedOperationException("The given target type of " + targetType.getName() + " is not a supported reactive type");
  }

  private static Observable<?> convertToObservable(Object reactiveInstance) {

    Preconditions.checkNotNull(reactiveInstance, "Cannot convert null objects");

    Observable<?> observable = null;
    if (reactiveInstance instanceof Observable) {
      observable = (Observable)reactiveInstance;
    }
    else if (reactiveInstance instanceof Single) {
      observable = ((Single)reactiveInstance).toObservable();
    }
    else if (reactiveInstance instanceof Maybe) {
      observable = ((Maybe)reactiveInstance).toObservable();
    }
    else if (reactiveInstance instanceof Publisher) {
      observable = Observable.fromPublisher((Publisher<?>)reactiveInstance);
    }
    else {
      throw new UnsupportedOperationException("The given instance of " + reactiveInstance.getClass().getName() + " is not a supported reactive type");
    }

    return observable;

  }
}

/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.hal.microservices.util;

import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.Single;


/**
 * Contains static utility methods to simplify working with RX Java streams
 */
public final class RxJavaTransformers {

  private RxJavaTransformers() {
    // contains only static methods
  }

  /**
   * @param asyncFilterFunc a function that returns a boolean {@link Single} to decide whether a given item should
   *          be filtered
   * @return a {@link ObservableTransformer} that can be passed to {@link Observable#compose(ObservableTransformer)}
   */
  public static <T> ObservableTransformer<T, T> filterWith(Function<T, Single<Boolean>> asyncFilterFunc) {
    return new AsyncFilterTransformer<T>(asyncFilterFunc);
  }

  private static final class AsyncFilterTransformer<T> implements ObservableTransformer<T, T> {

    private final Function<T, Single<Boolean>> asyncFilterFunc;

    private AsyncFilterTransformer(Function<T, Single<Boolean>> asyncFilterFunc) {
      this.asyncFilterFunc = asyncFilterFunc;
    }

    @Override
    public ObservableSource<T> apply(Observable<T> upstream) {

      return upstream
          .flatMapSingle(this::createPairOfItemAndFilterFlag)
          .filter(pair -> pair.getRight())
          .map(pair -> pair.getLeft());
    }

    private Single<Pair<T, Boolean>> createPairOfItemAndFilterFlag(T item) {

      Single<Boolean> filterFlags = asyncFilterFunc.apply(item);

      return filterFlags.map(flag -> Pair.of(item, flag));
    }
  }

}

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
package io.wcm.caravan.hal.comparison.impl.matching;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

/**
 * An algorithm that finds matching items by comparing an id.
 */
public class SimpleIdMatchingAlgorithm<T> implements MatchingAlgorithm<T> {

  private final Function<T, String> idProvider;

  /**
   * @param idProvider the function to generate/extract an id for a given item
   */
  public SimpleIdMatchingAlgorithm(Function<T, String> idProvider) {
    this.idProvider = idProvider;
  }

  @Override
  public MatchingResult<T> findMatchingItems(List<T> expectedList, List<T> actualList) {

    MatchingResultImpl<T> result = new MatchingResultImpl<T>();

    IndexedItemList<T> remainingExpected = new IndexedItemList<T>(expectedList, idProvider);
    IndexedItemList<T> remainingActual = new IndexedItemList<T>(actualList, idProvider);

    while (remainingExpected.hasMoreItems()) {
      ItemWithIdAndIndex<T> nextExpected = remainingExpected.getAndRemoveNextItem();
      String nextId = nextExpected.getId();

      if (remainingActual.hasItemWithId(nextId)) {
        ItemWithIdAndIndex<T> matchingActual = remainingActual.getAndRemoveItemWithId(nextId);
        result.addMatched(nextExpected, matchingActual);
      }
      else {
        result.addRemoved(nextExpected);
      }
    }

    while (remainingActual.hasMoreItems()) {
      result.addAdded(remainingActual.getAndRemoveNextItem());
    }

    return result;
  }

  private static class MatchingResultImpl<T> implements MatchingResult<T> {

    private final List<ItemWithIdAndIndex<T>> matchedExpected = new ArrayList<>();
    private final List<ItemWithIdAndIndex<T>> matchedActual = new ArrayList<>();

    private final List<ItemWithIdAndIndex<T>> removedExpected = new ArrayList<>();
    private final List<ItemWithIdAndIndex<T>> addedActual = new ArrayList<>();

    void addMatched(ItemWithIdAndIndex<T> expected, ItemWithIdAndIndex<T> actual) {
      matchedExpected.add(expected);
      matchedActual.add(actual);
    }

    void addRemoved(ItemWithIdAndIndex<T> removed) {
      this.removedExpected.add(removed);
    }

    void addAdded(ItemWithIdAndIndex<T> added) {
      this.addedActual.add(added);
    }

    @Override
    public boolean areMatchesReordered() {
      int lastIndex = -1;
      for (ItemWithIdAndIndex actual : matchedActual) {
        int nextIndex = actual.getIndex();
        if (nextIndex < lastIndex) {
          return true;
        }
        lastIndex = nextIndex;
      }
      return false;
    }

    private List<T> extractItems(List<ItemWithIdAndIndex<T>> list) {
      return list.stream()
          .map(ItemWithIdAndIndex<T>::getItem)
          .collect(Collectors.toList());
    }

    @Override
    public List<T> getMatchedExpected() {
      return extractItems(matchedExpected);
    }

    @Override
    public List<T> getMatchedActual() {
      return extractItems(matchedActual);
    }

    @Override
    public List<T> getRemovedExpected() {
      return extractItems(removedExpected);
    }

    @Override
    public List<T> getAddedActual() {
      return extractItems(addedActual);
    }
  }

  private static class ItemWithIdAndIndex<T> {

    private final T item;
    private final int index;
    private final String id;

    ItemWithIdAndIndex(T item, int index, String id) {
      this.item = item;
      this.index = index;
      this.id = id;
    }

    public T getItem() {
      return this.item;
    }

    public int getIndex() {
      return this.index;
    }

    public String getId() {
      return this.id;
    }

    static <T> List<ItemWithIdAndIndex<T>> createListFrom(Iterable<T> items, Function<T, String> idProvider) {
      List<ItemWithIdAndIndex<T>> itemList = new ArrayList<>();

      int index = 0;
      for (T item : items) {
        itemList.add(new ItemWithIdAndIndex<T>(item, index++, idProvider.apply(item)));
      }

      return itemList;
    }

    @Override
    public String toString() {
      return "{index=" + index + ", id=" + id + "}";
    }
  }

  private static class IndexedItemList<T> {

    private final List<ItemWithIdAndIndex<T>> itemList;
    private final Multimap<String, ItemWithIdAndIndex<T>> idItemMap = LinkedListMultimap.create();

    IndexedItemList(Iterable<T> resources, Function<T, String> idProvider) {
      this.itemList = ItemWithIdAndIndex.createListFrom(resources, idProvider);

      for (ItemWithIdAndIndex<T> item : itemList) {
        idItemMap.put(item.getId(), item);
      }
    }

    boolean hasMoreItems() {
      return itemList.size() > 0;
    }

    ItemWithIdAndIndex<T> getAndRemoveNextItem() {
      ItemWithIdAndIndex<T> item = itemList.remove(0);
      idItemMap.remove(item.getId(), item);
      return item;
    }

    boolean hasItemWithId(String id) {
      return idItemMap.containsKey(id);
    }

    ItemWithIdAndIndex<T> getAndRemoveItemWithId(String id) {
      ItemWithIdAndIndex<T> item = idItemMap.get(id).iterator().next();
      itemList.remove(item);
      idItemMap.remove(id, item);
      return item;
    }

    @Override
    public String toString() {
      return itemList.toString();
    }
  }
}

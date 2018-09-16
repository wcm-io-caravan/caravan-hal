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
package io.wcm.caravan.hal.comparison.impl.embedded.steps;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

import io.wcm.caravan.hal.resource.HalResource;

public class IdentityMatchingAlgorithm {


  public MatchingResult match(List<HalResource> expectedList, List<HalResource> actualList, Function<HalResource, String> idProvider) {

    MatchingResult result = new MatchingResult();

    FastLookupItemList remainingExpected = new FastLookupItemList(expectedList, idProvider);
    FastLookupItemList remainingActual = new FastLookupItemList(actualList, idProvider);

    while (remainingExpected.hasMoreItems()) {
      ItemWithIndex nextExpected = remainingExpected.getAndRemoveNextItem();
      String nextId = nextExpected.getId();
      if (remainingActual.hasItemWithId(nextId)) {
        ItemWithIndex matchingActual = remainingActual.getAndRemoveItemWithId(nextId);
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

  static class MatchingResult {

    private final List<ItemWithIndex> matchedExpected = new ArrayList<>();
    private final List<ItemWithIndex> matchedActual = new ArrayList<>();

    private final List<ItemWithIndex> removedExpected = new ArrayList<>();
    private final List<ItemWithIndex> addedActual = new ArrayList<>();

    boolean areMatchesReordered() {
      int lastIndex = -1;
      for (ItemWithIndex actual : matchedActual) {
        int nextIndex = actual.getIndex();
        if (nextIndex < lastIndex) {
          return true;
        }
        lastIndex = nextIndex;
      }
      return false;
    }

    void addMatched(ItemWithIndex expected, ItemWithIndex actual) {
      matchedExpected.add(expected);
      matchedActual.add(actual);
    }

    void addRemoved(ItemWithIndex removed) {
      this.removedExpected.add(removed);
    }

    void addAdded(ItemWithIndex added) {
      this.addedActual.add(added);
    }

    public List<ItemWithIndex> getMatchedExpected() {
      return this.matchedExpected;
    }

    public List<ItemWithIndex> getMatchedActual() {
      return this.matchedActual;
    }

    public List<ItemWithIndex> getRemovedExpected() {
      return this.removedExpected;
    }

    public List<ItemWithIndex> getAddedActual() {
      return this.addedActual;
    }
  }

  static class ItemWithIndex {

    private final HalResource item;
    private final int index;
    private final String id;

    public ItemWithIndex(HalResource item, int index, String id) {
      this.item = item;
      this.index = index;
      this.id = id;
    }

    public HalResource getItem() {
      return this.item;
    }

    public int getIndex() {
      return this.index;
    }

    public String getId() {
      return this.id;
    }

    static List<ItemWithIndex> createListFrom(Iterable<HalResource> resources, Function<HalResource, String> idProvider) {
      List<ItemWithIndex> itemList = new ArrayList<>();

      int index = 0;
      for (HalResource item : resources) {
        itemList.add(new ItemWithIndex(item, index++, idProvider.apply(item)));
      }

      return itemList;
    }

    @Override
    public String toString() {
      return "{index=" + index + ", id=" + id + "}";
    }
  }

  static class FastLookupItemList {

    private final List<ItemWithIndex> itemList;
    private final Multimap<String, ItemWithIndex> idItemMap = LinkedListMultimap.create();

    FastLookupItemList(Iterable<HalResource> resources, Function<HalResource, String> idProvider) {
      this.itemList = ItemWithIndex.createListFrom(resources, idProvider);

      for (ItemWithIndex item : itemList) {
        idItemMap.put(item.getId(), item);
      }
    }

    boolean hasMoreItems() {
      return itemList.size() > 0;
    }

    ItemWithIndex getAndRemoveNextItem() {
      ItemWithIndex item = itemList.remove(0);
      idItemMap.remove(item.getId(), item);
      return item;
    }

    boolean hasItemWithId(String id) {
      return idItemMap.containsKey(id);
    }

    ItemWithIndex getAndRemoveItemWithId(String id) {
      ItemWithIndex item = idItemMap.get(id).iterator().next();
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

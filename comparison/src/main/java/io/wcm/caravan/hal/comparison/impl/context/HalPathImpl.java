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
package io.wcm.caravan.hal.comparison.impl.context;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import io.wcm.caravan.hal.comparison.HalComparisonContext;

/**
 * Immutable implementation of {@link HalComparisonContext} interface
 */
public class HalPathImpl {

  private final List<Token> halTokens;
  private final List<Token> jsonTokens;

  /**
   * Constructor to use for the entry points.
   */
  public HalPathImpl() {
    this.halTokens = ImmutableList.of();
    this.jsonTokens = ImmutableList.of();
  }

  private HalPathImpl(List<Token> halTokens, List<Token> jsonPathTokens) {
    this.halTokens = ImmutableList.copyOf(halTokens);
    this.jsonTokens = ImmutableList.copyOf(jsonPathTokens);
  }

  /**
   * @param relation of the link/embedded resource
   * @param index optional index (to be used if there are multiple links/embedded resources with the same relation)
   * @param name optional name (used only for links)
   * @return a new instance with an updated location
   */
  public HalPathImpl append(String relation, Integer index, String name) {
    List<Token> newTokens = new ArrayList<>(halTokens);
    newTokens.add(new Token(relation, index, name));
    return new HalPathImpl(newTokens, jsonTokens);
  }

  /**
   * @param fieldName the name of a JSON property
   * @return a new instance with an updated JSON path suffix
   */
  public HalPathImpl appendJsonPath(String fieldName) {
    List<Token> newTokens = new ArrayList<>(jsonTokens);
    newTokens.add(new Token(fieldName, null, null));
    return new HalPathImpl(halTokens, newTokens);
  }

  /**
   * @param index of an element within an array
   * @return a new instance with an updated HAL path suffix
   */
  public HalPathImpl replaceHalPathIndex(int index) {
    List<Token> newTokens = new ArrayList<>(halTokens);
    Token lastToken = newTokens.remove(newTokens.size() - 1);
    newTokens.add(new Token(lastToken.getName(), index, lastToken.identifier));
    return new HalPathImpl(newTokens, jsonTokens);
  }

  /**
   * @param index of an element within an array
   * @return a new instance with an updated JSON path suffix
   */
  public HalPathImpl replaceJsonPathIndex(int index) {
    List<Token> newTokens = new ArrayList<>(jsonTokens);
    Token lastToken = newTokens.remove(newTokens.size() - 1);
    newTokens.add(new Token(lastToken.getName(), index, null));
    return new HalPathImpl(halTokens, newTokens);
  }

  private static String getLastOf(List<Token> tokens) {
    if (tokens.isEmpty()) {
      return "";
    }
    return Iterables.getLast(tokens).getName();
  }

  private static List<String> getAllOf(List<Token> tokens) {
    return tokens.stream()
        .map(Token::getName)
        .collect(Collectors.toList());
  }

  String getLastRelation() {
    return getLastOf(halTokens);
  }

  List<String> getAllRelations() {
    return getAllOf(halTokens);
  }

  String getLastProperyName() {
    return getLastOf(jsonTokens);
  }

  List<String> getAllPropertyNames() {
    return getAllOf(jsonTokens);
  }

  @Override
  public String toString() {

    String jsonPath = "";
    if (!jsonTokens.isEmpty()) {
      jsonPath = jsonTokens.stream()
          .map(Token::toString)
          .collect(Collectors.joining(".", "$.", ""));
    }

    return halTokens.stream()
        .map(Token::toString)
        .collect(Collectors.joining("/", "/", jsonPath));
  }

  private static class Token {

    private final String name;
    private final Integer index;
    private final String identifier;

    Token(String name, Integer index, String identifier) {
      this.name = name;
      this.index = index;
      this.identifier = identifier;
    }

    String getName() {
      return this.name;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder(name);
      if (identifier != null) {
        sb.append("['" + identifier + "']");
      }
      else if (index != null) {
        sb.append("[" + index + "]");
      }
      return sb.toString();
    }
  }
}

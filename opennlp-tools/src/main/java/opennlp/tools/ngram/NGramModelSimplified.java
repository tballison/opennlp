/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package opennlp.tools.ngram;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.dictionary.serializer.Attributes;
import opennlp.tools.dictionary.serializer.DictionaryEntryPersistor;
import opennlp.tools.dictionary.serializer.Entry;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.StringList;
import opennlp.tools.util.StringUtil;

/**
 * The {@link NGramModelSimplified} can be used to crate ngrams and character ngrams.
 *
 * @see StringList
 */
public class NGramModelSimplified implements Iterable<String> {

  protected static final String COUNT = "count";

  private Map<String, Integer> mNGrams = new HashMap<>();

  /**
   * Initializes an empty instance.
   */
  public NGramModelSimplified() {
  }



  /**
   * Retrieves the count of the given ngram.
   *
   * @param ngram an ngram
   * @return count of the ngram or 0 if it is not contained
   *
   */
  public int getCount(String ngram) {

    Integer count = mNGrams.get(ngram);

    if (count == null) {
      return 0;
    }

    return count;
  }

  /**
   * Sets the count of an existing ngram.
   *
   * @param ngram
   * @param count
   */
  public void setCount(String ngram, int count) {

    Integer oldCount = mNGrams.put(ngram, count);

    if (oldCount == null) {
      mNGrams.remove(ngram);
      throw new NoSuchElementException();
    }
  }

  /**
   * Adds one NGram, if it already exists the count increase by one.
   *
   * @param ngram
   */
  public void add(String ngram) {
    if (contains(ngram)) {
      setCount(ngram, getCount(ngram) + 1);
    } else {
      mNGrams.put(ngram, 1);
    }
  }


  /**
   * Adds character NGrams to the current instance.
   *
   * @param chars
   * @param minLength
   * @param maxLength
   */
  public void add(CharSequence chars, int minLength, int maxLength) {

    for (int lengthIndex = minLength; lengthIndex < maxLength + 1; lengthIndex++) {
      for (int textIndex = 0;
          textIndex + lengthIndex - 1 < chars.length(); textIndex++) {

        String gram = //StringUtil.toLowerCase(
            chars.subSequence(textIndex, textIndex + lengthIndex).toString();//);

        add(gram);
      }
    }
  }

  /**
   * Removes the specified tokens form the NGram model, they are just dropped.
   *
   * @param tokens
   */
  public void remove(StringList tokens) {
    mNGrams.remove(tokens);
  }

  /**
   * Checks fit he given tokens are contained by the current instance.
   *
   * @param tokens
   *
   * @return true if the ngram is contained
   */
  public boolean contains(String tokens) {
    return mNGrams.containsKey(tokens);
  }

  /**
   * Retrieves the number of {@link StringList} entries in the current instance.
   *
   * @return number of different grams
   */
  public int size() {
    return mNGrams.size();
  }

  /**
   * Retrieves an {@link Iterator} over all {@link StringList} entries.
   *
   * @return iterator over all grams
   */
  @Override
  public Iterator<String> iterator() {
    return mNGrams.keySet().iterator();
  }

  /**
   * Retrieves the total count of all Ngrams.
   *
   * @return total count of all ngrams
   */
  public int numberOfGrams() {
    int counter = 0;

    for (String ngram : this) {
      counter += getCount(ngram);
    }

    return counter;
  }

  /**
   * Deletes all ngram which do appear less than the cutoffUnder value
   * and more often than the cutoffOver value.
   *
   * @param cutoffUnder
   * @param cutoffOver
   */
  public void cutoff(int cutoffUnder, int cutoffOver) {

    if (cutoffUnder > 0 || cutoffOver < Integer.MAX_VALUE) {

      for (Iterator<String> it = iterator(); it.hasNext(); ) {

        String ngram = it.next();

        int count = getCount(ngram);

        if (count < cutoffUnder ||
            count > cutoffOver) {
          it.remove();
        }
      }
    }
  }


  @Override
  public boolean equals(Object obj) {
    boolean result;

    if (obj == this) {
      result = true;
    }
    else if (obj instanceof NGramModelSimplified) {
      NGramModelSimplified model  = (NGramModelSimplified) obj;

      result = mNGrams.equals(model.mNGrams);
    }
    else {
      result = false;
    }

    return result;
  }

  @Override
  public String toString() {
    return "Size: " + size();
  }

  @Override
  public int hashCode() {
    return mNGrams.hashCode();
  }
}

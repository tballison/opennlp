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

package opennlp.tools.langdetect;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opennlp.tools.util.StringUtil;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.standard.UAX29URLEmailTokenizerFactory;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.ClasspathResourceLoader;

/**
 * A context generator for language detector.
 */
public class LuceneNGramIterator implements IterableLanguageDetectorContextGenerator, Iterator<String> {

  private static final String FIELD = "f";

  private final Analyzer analyzer;
  private String next = null;
  private String text = null;
  private TokenStream tokenStream = null;
  private CharTermAttribute charTermAttribute = null;
  /**
   * Creates a customizable @{@link LuceneNGramIterator} that computes ngrams from text
   * @param minLength min ngrams chars
   * @param maxLength max ngrams chars
   */
  public LuceneNGramIterator(int minLength, int maxLength) {
    Map<String, String> ngramParams = new HashMap<>();
    ngramParams.put("minGramSize", Integer.toString(minLength));
    ngramParams.put("maxGramSize", Integer.toString(maxLength));
    try {
      analyzer = CustomAnalyzer.builder(new ClasspathResourceLoader(LuceneNGramIterator.class))
              .withTokenizer(UAX29URLEmailTokenizerFactory.class)
              .addTokenFilter(MyUAXTypeFilterFactory.class)
              .addTokenFilter(LowerCaseFilterFactory.class)
              .addTokenFilter(MyNGramTokenFilterFactory.class, ngramParams).build();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public String[] getContext(CharSequence sequence) {
    try {
      reset(sequence.toString());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    Set<String> tokens = new HashSet<>();
    while (hasNext()) {
      tokens.add(next());
    }
    return tokens.toArray(new String[tokens.size()]);
  }

  @Override
  public boolean hasNext() {
    return next != null;
  }

  @Override
  public String next() {
    if (tokenStream == null) {
      throw new IllegalStateException("Must call reset before iterating");
    }
    String ret = next;
    next = null;
    _next();
    return ret;
  }

  public void reset(String txt) throws IOException {
    if (tokenStream != null) {
      tokenStream.end();
      tokenStream.close();
    }
    this.text = txt;
    tokenStream = analyzer.tokenStream(FIELD, text);
    tokenStream.reset();
    charTermAttribute = tokenStream.getAttribute(CharTermAttribute.class);
    _next();
  }

  private void _next() {
    try {
      while (tokenStream.incrementToken()) {
        String t = charTermAttribute.toString();
        if (StringUtil.isEmpty(t)) {
          continue;
        }
        next = t;
        break;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

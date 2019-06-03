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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import opennlp.tools.ngram.NGramCharacterModel;
import opennlp.tools.util.StringUtil;
import opennlp.tools.util.normalizer.AggregateCharSequenceNormalizer;
import opennlp.tools.util.normalizer.CharSequenceNormalizer;
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
public class NGramCharContextGenerator implements LanguageDetectorContextGenerator {

  private static final String FIELD = "f";

  protected final int minLength;
  protected final int maxLength;
  protected final CharSequenceNormalizer normalizer;

  /**
   * Creates a customizable @{@link NGramCharContextGenerator} that computes ngrams from text
   * @param minLength min ngrams chars
   * @param maxLength max ngrams chars
   * @param normalizers zero or more normalizers to
   *                    be applied in to the text before extracting ngrams
   */
  public NGramCharContextGenerator(int minLength, int maxLength,
                                   CharSequenceNormalizer... normalizers) {
    this.minLength = minLength;
    this.maxLength = maxLength;

    this.normalizer = new AggregateCharSequenceNormalizer(normalizers);
  }

  /**
   * Generates the context for a document using character ngrams.
   * @param document document to extract context from
   * @return the generated context
   */
  @Override
  public String[] getContext(CharSequence document) {
    Collection<String> context = new ArrayList<>();

    NGramCharacterModel model = new NGramCharacterModel();
    model.add(normalizer.normalize(document), minLength, maxLength);

    for (String token : model) {
      context.add(token);
    }
    return context.toArray(new String[context.size()]);
  }
}

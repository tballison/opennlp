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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import opennlp.tools.formats.ResourceAsStreamFactory;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.TrainingParameters;


public class LanguageDetectorMETest {

  private LanguageDetectorModel model;

  @Before
  public void init() throws Exception {

    this.model = trainModel();

  }

  @Test
  public void testPredictLanguages() {
    LanguageDetector ld = new LanguageDetectorME(this.model);
    Language[] languages = ld.predictLanguages("estava em uma marcenaria na Rua Bruno");

    Assert.assertEquals(4, languages.length);
    Assert.assertEquals("pob", languages[0].getLang());
    Assert.assertEquals("ita", languages[1].getLang());
    Assert.assertEquals("spa", languages[2].getLang());
    Assert.assertEquals("fra", languages[3].getLang());
  }

  @Test
  public void testPredictLanguage() {
    LanguageDetector ld = new LanguageDetectorME(this.model);
    Language language = ld.predictLanguage("Dove è meglio che giochi");

    Assert.assertEquals("ita", language.getLang());
  }

  @Test
  public void testSupportedLanguages() {

    LanguageDetector ld = new LanguageDetectorME(this.model);
    String[] supportedLanguages = ld.getSupportedLanguages();

    Assert.assertEquals(4, supportedLanguages.length);
  }

  @Test
  public void testLoadFromSerialized() throws IOException {
    byte[] serialized = serializeModel(model);

    LanguageDetectorModel myModel = new LanguageDetectorModel(new ByteArrayInputStream(serialized));

    Assert.assertNotNull(myModel);

  }

  protected static byte[] serializeModel(LanguageDetectorModel model) throws IOException {

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    model.serialize(out);
    return out.toByteArray();
  }

  public static LanguageDetectorModel trainModel() throws Exception {
    return trainModel(new LanguageDetectorFactory());
  }

  public static LanguageDetectorModel trainModel(LanguageDetectorFactory factory) throws Exception {


    LanguageDetectorSampleStream sampleStream = createSampleStream();

    TrainingParameters params = new TrainingParameters();
    params.put(TrainingParameters.ITERATIONS_PARAM, 100);
    params.put(TrainingParameters.CUTOFF_PARAM, 5);
    params.put("DataIndexer", "TwoPass");
    params.put(TrainingParameters.ALGORITHM_PARAM, "NAIVEBAYES");

    return LanguageDetectorME.train(sampleStream, params, factory);
  }

  public static LanguageDetectorSampleStream createSampleStream() throws IOException {

    ResourceAsStreamFactory streamFactory = new ResourceAsStreamFactory(
        LanguageDetectorMETest.class, "/opennlp/tools/doccat/DoccatSample.txt");

    PlainTextByLineStream lineStream = new PlainTextByLineStream(streamFactory, "UTF-8");

    return new LanguageDetectorSampleStream(lineStream);
  }

  @Test
  public void testActual() throws Exception {
    LanguageDetectorModel model = new LanguageDetectorModel(
            new File("C:/data/langid/lang_models/langdetect-20190607.bin"));

    int[] lengths = new int[]{
            10,20,30,40,50,100,150,200,
            500,1000,
            5000,10000,20000,50000,70000, 80000,
            100000};//, 120000};
//    ProbingLanguageDetectorME ld = new ProbingLanguageDetectorME(model);
    LanguageDetectorME ld = new LanguageDetectorME(model);
    Map<String, List<String>> sents = load(Paths.get("C:/data/langid/leipzig_short/leipzig_1000-sents.txt"));
    for (int len : lengths) {
      long totalElapsed = 0;
      DoubleSummaryStatistics summaryStatistics = new DoubleSummaryStatistics();
      for (String lang : sents.keySet()) {
        if (! lang.equals("fra")) {
          //continue;
        }
        int correct = 0;
        int total = 0;
        for (int i = 0; i < 20; i++) {
          String txt = truncate(sents.get(lang), len);
          long start = System.currentTimeMillis();
          Language[] langs = ld.predictLanguages(txt);
          long elapsed = System.currentTimeMillis()-start;
          String predicted = langs[0].getLang();
          totalElapsed += elapsed;
          total++;
          if (predicted.equals(lang)) {
            correct++;
          } else {
            double diff = langs[0].getConfidence()-langs[1].getConfidence();
            boolean is2AHit = false;
            if (lang.equals(langs[1].getLang())) {
              is2AHit = true;
            }
            //System.out.println(
              //      String.format("%s %s %.3f", lang, is2AHit, diff));
          }
        }
        double percentCorrect = (double)correct/(double)total;
        summaryStatistics.accept(percentCorrect);
        // System.err.println(String.format("%s %s %.2f", lang, len, percentCorrect));
      }
      System.out.println(
              String.format("%s %s %.2f",
                      len, totalElapsed,summaryStatistics.getAverage()));//+ " : "+ld.totalElapsed);
//      ld.totalElapsed = 0;
    }

  }

  private String truncate(List<String> strings, int len) {
//    Collections.shuffle(strings);
    StringBuilder sb = new StringBuilder();
    for (String s : strings) {
      sb.append(" ").append(s);
      if (sb.length() > len) {
        break;
      }
    }
    sb.setLength(len);
    return sb.toString();
  }

  private Map<String, List<String>> load(Path path) throws IOException {
    Map<String, List<String>> m = new HashMap<>();
    try (BufferedReader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      String line = r.readLine();

      while (line != null) {
        String[] data = line.split("\t");
        String lang = data[0];
        String sent = data[1];
        List<String> sents = m.get(lang);
        if (sents == null) {
          sents = new ArrayList<>();
          m.put(lang, sents);
        }
        sents.add(sent);

        line = r.readLine();
      }
    }
    return m;
  }
}

package opennlp.tools.langdetect;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import opennlp.tools.ml.maxent.GISModel;
import opennlp.tools.util.normalizer.CharSequenceNormalizer;
import opennlp.tools.util.normalizer.EmojiCharSequenceNormalizer;
import opennlp.tools.util.normalizer.NumberCharSequenceNormalizer;
import opennlp.tools.util.normalizer.ShrinkCharSequenceNormalizer;
import opennlp.tools.util.normalizer.TwitterCharSequenceNormalizer;
import opennlp.tools.util.normalizer.UrlCharSequenceNormalizer;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class LanguageDetectorSpeedTest {

    static LanguageDetectorModel LANG_MODEL;

    @BeforeClass
    public static void init() throws IOException {
        LANG_MODEL = new LanguageDetectorModel(new File("C:/data/langid/lang_models/langdetect-183.bin"));
    }

    @Test
    public void testContextGenerators() throws Exception {
        Path sampleFile = Paths.get("C:\\data\\langid\\leipzig_short\\leipzig_1000-sents.txt");
        Map<String, List<String>> langs = loadSample(sampleFile);


        CharSequenceNormalizer[] defaultNormalizers = new CharSequenceNormalizer[]{EmojiCharSequenceNormalizer.getInstance(),
                UrlCharSequenceNormalizer.getInstance(),
                TwitterCharSequenceNormalizer.getInstance(),
                NumberCharSequenceNormalizer.getInstance(),
                ShrinkCharSequenceNormalizer.getInstance()
        };

        LanguageDetectorModel model = new LanguageDetectorModel(new File("C:/data/langid/lang_models/langdetect-183.bin"));
        Set<String> set = ((GISModel)model.getMaxentModel()).getFeatures();

        for (LanguageDetectorContextGenerator gen : new LanguageDetectorContextGenerator[]{
                new DefaultLanguageDetectorContextGenerator(1, 3),
                new NGramCharContextGenerator(1, 3, defaultNormalizers),
                new SlightlyFasterNGramCharContextGenerator(1, 3, defaultNormalizers).setTargetTokens(set),//this harms performance
                new SlightlyFasterNGramCharContextGenerator(1, 3, defaultNormalizers),
                new LuceneNGramIterator(1, 3)
        }) {
            LanguageDetector languageDetector = new LanguageDetectorME(model, gen);
            long elapsed = 0;
            for (Map.Entry<String, List<String>> e : langs.entrySet()) {
                //Collections.shuffle(e.getValue());
                StringBuilder sb = new StringBuilder();
                for (int i = 0 ; i < e.getValue().size() && i < 10; i++) {
                    sb.append(e.getValue().get(i)).append(" ");
                }
                elapsed += runLoops(sb.toString(), languageDetector);
            }
            System.out.println(gen.getClass().getSimpleName() + ": " + elapsed);
        }
    }

    private long runLoops(String txt, LanguageDetector ld) {
        long sum = 0;
        for (int j = 0; j < 4; j++) {
            long start = System.currentTimeMillis();
            Map<String, Integer> map = new HashMap<>();
            for (int i = 0; i < 100; i++) {
                Language language = ld.predictLanguage(txt);
                //Assert.assertEquals("por", language.getLang());
               // System.out.println(language.getLang());
                Integer cnt = map.get(language.getLang());
                if (cnt == null) {
                    map.put(language.getLang(), 1);
                } else {
                    map.put(language.getLang(), ++cnt);
                }
            }
            long elapsed = System.currentTimeMillis() - start;
            if (j > 0) {
                sum += elapsed;
            }
        }
        return sum;
    }

    @Test
    public void testConfidence() throws Exception {
        String s = "estava em uma marcenaria na Rua Bruno http://www.cnn.com blahdeblah@gmail.com";

        LanguageDetector ld = new LanguageDetectorME(LANG_MODEL);

        for (int i = 1; i < 10; i++) {
            run(s, i, ld);

        }
        for (int i = 20; i < 100; i += 10) {
            run(s, i, ld);
        }

        for (int i = 200; i < 10000; i += 100) {
            run(s, i, ld);
        }

    }

    private void run(String s, int copies, LanguageDetector ld) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < copies; i++) {
            sb.append(s).append(" ");
        }
        long start = System.currentTimeMillis();
        Language[] predicted = ld.predictLanguages(sb);
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("length: " + sb.length() + " :: " + elapsed);
        double sum = 0;
        for (int i = 0; i < 3 && i < predicted.length; i++) {
            System.out.println("\t" + predicted[i].getLang() + " " + predicted[i].getConfidence());
            sum += predicted[i].getConfidence();
        }
        System.out.println("SUM " + sum);
    }

    @Test
    public void debugConfOverTime() throws Exception {
        Path sampleFile = Paths.get("C:\\data\\langid\\leipzig_short\\leipzig_1000-sents.txt");
        Map<String, List<String>> langs = loadSample(sampleFile);
        LanguageDetector ld = new LanguageDetectorME(LANG_MODEL);

        int numIterations = 100;
        int correct = 0;
        int total = 0;
        long start = System.currentTimeMillis();
        SummaryStatistics lengthSumm = new SummaryStatistics();
        for (Map.Entry<String, List<String>> e : langs.entrySet()) {
            String lang = e.getKey();
            List<String> sents = e.getValue();
            SummaryStatistics confSumm = new SummaryStatistics();
            for (int i = 0; i < numIterations; i++) {
                correct += runIteration(lang, sents, ld, confSumm, lengthSumm);
                total++;
            }
            /*if (confSumm.getN() > 0) {
                System.out.println(
                        String.format("|%s|%.2f|%.2f|%.1f|%.1f|",
                                lang,
                                confSumm.getMean(),
                                confSumm.getStandardDeviation(),
                                lengthSumm.getMean(),
                                lengthSumm.getStandardDeviation()));
            }*/
        }
        long elapsed = System.currentTimeMillis()-start;
        System.out.println("corr: "+correct + " : "+total + " : "+lengthSumm.getMean() + " "+elapsed);

    }

    private int runIteration(String lang, List<String> sents, LanguageDetector ld,
                              SummaryStatistics confSumm, SummaryStatistics lengthSumm) {
        Collections.shuffle(sents);

        boolean printed = false;
        double maxWrongConf = -1.0;
        int maxWrongLength = -1;
        String maxWrongLang = "";
        LinkedList<Language> queue = new LinkedList<>();
        int maxQueueSize = 5;

        for (int len = 10; len < 10000; len += 20) {
            Collections.shuffle(sents);
            String txt = getText(len, sents);
            Language[] detected = ld.predictLanguages(txt);
            if (!detected[0].getLang().equals(lang)) {
                double conf = detected[0].getConfidence();
                if (conf > maxWrongConf) {
                    maxWrongConf = conf;
                    maxWrongLength = len;
                    maxWrongLang = detected[0].getLang();
                }
            }
            queue.add(detected[0]);
            if (queue.size() > maxQueueSize) {
                queue.remove();
            }
            if (queue.size() == maxQueueSize && seenEnough(queue)) {
                lengthSumm.addValue(len);
                if (!lang.equals(queue.getLast().getLang())) {
                   /* System.out.println("wrong "+len + " "+lang + " : "+queue.getLast().getLang());
                    for (Language language : queue) {
                        System.out.println(language.getLang() + " : "+ language.getConfidence());
                    }*/
                    return 0;
                } else {
                    return 1;
                    //System.out.println("right");
                }
            }

/*
                if (detected[0].getConfidence() > 0.8) {
                    System.out.println("|"+lang + "|"+len+"|" +
                            detected[0].getLang() + "|"+String.format("%.2f", detected[0].getConfidence())+
                            "|"+detected[1].getLang() + "|"+String.format("%.2f", detected[1].getConfidence()) +
                            "|"+detected[0].getLang().equals(lang)+"|");
                    printed = true;
                    break;
                }*/
        }
        return 0;
        /*
        if (maxWrongConf > -1.0) {
            confSumm.addValue(maxWrongConf);
            lengthSumm.addValue(maxWrongLength);

            System.out.println(
                    String.format("|%s|%s|%.2f|%s|",
                            lang,
                            maxWrongLang,
                            maxWrongConf,
                            maxWrongLength));

        }
        if (!printed) {
            //System.out.println("never confident in: "+lang);
        }*/

    }

    private boolean seenEnough(LinkedList<Language> queue) {
        String last = null;
        double lastConf = -1.0;
        for (Language lang : queue) {
            if (last == null) {
                last = lang.getLang();
            } else if (! last.equals(lang.getLang())) {
                return false;
            } else if (lastConf > lang.getConfidence()) {
                return false;
            }
            lastConf = lang.getConfidence();
        }
        return true;
    }


    private String getText(int len, List<String> sents) {
        StringBuilder sb = new StringBuilder();
        for (String sent : sents) {
            sb.append(" ").append(sent);
            if (sb.length() > len) {
                break;
            }
        }
        sb.setLength(len);
        return sb.toString();
    }


    private Map<String, List<String>> loadSample(Path sampleFile) throws IOException {
        Map<String, List<String>> m = new TreeMap<>();
        try (BufferedReader r = Files.newBufferedReader(sampleFile, StandardCharsets.UTF_8)) {
            String line = r.readLine();
            while (line != null) {
                int t = line.indexOf("\t");

                if (t > -1) {
                    String lang = line.substring(0, t);
                    line = line.substring(t+1);
                    List<String> txts = m.get(lang);
                    if (txts == null) {
                        txts = new ArrayList<>();
                        m.put(lang, txts);
                    }
                    txts.add(line);
                }
                line = r.readLine();
            }
        }
        return m;
    }
}

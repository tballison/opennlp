package opennlp.tools.langdetect;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class LanguageDetectorSpeedTest {
    @Test
    public void testContextGenerators() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("estava em uma marcenaria na Rua Bruno http://www.cnn.com blahdeblah@gmail.com");
        }
        String txt = sb.toString();
        LanguageDetectorModel model =  new LanguageDetectorModel(new File("C:/data/langid/langdetect-183.bin"));
        LanguageDetector ld = new LanguageDetectorME(model);
        for (LanguageDetectorContextGenerator gen : new LanguageDetectorContextGenerator[]{
                new DefaultLanguageDetectorContextGenerator(1,3),
                new NGramCharContextGenerator(1, 3),
                new SlightlyFasterNGramCharContextGenerator(1, 3),
                new LuceneDetectorContextGenerator(1, 3)
        }) {
            long elapsed = runLoops(txt, new LanguageDetectorME(model, gen));
            System.out.println(gen.getClass().getSimpleName()+": "+elapsed);
        }
    }

    private long runLoops(String txt, LanguageDetector ld) {
        long sum = 0;
        for (int j = 0; j < 4; j++) {
            long start = System.currentTimeMillis();
            Map<String, Integer> map = new HashMap<>();
            for (int i = 0; i < 5000; i++) {
                Language language = ld.predictLanguage(txt);
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
}

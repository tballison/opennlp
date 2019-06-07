package opennlp.tools.langdetect;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharacterUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.standard.UAX29URLEmailTokenizerFactory;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.Test;

public class MyNGramTokenFilterTest {
    static String FIELD = "f";
    @Test
    public void basic() throws Exception {
        String txt = "the quick brown fox jumped";
        Analyzer analyzer = CustomAnalyzer.builder()
                .withTokenizer(UAX29URLEmailTokenizerFactory.class)
                .addTokenFilter(MyNGramTokenFilterFactory.class).build();
        TokenStream ts = analyzer.tokenStream(FIELD, txt);
        CharTermAttribute chars = ts.addAttribute(CharTermAttribute.class);
        ts.reset();
        while (ts.incrementToken()) {
            System.out.println(chars.toString());
        }
        ts.end();
        ts.close();
    }
    @Test
    public void test() throws Exception {
        String s = "the quick";
        char[] chars = s.toCharArray();
        int c = Character.offsetByCodePoints(chars, 0,4,2,1);
        System.out.println(c);
    }
}

package opennlp.tools.langdetect;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharacterUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.standard.UAX29URLEmailTokenizerFactory;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.Assert;
import org.junit.Test;

public class MyNGramTokenFilterTest {
    static String FIELD = "f";
    @Test
    public void basicBeyondBMP() throws Exception {
        String txt = new String(new int[]{66577, 66577, 66578}, 0, 3);

        List<String> expected = new ArrayList<>();
        expected.add(" \uD801\uDC11");
        expected.add(" \uD801\uDC11\uD801\uDC11");
        expected.add("\uD801\uDC11");
        expected.add("\uD801\uDC11\uD801\uDC11");
        expected.add("\uD801\uDC11\uD801\uDC11\uD801\uDC12");
        expected.add("\uD801\uDC11");
        expected.add("\uD801\uDC11\uD801\uDC12");
        expected.add("\uD801\uDC12");

        Analyzer analyzer = CustomAnalyzer.builder()
                .withTokenizer(UAX29URLEmailTokenizerFactory.class)
                .addTokenFilter(MyNGramTokenFilterFactory.class).build();
        TokenStream ts = analyzer.tokenStream(FIELD, txt);
        CharTermAttribute chars = ts.addAttribute(CharTermAttribute.class);
        ts.reset();
        while (ts.incrementToken()) {
            String t = chars.toString();
            Assert.assertEquals(t, expected.remove(0));
        }
        ts.end();
        ts.close();
        Assert.assertEquals(0, expected.size());
    }
    @Test
    public void test() throws Exception {
        String txt = "普林斯顿大学";
        List<String> expected = new ArrayList<>();
        expected.add(" 普");
        expected.add(" 普林");
        expected.add("普");
        expected.add("普林");
        expected.add("普林斯");
        expected.add("林");
        expected.add("林斯");
        expected.add("林斯顿");
        expected.add("斯");
        expected.add("斯顿");
        expected.add("斯顿大");
        expected.add("顿");
        expected.add("顿大");
        expected.add("顿大学");
        expected.add("大");
        expected.add("大学");
        expected.add("学");

        //txt = "abc def ghi";
        Analyzer analyzer = CustomAnalyzer.builder()
                .withTokenizer(UAX29URLEmailTokenizerFactory.class)
                .addTokenFilter(MyNGramTokenFilterFactory.class).build();
        TokenStream ts = analyzer.tokenStream(FIELD, txt);
        CharTermAttribute chars = ts.addAttribute(CharTermAttribute.class);
        ts.reset();
            while (ts.incrementToken()) {
                String t = chars.toString();
                Assert.assertEquals(expected.remove(0), t);
            }
            ts.end();
            ts.close();
        Assert.assertEquals(0, expected.size());

    }

    @Test
    public void testCodePointsAt() throws Exception {
//        char[] c = new char[] { 'a', 'b', 'c', 'd', 'e', 'f' };
        //PEE, PEE, BEE
        String s = new String(new int[]{66577, 66577, 66578}, 0, 3);
        char[] c = s.toCharArray();
        System.out.println(s + " : " + c.length);
        int start = 0;
        int count = 6;
        int index = 4;
        int codePointOffset = 1;
        int result = Character.offsetByCodePoints(c, start, count, index, codePointOffset);
        System.out.println(index + " : " + result);

        //codePointOffset must be < start+count
        //index has to be >= start, < count,
        //and it starts from the beginning of the array, not from the start
        //and the index is the character count, NOT the number of codepoints
        // from the start of the array

/*        for (int i = 0; i < 6; i++) {
            int result = Character.offsetByCodePoints(c, start, count, i, codePointOffset);
            System.out.println(i + " : " + result);
        }*/
    }
}

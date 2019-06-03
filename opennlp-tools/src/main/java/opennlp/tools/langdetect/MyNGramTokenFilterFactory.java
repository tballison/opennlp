package opennlp.tools.langdetect;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.TokenFilterFactory;

public class MyNGramTokenFilterFactory extends TokenFilterFactory {

    private final int min;
    private final int max;
    public MyNGramTokenFilterFactory(Map<String, String> args) {
        super(args);
        if (args.containsKey("minGramSize")) {
            min = Integer.parseInt(args.get("minGramSize"));
        } else {
            min = 1;
        }
        if (args.containsKey("maxGramSize")) {
            max = Integer.parseInt(args.get("maxGramSize"));
        } else {
            max = 3;
        }
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new MyNGramTokenFilter(tokenStream, min, max);
    }

}

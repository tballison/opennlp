package opennlp.tools.langdetect;

import java.util.Map;

import org.apache.lucene.analysis.TokenStream;

import org.apache.lucene.analysis.util.TokenFilterFactory;

public class MyUAXTypeFilterFactory extends TokenFilterFactory {


    public MyUAXTypeFilterFactory(Map<String, String> args) {
        super(args);
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new MyUAXTypeFilter(tokenStream);
    }
}

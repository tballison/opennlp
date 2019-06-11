package opennlp.tools.langdetect;

import static org.apache.lucene.analysis.standard.UAX29URLEmailTokenizer.EMAIL;
import static org.apache.lucene.analysis.standard.UAX29URLEmailTokenizer.EMOJI;
import static org.apache.lucene.analysis.standard.UAX29URLEmailTokenizer.URL;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.analysis.FilteringTokenFilter;
import org.apache.lucene.analysis.TokenStream;

import org.apache.lucene.analysis.standard.UAX29URLEmailTokenizer;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.util.TokenFilterFactory;

public class MyUAXTypeFilterFactory extends TokenFilterFactory {


    public MyUAXTypeFilterFactory(Map<String, String> args) {
        super(args);
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new MyUAXTypeFilter(tokenStream);
    }

    private static class MyUAXTypeFilter extends FilteringTokenFilter {

        private final TypeAttribute type;
        public MyUAXTypeFilter(TokenStream in) {
            super(in);
            this.type = this.getAttribute(TypeAttribute.class);

        }

        @Override
        protected boolean accept() throws IOException {
            String tp = type.type();
            if (tp == UAX29URLEmailTokenizer.TOKEN_TYPES[URL] ||
                    tp == UAX29URLEmailTokenizer.TOKEN_TYPES[EMOJI] ||
                    tp == UAX29URLEmailTokenizer.TOKEN_TYPES[EMAIL]) {
                return false;
            }
            return true;
        }
    }
}

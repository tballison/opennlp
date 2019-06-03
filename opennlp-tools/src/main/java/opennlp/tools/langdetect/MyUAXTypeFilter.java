package opennlp.tools.langdetect;

import static org.apache.lucene.analysis.standard.UAX29URLEmailTokenizer.EMAIL;
import static org.apache.lucene.analysis.standard.UAX29URLEmailTokenizer.EMOJI;
import static org.apache.lucene.analysis.standard.UAX29URLEmailTokenizer.URL;

import java.io.IOException;

import org.apache.lucene.analysis.FilteringTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.UAX29URLEmailTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

/**
 * Optimized TypeFilter that uses == instead of equals() for
 * {@link UAX29URLEmailTokenizer#TOKEN_TYPES}.
 *
 * It actually did make a difference...
 */
public class MyUAXTypeFilter extends FilteringTokenFilter {

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

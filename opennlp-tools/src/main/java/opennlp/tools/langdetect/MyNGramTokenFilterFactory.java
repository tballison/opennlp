package opennlp.tools.langdetect;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
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

    /**
     * Copied from Lucene.  This adds a single space before and after each token
     * as a start/end word sentinel.
     */
    private static class MyNGramTokenFilter extends TokenFilter {
        private static final char SPACE = ' ';
        private final int minGram;
        private final int maxGram;
        private char[] buffer = new char[1000];
        private int curTermLength;
        private int curTermCodePointCount;
        private int curGramSize;
        private int curPos;
        private int curPosIncr;
        //this is the logical end of the buffer
        private int bufferEnd = 0;
        private boolean noMoreTokens = false;
        private State state;
        private final CharTermAttribute termAtt;
        private final PositionIncrementAttribute posIncrAtt;

        public MyNGramTokenFilter(TokenStream input, int minGram, int maxGram) {
            super(input);
            this.termAtt = (CharTermAttribute)this.addAttribute(CharTermAttribute.class);
            this.posIncrAtt = (PositionIncrementAttribute)this.addAttribute(PositionIncrementAttribute.class);
            if (minGram < 1) {
                throw new IllegalArgumentException("minGram must be greater than zero");
            } else if (minGram > maxGram) {
                throw new IllegalArgumentException("minGram must not be greater than maxGram");
            } else {
                this.minGram = minGram;
                this.maxGram = maxGram;
            }
        }

        public final boolean incrementToken() throws IOException {
            while(true) {

                //|| this.curPos + this.curGramSize > this.curTermCodePointCount
                if (this.curGramSize > this.maxGram) {
                    ++this.curPos;
                    this.curGramSize = this.minGram;
                }
                if (bufferEnd == 0 || curPos + maxGram >= bufferEnd) {
                    if (! noMoreTokens) {
                        appendBuffer();
                    }
                }
                if (buffer[this.curPos] == SPACE && curGramSize == 1) {
                    curGramSize++;
                }
                if (this.curPos + this.curGramSize <= bufferEnd) {
                    this.restoreState(this.state);
/*                int start = Character.offsetByCodePoints(this.curTermBuffer, 0, this.curTermLength, 0, this.curPos);
                int end = Character.offsetByCodePoints(this.curTermBuffer, 0, this.curTermLength, start, this.curGramSize);
                this.termAtt.copyBuffer(this.curTermBuffer, start, end - start);*/
                    int count = this.curTermLength;
                    if (this.curPos + this.curTermLength > this.bufferEnd) {
                        count = this.bufferEnd-curPos;
                    }
                    int start = Character.offsetByCodePoints(this.buffer, this.curPos,
                            count, this.curPos, 0);
                    int end = Character.offsetByCodePoints(this.buffer, this.curPos,
                            count, start, this.curGramSize);
                    this.termAtt.copyBuffer(this.buffer, start, end - start);

                    this.posIncrAtt.setPositionIncrement(this.curPosIncr);
                    this.curPosIncr = 0;
                    ++this.curGramSize;
                    return true;
                } else if (noMoreTokens) {
                    while (curPos+curGramSize >= bufferEnd && curGramSize > minGram) {
                        curGramSize--;
                    }
                    if (curGramSize == minGram) {
                        curPos++;
                    }
                    if (curPos + minGram > bufferEnd) {
                        return false;
                    }
                }
            }
        }

        private void appendBuffer() throws IOException {
            if (!this.input.incrementToken()) {
                noMoreTokens = true;
                return;

            }
            this.state = this.captureState();
            this.curTermLength = this.termAtt.length();
            this.curTermCodePointCount = Character.codePointCount(this.termAtt, 0, this.termAtt.length());
            this.curPosIncr += this.posIncrAtt.getPositionIncrement();
            this.curGramSize = this.minGram;

            //+1 for the potential dividing space
            if ((this.curPos+curTermLength+1) >= this.buffer.length) {
                //have to start over from the beginning
                int lenToCopy = bufferEnd-curPos;
                System.arraycopy(this.buffer, curPos, this.buffer, 0, lenToCopy);
                this.curPos = 0;
                bufferEnd = lenToCopy;
            }
            if (posIncrAtt.getPositionIncrement() > 0) {
                this.buffer[this.bufferEnd] = SPACE;
                this.bufferEnd++;
            }

            System.arraycopy(termAtt.buffer(), 0, buffer, bufferEnd, curTermLength);
            bufferEnd += curTermLength;
        }

        public void reset() throws IOException {
            super.reset();
            this.curPosIncr = 0;
            this.bufferEnd = 0;
            this.curPos = 0;
        }

        public void end() throws IOException {
            super.end();
            this.posIncrAtt.setPositionIncrement(this.curPosIncr);
        }
    }
}

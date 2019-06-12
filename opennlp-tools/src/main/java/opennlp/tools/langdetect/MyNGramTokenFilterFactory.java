package opennlp.tools.langdetect;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
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
        private final int maxTokenLength = 1024;//in chars
        private char[] buffer = new char[2*maxTokenLength+1];
        private int curTermLength;
        private int curGramSize;
        private int curPos;//literal offsets in the char array, not codepoint offsets
        //this is the logical end of the buffer
        private int bufferEnd = 0;
        private boolean noMoreTokens = false;
        private State state;
        private final CharTermAttribute termAtt;
        private final OffsetAttribute offsetAtt;
        private int lastOffset = -1;

        public MyNGramTokenFilter(TokenStream input, int minGram, int maxGram) {
            super(input);
            this.termAtt = this.addAttribute(CharTermAttribute.class);
            this.offsetAtt = this.addAttribute(OffsetAttribute.class);
            if (minGram < 1) {
                throw new IllegalArgumentException("minGram must be greater than zero");
            } else if (minGram > maxGram) {
                throw new IllegalArgumentException("minGram must not be greater than maxGram");
            } else {
                this.minGram = minGram;
                this.curGramSize = minGram;
                this.maxGram = maxGram;
            }
        }

        public final boolean incrementToken() throws IOException {
            while(true) {

                if (this.curGramSize > this.maxGram) {
                    incrementCurrPos();
                    this.curGramSize = this.minGram;
                }
                if (bufferEnd == 0 || (curPos + maxGram*2) > bufferEnd) {
                    if (! noMoreTokens) {
                        appendBuffer();
                    }
                }
                if (buffer[this.curPos] == SPACE && curGramSize == 1 && maxGram > 1) {
                    curGramSize++;
                }
                int start = this.curPos;
                int end = getEnd(curGramSize);

                if (end > -1 && end <= bufferEnd) {
                    this.restoreState(this.state);
                    this.termAtt.copyBuffer(this.buffer, start, end - start);
                    ++this.curGramSize;
                    return true;
                } else if (noMoreTokens) {
                    int endEnd = getEnd(curGramSize);
                    while ((endEnd >= bufferEnd || endEnd < 0) && curGramSize > minGram) {
                        curGramSize--;
                        endEnd = getEnd(curGramSize);
                    }
                    if (curGramSize == minGram) {
                        incrementCurrPos();
//                        curGramSize = minGram;
                    }
                    if (getEnd(curGramSize) < 0) {
                        return false;
                    }
                }
            }
        }

        /**
         *
         * @return 1 the end (exclusive) or -1 if there are too many codepoints
         * in the currGramSize before the buffer end.
         */
        private int getEnd(int codePointCount) {
            int end = -1;
            try {
                end = Character.offsetByCodePoints(this.buffer, this.curPos,
                        bufferEnd-this.curPos, this.curPos, codePointCount);
            } catch (IndexOutOfBoundsException e) {
                //swallow
            }
            return end;
        }

        private void incrementCurrPos() {
            ++this.curPos;
            if (Character.isLowSurrogate(this.buffer[this.curPos])) {
                ++this.curPos;
            }
        }

        private void appendBuffer() throws IOException {
            if (!this.input.incrementToken()) {
                noMoreTokens = true;
                return;
            }
            this.state = this.captureState();
            this.curTermLength = this.termAtt.length();

            //+1 for the potential dividing space
            if ((bufferEnd+curTermLength+1) >= this.buffer.length) {
                //have to start over from the beginning
                int lenToCopy = bufferEnd-curPos;
                System.arraycopy(this.buffer, curPos, this.buffer, 0, lenToCopy);
                this.curPos = 0;
                bufferEnd = lenToCopy;
            }
            if (offsetAtt.startOffset()-lastOffset > 0) {
                this.buffer[this.bufferEnd] = SPACE;
                this.bufferEnd++;
            }
            System.arraycopy(termAtt.buffer(), 0, buffer, bufferEnd, curTermLength);
            bufferEnd += curTermLength;
            lastOffset = offsetAtt.endOffset();
        }

        public void reset() throws IOException {
            super.reset();
            this.bufferEnd = 0;
            this.curPos = 0;
            this.lastOffset = -1;
            this.curTermLength = 0;
            this.curGramSize = minGram;
            this.noMoreTokens = false;
        }

        public void end() throws IOException {
            super.end();
            //this.posIncrAtt.setPositionIncrement(this.curPosIncr);
        }
    }
}

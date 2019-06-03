package opennlp.tools.langdetect;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//



import java.io.IOException;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.AttributeSource.State;

/**
 * Copied from Lucene.  This adds a single space before and after each token
 * as a start/end word sentinel.
 */
public final class MyNGramTokenFilter extends TokenFilter {
    private final int minGram;
    private final int maxGram;
    private char[] curTermBuffer;
    private int curTermLength;
    private int curTermCodePointCount;
    private int curGramSize;
    private int curPos;
    private int curPosIncr;
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
            if (this.curTermBuffer == null) {
                if (!this.input.incrementToken()) {
                    return false;
                }

                this.state = this.captureState();
                this.curTermLength = this.termAtt.length()+2;
                this.curTermCodePointCount = Character.codePointCount(this.termAtt, 0, this.termAtt.length())+2;
                this.curPosIncr += this.posIncrAtt.getPositionIncrement();
                this.curPos = 0;

                this.curTermBuffer = new char[curTermLength];
                curTermBuffer[0] = ' ';
                curTermBuffer[curTermBuffer.length-1] = ' ';
                System.arraycopy(termAtt.buffer(), 0, curTermBuffer, 1, curTermLength-2);
                this.curGramSize = this.minGram;
            }

            if (this.curGramSize > this.maxGram || this.curPos + this.curGramSize > this.curTermCodePointCount) {
                ++this.curPos;
                this.curGramSize = this.minGram;
            }

            if (this.curPos + this.curGramSize <= this.curTermCodePointCount) {
                this.restoreState(this.state);
                int start = Character.offsetByCodePoints(this.curTermBuffer, 0, this.curTermLength, 0, this.curPos);
                int end = Character.offsetByCodePoints(this.curTermBuffer, 0, this.curTermLength, start, this.curGramSize);
                this.termAtt.copyBuffer(this.curTermBuffer, start, end - start);
                this.posIncrAtt.setPositionIncrement(this.curPosIncr);
                this.curPosIncr = 0;
                ++this.curGramSize;
                return true;
            }

            this.curTermBuffer = null;
        }
    }

    public void reset() throws IOException {
        super.reset();
        this.curTermBuffer = null;
        this.curPosIncr = 0;
    }

    public void end() throws IOException {
        super.end();
        this.posIncrAtt.setPositionIncrement(this.curPosIncr);
    }
}


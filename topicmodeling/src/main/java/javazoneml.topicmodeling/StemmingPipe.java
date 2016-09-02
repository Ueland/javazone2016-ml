package javazoneml.topicmodeling;

import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;
import javazoneml.tools.preprocessing.Stemmer;

public class StemmingPipe {
    Stemmer stemmer = new Stemmer();

    public Instance pipe (Instance carrier)
    {
        TokenSequence ts = (TokenSequence) carrier.getData();
        for (Token t : ts) {
            String stemmed = stemmer.stem(t.getText());
            t.setText(stemmed);
        }
        return carrier;
    }
}

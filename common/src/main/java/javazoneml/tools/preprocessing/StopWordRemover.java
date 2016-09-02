package javazoneml.tools.preprocessing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StopWordRemover {
    private static final String space = " ";
    private List<String> stopWords = new ArrayList<>();

    public StopWordRemover(List<String> stopWords){
        this.stopWords = stopWords;
    }

    public String removeStopWords(String str){
        if(str == null){
            return null;
        }
        return trimAndSplit(str).stream().filter(s -> !isStopWord(s)).collect(Collectors.joining(space));
    }

    private List<String> trimAndSplit(String str){
        str = str.replaceAll("\\s+", space).trim();
        return Arrays.asList(str.split(space));
    }

    private boolean isStopWord(String str){
        return stopWords.contains(str.toLowerCase());
    }
}

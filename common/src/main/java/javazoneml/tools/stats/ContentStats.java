package javazoneml.tools.stats;

import com.google.common.base.Splitter;
import javazoneml.model.Presentation;
import javazoneml.tools.data.DataFilter;
import javazoneml.tools.data.DataProvider;
import javazoneml.tools.preprocessing.StopWordRemover;
import javazoneml.tools.preprocessing.StopWords;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ContentStats {

    private static List<String> noStopWords = new ArrayList<>();
    private static List<String> enStopWords = new ArrayList<>();

    static {
        noStopWords.addAll(StopWords.NO);
        noStopWords.addAll(StopWords.NO_SPECIFIC);

        enStopWords.addAll(StopWords.EN);
        enStopWords.addAll(StopWords.EN_SPECIFIC);
    }

    public static void main(String[] args) {
        new ContentStats().printTfIdfStats("no");
    }

    public void printTfIdfStats(String language) {
        DataProvider reader = new DataProvider();
        List<Predicate<Presentation>> excludes = new ArrayList<>();
        excludes.addAll(DataFilter.EXCLUDES);
        excludes.add(p -> (p.getSummary() == null) && (p.getDescription() == null));
        List<Presentation> presentations = reader.getAll(excludes);

        List<List<String>> documents = getDocumentsContent(presentations, language);

        HashMap<String, Double> tfidf = getTfIdfRanking(documents);

        System.out.println("=== Sorted Tf-IDF ===");
        tfidf.entrySet().stream().sorted(Map.Entry.<String, Double> comparingByValue()).forEach(System.out::println);
    }

    private HashMap<String, Double> getTfIdfRanking(List<List<String>> documents) {

        HashMap<String, Double> tfidf = new HashMap<>();
        HashMap<String, List<Double>> tfidf_tmp = new HashMap<>();

        System.out.println("Processing " + documents.size() + " documents");
        int counter = 0;
        for (List<String> document : documents) {
            for (String word : document) {
                double result = getTfIdf(document, documents, word);

                if (tfidf_tmp.get(word) == null)
                    tfidf_tmp.put(word, new LinkedList<>());

                tfidf_tmp.get(word).add(result);
            }

            if (++counter % 10 == 0)
                System.out.println(counter + ".. done");
        }

        for (String word : tfidf_tmp.keySet()) {
            Double result = tfidf_tmp.get(word).stream().collect(Collectors.averagingDouble(d -> d));
            tfidf.put(word, result);
        }

        System.out.println("All documents processed!\n\n");

        return tfidf;
    }

    private double getTfIdf(List<String> document, List<List<String>> documents, String term) {
        double tf = getTermFreqency(document, term);
        double idf = getInverseDocumentFrequency(documents, term);
        return tf * idf;
    }

    private double getTermFreqency(List<String> document, String term) {
        double counter = 0;

        for (String token : document) {
            if (term.equalsIgnoreCase(token))
                counter++;
        }

        return counter / document.size();
    }

    private double getInverseDocumentFrequency(List<List<String>> documents, String term) {

        double counter = 0;
        for (List<String> document : documents) {
            for (String token : document) {
                if (term.equalsIgnoreCase(token)) {
                    counter++;
                    break;
                }
            }
        }

        return Math.log10(documents.size() / counter);
    }

    private List<List<String>> getDocumentsContent(List<Presentation> presentations, String language) {
        List<List<String>> documents = new LinkedList<>();
        Splitter splitter = Splitter.on(' ');

        for (Presentation presentation : presentations) {
            if (language.equalsIgnoreCase(presentation.getLanguage())) {
                String document = "";
                if (!StringUtils.isEmpty(presentation.getSummary())
                        && !presentation.getSummary().equals(presentation.getDescription()))
                    document = normalizeText(presentation.getSummary(), language) + " ";

                document = document + normalizeText(presentation.getDescription(), language);
                documents.add(splitter.splitToList(document));
            }
        }

        return documents;
    }

    private String removeStopWords(String document, List<String> stopWords) {
        StopWordRemover remover = new StopWordRemover(stopWords);
        return remover.removeStopWords(document);
    }

    private String normalizeText(String input, String language) {

        if (StringUtils.isEmpty(input))
            return "";

        String s = input.replaceAll("[^a-zA-Z]", " ").toLowerCase();

        if ("no".equalsIgnoreCase(language))
            s = removeStopWords(s, noStopWords);
        else
            s = removeStopWords(s, enStopWords);

        return s;
    }
}

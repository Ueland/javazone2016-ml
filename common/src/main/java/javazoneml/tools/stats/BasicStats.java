package javazoneml.tools.stats;

import javazoneml.model.Presentation;
import javazoneml.tools.data.DataFilter;
import javazoneml.tools.data.DataProvider;

import java.util.*;
import java.util.function.Predicate;

public class BasicStats {

    public static void main(String[] args) {
        new BasicStats().printCommonStats();
        new BasicStats().printContentStats();
    }

    public void printCommonStats() {
        DataProvider reader = new DataProvider();
        List<Predicate<Presentation>> excludes = new ArrayList<>();
        excludes.addAll(DataFilter.EXCLUDES);
        excludes.add(p -> (p.getSummary() == null) && (p.getDescription() == null));
        List<Presentation> presentations = reader.getAll(excludes);

        HashMap<String, Long> stats = new HashMap<>();

        for (Presentation presentation : presentations) {
            stats.compute("level (" + presentation.getLevel() + ")", (k, v) -> v == null ? 1 : v + 1);
            stats.compute("language (" + presentation.getLanguage() + ")", (k, v) -> v == null ? 1 : v + 1);
            stats.compute("year (" + presentation.getYear() + ")", (k, v) -> v == null ? 1 : v + 1);
            stats.compute("format (" + presentation.getFormat() + ")", (k, v) -> v == null ? 1 : v + 1);
        }

        System.out.println("=== Common stats ===");
        stats.entrySet().stream().sorted(Map.Entry.<String, Long> comparingByKey()).forEach(System.out::println);
        System.out.println("--------------------");
    }

    public void printContentStats() {
        DataProvider reader = new DataProvider();
        List<Predicate<Presentation>> excludes = new ArrayList<>();
        excludes.addAll(DataFilter.EXCLUDES);
        excludes.add(p -> (p.getSummary() == null) && (p.getDescription() == null));
        List<Presentation> presentations = reader.getAll(excludes);

        HashMap<String, Long> stats = new HashMap<>();

        Set<Integer> years = new HashSet<>();
        for (Presentation presentation : presentations) {
            long wordCountSummary = getWordCount(presentation.getSummary());
            long wordCountDescription = getWordCount(presentation.getDescription());
            long wordCountTitle = getWordCount(presentation.getTitle());
            long wordCountKeywords = presentation.getKeyWords() == null ? 0 : presentation.getKeyWords().size();
            stats.compute("documents (" + presentation.getYear() + ")", (k, v) -> v == null ? 1 : v + 1);

            stats.compute("words-summary (" + presentation.getYear() + ")",
                    (k, v) -> v == null ? 1 : v + wordCountSummary);
            stats.compute("words-descr (" + presentation.getYear() + ")",
                    (k, v) -> v == null ? 1 : v + wordCountDescription);
            stats.compute("words-title (" + presentation.getYear() + ")", (k, v) -> v == null ? 1 : v + wordCountTitle);
            stats.compute("words-keywords (" + presentation.getYear() + ")",
                    (k, v) -> v == null ? 1 : v + wordCountKeywords);

            years.add(presentation.getYear());

            stats.compute("documents (all)", (k, v) -> v == null ? 1 : v + 1);
            stats.compute("words-summary (all)", (k, v) -> v == null ? 1 : v + wordCountSummary);
            stats.compute("words-descr (all)", (k, v) -> v == null ? 1 : v + wordCountDescription);
            stats.compute("words-title (all)", (k, v) -> v == null ? 1 : v + wordCountTitle);
            stats.compute("words-keywords (all)", (k, v) -> v == null ? 1 : v + wordCountKeywords);
        }

        for (int year : years) {
            long totalDocuments = stats.get("documents (" + year + ")");

            long totalWords = stats.get("words-summary (" + year + ")");
            stats.put("words-summary (" + year + "-avg)", totalWords / totalDocuments);

            totalWords = stats.get("words-descr (" + year + ")");
            stats.put("words-descr (" + year + "-avg)", totalWords / totalDocuments);

            totalWords = stats.get("words-title (" + year + ")");
            stats.put("words-title (" + year + "-avg)", totalWords / totalDocuments);

            totalWords = stats.get("words-keywords (" + year + ")");
            stats.put("words-keywords (" + year + "-avg)", totalWords / totalDocuments);
        }

        long alldocuments = stats.get("documents (all)");
        stats.put("words-summary (all-avg)", stats.get("words-summary (all)") / alldocuments);
        stats.put("words-descr (all-avg)", stats.get("words-descr (all)") / alldocuments);
        stats.put("words-title (all-avg)", stats.get("words-title (all)") / alldocuments);
        stats.put("words-keywords (all-avg)", stats.get("words-keywords (all)") / alldocuments);

        System.out.println("=== Content stats ===");
        stats.entrySet().stream().sorted(Map.Entry.<String, Long> comparingByKey()).forEach(System.out::println);
        System.out.println("---------------------");
    }

    private long getWordCount(String input) {
        if (input == null)
            return 0;

        String[] words = input.split(" ");
        if (words == null)
            return 0;
        else
            return words.length;
    }
}

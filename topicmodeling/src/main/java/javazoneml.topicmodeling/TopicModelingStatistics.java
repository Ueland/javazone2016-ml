package javazoneml.topicmodeling;

import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicAssignment;
import cc.mallet.types.Instance;
import javazoneml.model.Presentation;

import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class TopicModelingStatistics {

    public void printGroupedTopicDistributions(ParallelTopicModel model, TopicModeling.LanguageData languageData, PrintWriter out) {
        ArrayList<TopicAssignment> data = model.getData();
        Presentation[] presentations = getPresentations(languageData, data);
        double[][] documentTopics = model.getDocumentTopics(true, true);

        Map<Object, Integer> yearCount = new HashMap<>();
        Map<Object, double[]> yearDist = new HashMap<>();
        Map<Object, Integer> levelCount = new HashMap<>();
        Map<Object, double[]> levelDist = new HashMap<>();
        Map<Object, Integer> formatCount = new HashMap<>();
        Map<Object, double[]> formatDist = new HashMap<>();
        Map<Object, Integer> keywordsCount = new HashMap<>();
        Map<Object, double[]> keywordsDist = new HashMap<>();

        for (int i = 0; i < documentTopics.length; i++) {
            Presentation presentation = presentations[i];
            updateDistributions(yearCount, yearDist, presentation.getYear(), documentTopics[i]);
            updateDistributions(levelCount, levelDist, presentation.getLevel(), documentTopics[i]);
            updateDistributions(formatCount, formatDist, presentation.getFormat(), documentTopics[i]);
            if (presentation.getKeyWords() != null) {
                for (String keyword : presentation.getKeyWords()) {
                    updateDistributions(keywordsCount, keywordsDist, keyword, documentTopics[i]);
                }
            }

        }

        printDistribution(out, yearCount, yearDist);
        printDistribution(out, levelCount, levelDist);
        printDistribution(out, formatCount, formatDist);
        printDistribution(out, keywordsCount, keywordsDist);
    }

    private Presentation[] getPresentations(TopicModeling.LanguageData languageData, ArrayList<TopicAssignment> data) {
        Presentation[] presentations = new Presentation[data.size()];
        for (int i = 0; i < data.size(); i++) {
            Instance instance = data.get(i).instance;
            String title = instance.getName().toString().split(",")[0];
            presentations[i] = languageData.getPresentation(title);
            if (presentations[i] == null) {
                System.out.println(title);
            }
        }
        return presentations;
    }

    private void updateDistributions(Map<Object, Integer> counts, Map<Object, double[]> distributions, Object key, double[] distribution) {
        if (key != null) {
            if (counts.containsKey(key)) {
                counts.put(key, counts.get(key) + 1);
                double[] oldDist = distributions.get(key);
                double[] newDist = new double[oldDist.length];
                for (int j = 0; j < oldDist.length; j++) {
                    newDist[j] = oldDist[j] + distribution[j];
                }
                distributions.put(key, newDist);
            } else {
                counts.put(key, 1);
                distributions.put(key, distribution);
            }
        }
    }

    private void printDistribution(PrintWriter out, Map<Object, Integer> counts, Map<Object, double[]> distributions) {
        for (Object key : counts.keySet()) {
            int count = counts.get(key);
            out.print(key + "\t" + count + "\t");
            double[] distribution = distributions.get(key);
            for (double topic : distribution) {
                double val = topic / count;
                String valStr = String.format(Locale.getDefault(), "%,6f", val);
                out.print(valStr + "\t");
            }
            out.println();
        }
    }

    public void printTopicDistances(ParallelTopicModel model, PrintWriter out) {
        List<Distance> distances = calculateDistances(model);
        printDistanceType(
                out,
                distances,
                DistanceType.HELLINGER);
        printDistanceType(
                out,
                distances,
                DistanceType.EUCLIDEAN);
    }

    private void printDistanceType(PrintWriter out, List<Distance> distances, DistanceType type) {
        List<Distance> sorted = distances.stream()
                .filter(d -> d.type.equals(type))
                .sorted((d1, d2) -> Double.compare(d1.distance, d2.distance))
                .limit(100)
                .collect(Collectors.toList());
        out.println(String.format("100 smallest %s distances between topics", type.name));
        for (Distance distance : sorted) {
            out.println(distance.first + "\t" + distance.second + "\t" + distance.distance);
        }
    }

    private List<Distance> calculateDistances(ParallelTopicModel model) {
        List<Distance> distances = new ArrayList<>();
        double[][] documentTopics = model.getDocumentTopics(true, true);
        for (int i = 0; i < documentTopics.length; i++) {
            for (int j = i + 1; j < documentTopics.length; j++) {
                double[] iTopics = documentTopics[i];
                double[] jTopics = documentTopics[j];
                distances.add(new Distance(i, j, DistanceType.EUCLIDEAN, euclideanDistance(iTopics, jTopics)));
                distances.add(new Distance(i, j, DistanceType.HELLINGER, hellingerDistance(iTopics, jTopics)));
            }
        }

        return distances;
    }

    protected double euclideanDistance(double[] x, double[] y) {
        if (x == null || y == null || x.length != y.length) {
            throw new IllegalArgumentException();
        }
        double squares = 0.0;
        for (int i = 0; i < x.length; i++) {
            squares += Math.pow(x[i] - y[i], 2);
        }
        return Math.sqrt(squares);
    }

    protected double hellingerDistance(double[] x, double[] y) {
        double[] sqrtX = Arrays.stream(x).map(Math::sqrt).toArray();
        double[] sqrtY = Arrays.stream(y).map(Math::sqrt).toArray();
        return 1 / Math.sqrt(2) * euclideanDistance(sqrtX, sqrtY);
    }

    private enum DistanceType {
        HELLINGER("Hellinger"),
        EUCLIDEAN("Euclidean");

        String name;

        DistanceType(String name) {
            this.name = name;
        }
    }

    private static class Distance {
        int first, second;
        double distance;
        DistanceType type;

        public Distance(int first, int second, DistanceType type, double distance) {
            this.first = first;
            this.second = second;
            this.type = type;
            this.distance = distance;
        }
    }
}

package javazoneml.topicmodeling;

import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.Alphabet;
import cc.mallet.types.IDSorter;
import cc.mallet.types.InstanceList;
import javazoneml.model.Presentation;
import javazoneml.tools.data.DataFilter;
import javazoneml.tools.data.DataProvider;
import javazoneml.tools.preprocessing.StopWords;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * http://mallet.cs.umass.edu/topics-devel.php
 */
public class TopicModeling {
    private static List<String> noStopWords = new ArrayList<>();
    private static List<String> enStopWords = new ArrayList<>();
    private static LanguageData en, no;
    private static List<Integer> numberOfTopics = Arrays.asList(10, 15, 20, 25, 30, 50, 70, 100);
    private static List<Double> alphaValues = Arrays.asList(0.5, 0.1);
    private static List<Double> betaValues = Arrays.asList(0.05, 0.01);
    private TopicModelingStatistics topicModelingStatistics;

    static {
        noStopWords.addAll(StopWords.NO);
        noStopWords.addAll(StopWords.NO_SPECIFIC);

        enStopWords.addAll(StopWords.EN);
        enStopWords.addAll(StopWords.EN_SPECIFIC);
    }

    public TopicModeling(DataProvider dataProvider, TopicModelingStatistics topicModelingStatistics) {
        this.topicModelingStatistics = topicModelingStatistics;
        List<Predicate<Presentation>> excludes = new ArrayList<>();
        excludes.addAll(DataFilter.EXCLUDES);
        excludes.add(p -> (p.getSummary() == null || p.getSummary().length() < 100) && (p.getDescription() == null || p.getDescription().length() < 100));
        List<Presentation> presentations = dataProvider.getAll(excludes);
        no = new LanguageData("no", noStopWords, presentations);
        en = new LanguageData("en", enStopWords, presentations);
    }

    public void run() {
        try {
            List<LanguageData> languageDataList = Arrays.asList(no, en);//, no);
            for (LanguageData languageData : languageDataList) {
                StringBuilder fileContent = new StringBuilder();
                fileContent
                        .append("alpha").append(",")
                        .append("beta").append(",")
                        .append("topics").append(",")
                        .append("logLikelihood").append(",")
                        .append("perplexity").append("\n");
                for (Double alpha : alphaValues) {
                    for (Double beta : betaValues) {
                        for (int numTopics : numberOfTopics) {
                            ParallelTopicModel model = findTopics(languageData, numTopics, alpha, beta);
                            double logLikelihood = model.modelLogLikelihood();
                            fileContent.append(String.valueOf(alpha)).append(",")
                                    .append(String.valueOf(beta)).append(",");
                            fileContent.append(numTopics).append(",")
                                    .append(logLikelihood).append(",")
                                    .append(getPerplexity(logLikelihood, model.totalTokens)).append("\n");
                        }
                    }
                }
                writeToFile(String.format("stats_%s.txt", languageData.code), fileContent.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        TopicModeling topicModeling = new TopicModeling(new DataProvider(), new TopicModelingStatistics());
        topicModeling.run();
    }

    private TokenSequenceRemoveStopwords stopWordsRemover(LanguageData languageData) {
        List<String> stopWords = languageData.stopWords;
        String[] stopWordsArray = stopWords.toArray(new String[stopWords.size()]);
        TokenSequenceRemoveStopwords stopWordsRemover = new TokenSequenceRemoveStopwords();
        stopWordsRemover.addStopWords(stopWordsArray);
        return stopWordsRemover;
    }

    private InstanceList createInstanceList(LanguageData languageData) {
        ArrayList<Pipe> pipeList = new ArrayList<>();
        // Pipes: lowercase, tokenize, remove stopwords, map to features
        pipeList.add(new CharSequenceLowercase());
        pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")));
        pipeList.add(stopWordsRemover(languageData));
        pipeList.add(new TokenSequence2FeatureSequence());

        return new InstanceList(new SerialPipes(pipeList));
    }

    public ParallelTopicModel findTopics(
            LanguageData languageData,
            int numberOfTopics,
            double alpha,
            double beta) throws IOException {
        String data = languageData.document;
        String filename = languageData.code + "_" + numberOfTopics + "_a" + alpha + "_b" + beta;
        filename = filename.replace(".", "_");
        filename += ".txt";
        StringBuilder fileContent = new StringBuilder();
        InstanceList instances = createInstanceList(languageData);

        Reader dataReader = new InputStreamReader(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        instances.addThruPipe(new CsvIterator(dataReader, Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$"),
                3, 2, 1)); // data, label, name fields

        // Create a model with numberOfTopics topics
        //  Note that the first parameter is passed as the sum over topics, while
        //  the second is the parameter for a single dimension of the Dirichlet prior.
        double alphaSum = alpha * numberOfTopics;
        ParallelTopicModel model = new ParallelTopicModel(numberOfTopics, alphaSum, beta);

        model.addInstances(instances);

        // Use two parallel samplers, which each look at one half the corpus and combine
        //  statistics after every iteration.
        model.setNumThreads(2);

        // Run the model for 50 iterations and stop (this is for testing only,
        //  for real applications, use 1000 to 2000 iterations)
        model.setNumIterations(2000);
        model.estimate();

        // The data alphabet maps word IDs to strings
        Alphabet dataAlphabet = instances.getDataAlphabet();

        // Get an array of sorted sets of word ID/count pairs
        ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();

        // Show top 5 words in topics with proportions for the first document
        for (int topic = 0; topic < numberOfTopics; topic++) {
            Iterator<IDSorter> iterator = topicSortedWords.get(topic).iterator();

            Formatter out = new Formatter(new StringBuilder(), Locale.getDefault());
            out.format("%d\t", topic);
            int rank = 0;
            while (iterator.hasNext() && rank < 10) {
                IDSorter idCountPair = iterator.next();
                out.format("%s (%.0f) ", dataAlphabet.lookupObject(idCountPair.getID()), idCountPair.getWeight());
                rank++;
            }
            fileContent.append(out);
            fileContent.append("\n");
        }

        StringWriter stringWriter = new StringWriter();
        PrintWriter pw = new PrintWriter(stringWriter);
        model.printDocumentTopics(pw);
        model.printTopicDocuments(pw);
        topicModelingStatistics.printGroupedTopicDistributions(model, languageData, pw);
        topicModelingStatistics.printTopicDistances(model, pw);
        fileContent.append(stringWriter.toString());
        writeToFile(filename, fileContent.toString());
        return model;
    }

    private void writeToFile(String filename, String content) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(filename)))) {
            bw.write(content);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private double getPerplexity(double logLikelihood, int totalTokens) {
        return Math.exp(-logLikelihood / totalTokens);
    }

    public static class LanguageData {
        private final String code;
        private final List<String> stopWords;
        private String document;
        private Map<String, Presentation> presentationMap = new HashMap<>();

        public LanguageData(String code, List<String> stopWords, List<Presentation> presentations) {
            this.code = code;
            this.stopWords = stopWords;
            document = createDocument(presentations, code);
        }

        private String createDocument(List<Presentation> presentations, String code) {
            StringBuilder stringBuilder = new StringBuilder();
            presentations.stream().filter(presentation -> code.equals(presentation.getLanguage())).forEach(presentation -> {
                String title = getPresentationTitle(presentation);
                stringBuilder.append(getPresentationString(presentation, title));
                presentationMap.put(title, presentation);
            });
            return stringBuilder.toString();
        }

        private String getPresentationString(Presentation presentation, String title) {
            String description = presentation.getDescription() != null ? presentation.getDescription() : "";
            String summary = presentation.getSummary() != null ? presentation.getSummary() : "";
            String text = summary + " " + description;

            return title +
                    "," +
                    "X" +
                    "," +
                    text +
                    "\n";
        }

        private String getPresentationTitle(Presentation presentation) {
            String titleString = presentation.getTitle().replace("\n", " ").replace(",", " ").replace(" ", "_");
            return titleString + "_" + presentation.getYear();
        }

        public Presentation getPresentation(String title) {
            return presentationMap.getOrDefault(title, null);
        }
    }
}
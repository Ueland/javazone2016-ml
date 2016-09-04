package javazoneml.deeplearning;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.storage.StorageLevel;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.spark.impl.multilayer.SparkDl4jMultiLayer;
import org.deeplearning4j.spark.impl.paramavg.ParameterAveragingTrainingMaster;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * https://github.com/deeplearning4j/dl4j-spark-cdh5-examples/blob/master/src/main/java/org/deeplearning4j/examples/rnn/GravesLSTMCharModellingExample.java
 */
public class SparkGravesLSTMCharModeling extends GravesLSTMCharModeling{

    private static Map<Integer, Character> INT_TO_CHAR = getIntToChar();
    private static Map<Character, Integer> CHAR_TO_INT = getCharToInt();
    private static final int N_CHARS = INT_TO_CHAR.size();
    private static int nIn = CHAR_TO_INT.size();
    private static int nOut = CHAR_TO_INT.size();

    private static int sequenceLength = 5000;                      //Length of each sequence (used in truncated BPTT)
    private static int truncatedBPTTLength = 50;                  //Configuration for truncated BPTT. See http://deeplearning4j.org/usingrnns.html for details
    private static int lstmLayerSize = 300;                        //Number of units in each GravesLSTM layer
    private static int numEpochs = 50;                              //Total number of training + sample generation epochs
    private static double learningRate = 0.05;
    private static int nSamplesToGenerate = 4;                     //Number of samples to generate after each training epoch
    private static int nCharactersToSample = 2000;                  //Length of each sample to generate
    private static String generationInitialization = "Java";         //Optional character initialization; a random character is used if null
    // Above is Used to 'prime' the LSTM with a character sequence to continue/complete.
    // Initialization characters must all be in CharacterIterator.getMinimalCharacterSet() by default
    private MultiLayerNetwork multiLayerNetwork;
    private Random random = new Random(12345);

    public static void main(String[] args) throws Exception {
        SparkGravesLSTMCharModeling sparkGravesLSTMCharModeling = new SparkGravesLSTMCharModeling();
        sparkGravesLSTMCharModeling.doTraining();
    }

    private SparkGravesLSTMCharModeling(){
        multiLayerNetwork = new MultiLayerNetwork(getConfiguration(nIn, nOut, lstmLayerSize, learningRate, truncatedBPTTLength));
    }

    public SparkGravesLSTMCharModeling(MultiLayerNetwork multiLayerNetwork){
        this.multiLayerNetwork = multiLayerNetwork;
    }

    @Override
    public void doTraining() {
        multiLayerNetwork.init();
        printNetworkParameters(multiLayerNetwork);
        int examplesPerWorker = 50;      //How many examples should be used per worker (executor) when fitting?
        /* How frequently should we average parameters (in number of minibatches)?
        Averaging too frequently can be slow (synchronization + serialization costs) whereas too infrequently can result
        learning difficulties (i.e., network may not converge) */
        int averagingFrequency = 3;

        SparkConf sparkConf = new SparkConf();
        sparkConf.setMaster("local[*]");
        sparkConf.setAppName("LSTM_Char");
        JavaSparkContext sc = new JavaSparkContext(sparkConf);

        List<String> list = getPresentationDataSequence(sequenceLength);
        JavaRDD<String> rawStrings = sc.parallelize(list);
        rawStrings.persist(StorageLevel.MEMORY_ONLY());
        final Broadcast<Map<Character, Integer>> bcCharToInt = sc.broadcast(CHAR_TO_INT);

        //Set up the TrainingMaster. The TrainingMaster controls how learning is actually executed on Spark
        int examplesPerDataSetObject = 1;
        ParameterAveragingTrainingMaster tm = new ParameterAveragingTrainingMaster.Builder(examplesPerDataSetObject)
                .workerPrefetchNumBatches(2)
                .saveUpdater(true)
                .averagingFrequency(averagingFrequency)
                .batchSizePerWorker(examplesPerWorker)
                .build();
        SparkDl4jMultiLayer sparkNetwork = new SparkDl4jMultiLayer(sc, multiLayerNetwork, tm);

        JavaRDD<DataSet> data = rawStrings.map(new StringToDataSetFn(bcCharToInt));

        for (int i = 0; i < numEpochs; i++) {
            multiLayerNetwork = sparkNetwork.fit(data);
            System.out.println("Epoch: " + i + " score: " + sparkNetwork.getScore());
            printSample(random, generationInitialization, nCharactersToSample, nSamplesToGenerate);
        }

        writeNetworkToFile(multiLayerNetwork);
    }

    public void printSample(Random rnd, String initialization, int numChars, int numSamples) {
        System.out.println("--------------------");
        System.out.println("Sampling characters from network given initialization \"" +
                (initialization == null ? "" : initialization) + "\"");
        String[] samples = sampleCharactersFromNetwork(
                initialization,
                multiLayerNetwork,
                rnd,
                i -> INT_TO_CHAR.get(i),
                c -> CHAR_TO_INT.get(c),
                INT_TO_CHAR.size(),
                numChars, numSamples);
        for (int j = 0; j < samples.length; j++) {
            System.out.println("----- Sample " + j + " -----");
            System.out.println(samples[j]);
            System.out.println();
        }
    }

    public static class StringToDataSetFn implements Function<String, DataSet> {
        private final Broadcast<Map<Character, Integer>> ctiBroadcast;

        public StringToDataSetFn(Broadcast<Map<Character, Integer>> characterIntegerMap) {
            this.ctiBroadcast = characterIntegerMap;
        }

        @Override
        public DataSet call(String s) throws Exception {
            Map<Character, Integer> cti = ctiBroadcast.getValue();
            int length = s.length();
            INDArray features = Nd4j.zeros(1, N_CHARS, length - 1);
            INDArray labels = Nd4j.zeros(1, N_CHARS, length - 1);
            char[] chars = s.toCharArray();
            int[] f = new int[3];
            int[] l = new int[3];
            for (int i = 0; i < chars.length - 2; i++) {
                f[1] = cti.get(chars[i]);
                f[2] = i;
                l[1] = cti.get(chars[i + 1]);
                l[2] = i;

                features.putScalar(f, 1.0);
                labels.putScalar(l, 1.0);
            }
            return new DataSet(features, labels);
        }
    }

    private List<String> getPresentationDataSequence(int sequenceLength){
        String data = getPresentationData();
        List<String> list = new ArrayList<>();
        int length = data.length();
        int currIdx = 0;
        while (currIdx + sequenceLength < length) {
            int end = currIdx + sequenceLength;
            String substr = data.substring(currIdx, end);
            currIdx = end;
            list.add(substr);
        }
        return list;
    }

    private String getPresentationData(){
        String content = getDataLines().stream().collect(Collectors.joining("\n"));
        char[] chars = content.toCharArray();
        StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0; i< content.length(); i++){
            if(CHAR_TO_INT.containsKey(chars[i])){
                stringBuilder.append(chars[i]);
            }
        }
        return stringBuilder.toString();
    }

    /**
     * A minimal character set, with a-z, A-Z, 0-9 and common punctuation etc
     */
    private static char[] getValidCharacters() {
        List<Character> validChars = new LinkedList<>();
        for (char c = 'a'; c <= 'z'; c++) validChars.add(c);
        for (char c = 'A'; c <= 'Z'; c++) validChars.add(c);
        for (char c = '0'; c <= '9'; c++) validChars.add(c);
        char[] temp = {'!', '&', '(', ')', '?', '-', '\'', '"', ',', '.', ':', ';', ' ', '\n', '\t'};
        for (char c : temp) validChars.add(c);
        char[] out = new char[validChars.size()];
        int i = 0;
        for (Character c : validChars) out[i++] = c;
        return out;
    }

    private static Map<Integer, Character> getIntToChar() {
        Map<Integer, Character> map = new HashMap<>();
        char[] chars = getValidCharacters();
        for (int i = 0; i < chars.length; i++) {
            map.put(i, chars[i]);
        }
        return map;
    }

    private static Map<Character, Integer> getCharToInt() {
        Map<Character, Integer> map = new HashMap<>();
        char[] chars = getValidCharacters();
        for (int i = 0; i < chars.length; i++) {
            map.put(chars[i], i);
        }
        return map;
    }
}
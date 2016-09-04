package javazoneml.deeplearning;

import com.google.common.base.Strings;
import javazoneml.model.Presentation;
import javazoneml.tools.data.DataFilter;
import javazoneml.tools.data.DataProvider;
import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.*;
import org.deeplearning4j.nn.conf.layers.GravesLSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.util.ModelSerializer;
import org.joda.time.DateTime;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class GravesLSTMCharModeling {
    private DataProvider dataProvider = new DataProvider();

    public abstract void doTraining();

    protected List<String> getDataLines(){
        List<Predicate<Presentation>> excludes = new ArrayList<>();
        excludes.addAll(DataFilter.EXCLUDES);
        excludes.add(p -> (p.getSummary() == null) && (p.getDescription() == null));
        List<Presentation> presentations = dataProvider.getAll(excludes);
        Collections.shuffle(presentations);

        String content = presentations.stream()
                .filter(p -> p.getLanguage().equals("en"))
                .map(p -> p.getSummary()+"\n" + p.getDescription()).collect(Collectors.joining("\n"));
        List <String> lines = Arrays.asList(content.split("\n"));
        return lines.stream().filter(l -> !Strings.isNullOrEmpty(l)).collect(Collectors.toList());
    }

    protected MultiLayerConfiguration getConfiguration(int in, int out, int layerSize, double learningRate, int truncatedBPTTLength){
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .iterations(1)
                .learningRate(learningRate)
                .learningRateDecayPolicy(LearningRatePolicy.Score)
                .lrPolicyDecayRate(0.97)
                .learningRateScoreBasedDecayRate(0.97)
                .rmsDecay(0.95)
                .seed(12345)
                .weightInit(WeightInit.XAVIER)
                .activation("tanh")
                .updater(Updater.RMSPROP)
                .regularization(true)
                .l2(0.001)
                .list()
                .layer(0, new GravesLSTM.Builder().nIn(in).nOut(layerSize).build())
                .layer(1, new GravesLSTM.Builder().activation("tanh").nIn(layerSize).nOut(layerSize).build())
                .layer(2, new GravesLSTM.Builder().activation("tanh").nIn(layerSize).nOut(layerSize).build())
                .layer(3, new RnnOutputLayer.Builder(LossFunctions.LossFunction.MCXENT).activation("softmax")        //MCXENT + softmax for classification
                        .nIn(layerSize).nOut(out).build())
                .pretrain(false).backprop(true)
                .backpropType(BackpropType.TruncatedBPTT).tBPTTForwardLength(truncatedBPTTLength).tBPTTBackwardLength(truncatedBPTTLength)
                .build();
        return conf;
    }

    protected void printNetworkParameters(MultiLayerNetwork multiLayerNetwork){
        //Print the  number of parameters in the network (and for each layer)
        Layer[] layers = multiLayerNetwork.getLayers();
        int totalNumParams = 0;
        for (int i = 0; i < layers.length; i++) {
            int nParams = layers[i].numParams();
            System.out.println("Number of parameters in layer " + i + ": " + nParams);
            totalNumParams += nParams;
        }
        System.out.println("Total number of network parameters: " + totalNumParams);
    }

    /**
     * Generate a sample from the network, given an (optional, possibly null) initialization. Initialization
     * can be used to 'prime' the RNN with a sequence you want to extend/continue.<br>
     * Note that the initalization is used for all samples
     *
     * @param initialization     String, may be null. If null, select a random character as initialization for all samples
     * @param charactersToSample Number of characters to sample from network (excluding initialization)
     * @param net                MultiLayerNetwork with one or more GravesLSTM/RNN layers and a softmax output layer
     */
    protected String[] sampleCharactersFromNetwork(String initialization,
                                                   MultiLayerNetwork net,
                                                   Random random,
                                                   Function<Integer, Character> intToChar,
                                                   Function<Character, Integer> charToInt,
                                                   int numCharacters,
                                                   int charactersToSample,
                                                   int numSamples) {
        //Set up initialization. If no initialization: use a random character
        if (initialization == null) {
            int randomCharIdx = random.nextInt(numCharacters);
            initialization = String.valueOf(intToChar.apply(randomCharIdx));
        }

        //Create input for initialization
        INDArray initializationInput = Nd4j.zeros(numSamples, numCharacters, initialization.length());
        char[] init = initialization.toCharArray();
        for (int i = 0; i < init.length; i++) {
            int idx = charToInt.apply(init[i]);
            for (int j = 0; j < numSamples; j++) {
                initializationInput.putScalar(new int[]{j, idx, i}, 1.0f);
            }
        }

        StringBuilder[] sb = new StringBuilder[numSamples];
        for (int i = 0; i < numSamples; i++) sb[i] = new StringBuilder(initialization);

        //Sample from network (and feed samples back into input) one character at a time (for all samples)
        //Sampling is done in parallel here
        net.rnnClearPreviousState();
        INDArray output = net.rnnTimeStep(initializationInput);
        output = output.tensorAlongDimension(output.size(2) - 1, 1, 0);    //Gets the last time step output

        for (int i = 0; i < charactersToSample; i++) {
            //Set up next input (single time step) by sampling from previous output
            INDArray nextInput = Nd4j.zeros(numSamples, numCharacters);
            //Output is a probability distribution. Sample from this for each example we want to generate, and add it to the new input
            for (int s = 0; s < numSamples; s++) {
                double[] outputProbDistribution = new double[numCharacters];
                for (int j = 0; j < outputProbDistribution.length; j++)
                    outputProbDistribution[j] = output.getDouble(s, j);
                int sampledCharacterIdx = sampleFromDistribution(outputProbDistribution, random);

                nextInput.putScalar(new int[]{s, sampledCharacterIdx}, 1.0f);        //Prepare next time step input
                sb[s].append(intToChar.apply(sampledCharacterIdx));    //Add sampled character to StringBuilder (human readable output)
            }

            output = net.rnnTimeStep(nextInput);    //Do one time step of forward pass
        }

        String[] out = new String[numSamples];
        for (int i = 0; i < numSamples; i++) out[i] = sb[i].toString();
        return out;
    }

    /**
     * Given a probability distribution over discrete classes, sample from the distribution
     * and return the generated class index.
     *
     * @param distribution Probability distribution over classes. Must sum to 1.0
     */
    private int sampleFromDistribution(double[] distribution, Random rng) {
        double d = rng.nextDouble();
        double sum = 0.0;
        for (int i = 0; i < distribution.length; i++) {
            sum += distribution[i];
            if (d <= sum) return i;
        }
        //Should never happen if distribution is a valid probability distribution
        throw new IllegalArgumentException("Distribution is invalid? d=" + d + ", sum=" + sum);
    }

    protected void writeNetworkToFile(MultiLayerNetwork multiLayerNetwork){
        String filename =
                String.format("%s-%d-%d-%d",
                        this.getClass().getCanonicalName(),
                        DateTime.now().getYear(),
                        DateTime.now().getMonthOfYear(),
                        DateTime.now().getDayOfMonth())
                        .replace(".", "_");
        try {
            ModelSerializer.writeModel(multiLayerNetwork, filename, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

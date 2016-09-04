package javazoneml.deeplearning;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.dataset.DataSet;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Random;

/**
 * https://github.com/deeplearning4j/dl4j-examples/blob/master/dl4j-examples/src/main/java/org/deeplearning4j/examples/recurrent/character/GravesLSTMCharModellingExample.java
 */
public class StandardGravesLSTMCharModeling extends GravesLSTMCharModeling{
    private CharacterIterator characterIterator;
    private MultiLayerNetwork multiLayerNetwork;
    private int lstmLayerSize = 300;                    //Number of units in each GravesLSTM layer
    private int miniBatchSize = 50;                     //Size of mini batch to use when training
    private int exampleLength = 5000;                   //Length of each training example sequence to use. This could certainly be increased
    private int tbpttLength = 50;                       //Length for truncated backpropagation through time. i.e., do parameter updates ever 50 characters
    private int numEpochs = 100;                        //Total number of training epochs
    private double learningRate = 0.05;
    private int nSamplesToGenerate = 4;                 //Number of samples to generate after each training epoch
    private int nCharactersToSample = 1000;             //Length of each sample to generate
    private String generationInitialization = "Java";   //Optional character initialization; a random character is used if null
    private Random random = new Random(12345);

    public static void main(String[] args) throws Exception {
        StandardGravesLSTMCharModeling standardGravesLSTMCharModeling = new StandardGravesLSTMCharModeling();
        standardGravesLSTMCharModeling.doTraining();
    }

    public StandardGravesLSTMCharModeling(){

        //Get a DataSetIterator that handles vectorization of text into something we can use to train
        // our GravesLSTM network.
        characterIterator = getPresentationDataIterator(miniBatchSize, exampleLength);

        //Set up network configuration:
        MultiLayerConfiguration conf = getConfiguration(
                characterIterator.inputColumns(),
                characterIterator.totalOutcomes(),
                lstmLayerSize,
                learningRate,
                tbpttLength);

        multiLayerNetwork = new MultiLayerNetwork(conf);
    }

    public void doTraining(){
        multiLayerNetwork.init();
        multiLayerNetwork.setListeners(new ScoreIterationListener(1));
        printNetworkParameters(multiLayerNetwork);

        //Do training, and then generate and print samples from network
        for (int i = 0; i < numEpochs; i++) {
            while (characterIterator.hasNext()) {
                DataSet ds = characterIterator.next();
                multiLayerNetwork.fit(ds);
            }

            characterIterator.reset();    //Reset iterator for another epoch
            printSample(i);
        }
        writeNetworkToFile(multiLayerNetwork);
    }

    private void printSample(int epoch) {
        System.out.println("--------------------");
        System.out.println("Epoch: " + epoch);
        System.out.println("Sampling characters from network given initialization \"" + (generationInitialization == null ? "" : generationInitialization) + "\"");
        String[] samples = sampleCharactersFromNetwork(generationInitialization, multiLayerNetwork, characterIterator, random, nCharactersToSample, nSamplesToGenerate);
        for (int j = 0; j < samples.length; j++) {
            System.out.println("----- Sample " + j + " -----");
            System.out.println(samples[j]);
            System.out.println();
        }
    }

    private CharacterIterator getPresentationDataIterator(int miniBatchSize, int sequenceLength)  {
        List<String> dataLines = getDataLines();
        char[] validCharacters = CharacterIterator.getMinimalCharacterSet();
        try {
            return new CharacterIterator(dataLines, Charset.forName("UTF-8"),
                    miniBatchSize, sequenceLength, validCharacters, new Random(12345));
        } catch (IOException e) {
            return null;
        }
    }

    private String[] sampleCharactersFromNetwork(String initialization,
                                                 MultiLayerNetwork net,
                                                 CharacterIterator iterator,
                                                 Random rng,
                                                 int charactersToSample,
                                                 int numSamples) {
        return sampleCharactersFromNetwork(
                initialization,
                net,
                rng,
                iterator::convertIndexToCharacter,
                iterator::convertCharacterToIndex,
                iterator.inputColumns(),
                charactersToSample,
                numSamples);
    }
}
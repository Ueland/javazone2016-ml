package javazoneml.deeplearning;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ModelFileReader {

    public static void main(String [] args){
        try(InputStream inputStream = ModelFileReader.class.getClassLoader().getResourceAsStream("models/SparkGravesLSTMCharModeling")){
            MultiLayerNetwork multiLayerNetwork = ModelSerializer.restoreMultiLayerNetwork(inputStream);
            SparkGravesLSTMCharModeling sparkModel = new SparkGravesLSTMCharModeling(multiLayerNetwork);
            Random random = new Random(12345);
            List<String> initializations = Arrays.asList("java", "Code", "The", "Architecture", "This");
            for(String init : initializations){
                sparkModel.printSample(random, init, 2000, 5);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
package javazoneml.sentiment;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.deeplearning4j.models.embeddings.learning.impl.elements.CBOW;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.plot.BarnesHutTsne;
import org.deeplearning4j.text.sentenceiterator.LineSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.deeplearning4j.ui.UiServer;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import javazoneml.model.Presentation;
import javazoneml.model.Speaker;
import javazoneml.tools.data.DataFilter;
import javazoneml.tools.data.DataProvider;

public class DistributedWordModel {

	public static void main(String[] args) throws Exception {
		// train all the different models
		new DistributedWordModel().createWord2VecModel("src/main/resources/data/javazone-en-bio.txt",
				"target/javazone-en-bio.model");
		new DistributedWordModel().createWord2VecModel("src/main/resources/data/javazone-en-talk.txt",
				"target/javazone-en-talk.cbow.model");
		new DistributedWordModel().createWord2VecModel("src/main/resources/data/javazone-all-talk.txt",
				"target/javazone-all-talk.model");
		new DistributedWordModel().createWord2VecModel("src/main/resources/data/javazone-all-all.txt",
				"target/javazone-all-all.model");
		new DistributedWordModel().createWord2VecModelWithLemma("src/main/resources/data/javazone-en-all.txt",
				"target/javazone-en-all.cbow.lemma.model");
		new DistributedWordModel().createWord2VecModelWithLemma("src/main/resources/data/javazone-all-all.txt",
				"target/javazone-all-all.lemma.model");

		// print various stats
		new DistributedWordModel().printPositiveAndNegativeAssociations("target/javazone-all-all.lemma.model",
				Arrays.asList("happy", "best", "nice", "popluar", "fantastic", "excited", "funny", "perfect", "great",
						"impressive", "awesome", "wonderful", "excellent", "good", "lucky"),
				Arrays.asList("hate", "illegal", "wrong", "sad", "weak", "crazy", "criminal", "vulerable", "severe",
						"negative", "poor"),
				Arrays.asList("java", "scala", "ajax", "angular", "programming", "python", "agile", "soa", "frontend",
						"backend", "web", "devops", "spring", "framework", "design", "code", "developer", "software",
						"architect", "build"));

		new DistributedWordModel().printStrongestAssociations("target/javazone-all-all.lemma.model",
				"src/main/resources/data/partners.txt", "src/main/resources/data/words.txt", 10);

		new DistributedWordModel().printAssociationsForEntity("target/javazone-all-all.lemma.model",
				"src/main/resources/data/partners.txt",
				Arrays.asList("security", "devops", "frontend", "backend", "agile", "java"));

		new DistributedWordModel().printAssociations("target/javazone-all-all.lemma.model",
				"src/main/resources/data/partners.txt", Arrays.asList("security"));

		new DistributedWordModel().printNearestAssociationsForEntity("target/javazone-all-all.lemma.model",
				"src/main/resources/data/partners.txt", 10);
	}

	/**
	 * Print nearest associations for entity.
	 * 
	 * @param modelInput
	 * @param entity
	 * @param limit
	 * @throws IOException
	 */
	public void printNearestAssociationsForEntity(String modelInput, String entity, int limit) throws IOException {
		Word2Vec model = WordVectorSerializer.loadFullModel(modelInput);

		System.out.println("Checking entity " + entity);
		if (model.hasWord(entity)) {
			for (String word : model.wordsNearest(entity, limit)) {
				System.out.println(word + "\t" + String.valueOf(model.similarity(entity, word)).replace(".", ","));
			}

		}
	}

	/**
	 * Print associations for an entity for a list of words.
	 * 
	 * @param modelInput
	 * @param entity
	 * @param words
	 * @throws IOException
	 */
	public void printAssociationsForEntity(String modelInput, String entity, List<String> words) throws IOException {
		Word2Vec model = WordVectorSerializer.loadFullModel(modelInput);

		System.out.println("Checking entity " + entity);
		if (model.hasWord(entity)) {
			for (String word : words) {
				System.out.println(
						word + "\t" + String.valueOf(Math.abs(model.similarity(entity, word))).replace(".", ","));
			}

		}
	}

	/**
	 * Print associations for each entities for a list of words.
	 * 
	 * @param modelInput
	 * @param entitiesInput
	 * @param words
	 * @throws IOException
	 */
	public void printAssociations(String modelInput, String entitiesInput, List<String> words) throws IOException {
		Word2Vec model = WordVectorSerializer.loadFullModel(modelInput);
		List<String> entities = FileUtils.readLines(new File(entitiesInput), StandardCharsets.UTF_8.name());

		for (String word : words) {
			if (model.hasWord(word)) {
				System.out.println("Checking word " + word);
				HashMap<String, Double> similarities = new HashMap<>();

				for (String entity : entities) {
					if (model.hasWord(entity)) {
						double similiarity = model.similarity(entity, word);
						similarities.put(entity, Math.abs(similiarity));
					}
				}

				similarities.entrySet().stream().sorted(Map.Entry.<String, Double> comparingByValue().reversed())
						.forEach(p -> System.out
								.println(p.getKey() + "\t" + String.valueOf(Math.abs(p.getValue())).replace(".", ",")));
			}
		}
	}

	/**
	 * Print associations for each entities for words in a file.
	 * 
	 * @param modelInput
	 * @param entitiesInput
	 * @param wordsInput
	 * @throws IOException
	 */
	public void printAssociations(String modelInput, String entitiesInput, String wordsInput) throws IOException {
		Word2Vec model = WordVectorSerializer.loadFullModel(modelInput);
		List<String> entities = FileUtils.readLines(new File(entitiesInput), StandardCharsets.UTF_8.name());
		List<String> words = FileUtils.readLines(new File(wordsInput), StandardCharsets.UTF_8.name());

		for (String entity : entities) {
			System.out.println("Testing entity " + entity);
			if (!model.hasWord(entity)) {
				System.out.println("NA!");
			} else {
				for (String word : words) {
					if (model.hasWord(word)) {
						double similiarity = model.similarity(entity, word);
						System.out.println(word + "\t\t" + String.valueOf(Math.abs(similiarity)).replace(".", ","));
					} else {
						System.out.println(word + "\t\tNA");
					}
				}
			}
			System.out.println("=============");
		}
	}

	/**
	 * Print the strongest associations in sorted order for a entities and a
	 * given set of words.
	 * 
	 * @param modelInput
	 * @param entitiesInput
	 * @param wordsInput
	 * @param limit
	 * @throws IOException
	 */
	public void printStrongestAssociations(String modelInput, String entitiesInput, String wordsInput, int limit)
			throws IOException {
		System.out.println("Using model " + modelInput);
		Word2Vec model = WordVectorSerializer.loadFullModel(modelInput);
		List<String> entities = FileUtils.readLines(new File(entitiesInput), StandardCharsets.UTF_8.name());
		List<String> words = FileUtils.readLines(new File(wordsInput), StandardCharsets.UTF_8.name());
		HashMap<String, HashMap<String, Double>> entitiesMap = new HashMap<>();

		for (String entity : entities) {
			HashMap<String, Double> similarities = new HashMap<>();

			if (model.hasWord(entity)) {
				for (String word : words) {
					if (model.hasWord(word)) {
						double similiarity = model.similarity(entity, word);
						similarities.put(word, Math.abs(similiarity));
					}
				}

				entitiesMap.put(entity, similarities);
			}
		}

		for (String key : entitiesMap.keySet()) {
			System.out.println("Entity " + key);
			HashMap<String, Double> similarities = entitiesMap.get(key);
			similarities.entrySet().stream().sorted(Map.Entry.<String, Double> comparingByValue().reversed())
					.limit(limit).forEach(System.out::println);
			System.out.println("=============");
		}
	}

	/**
	 * Print similarities from a list of entities to good and bad words.
	 * 
	 * @param input
	 * @param goodWords
	 * @param badWords
	 * @param entities
	 * @throws IOException
	 */
	public void printPositiveAndNegativeAssociations(String input, List<String> goodWords, List<String> badWords,
			List<String> entities) throws IOException {
		Word2Vec vec = WordVectorSerializer.loadFullModel(input);

		for (String entity : entities) {
			System.out.println("Testing entity " + entity);
			for (String good : goodWords) {
				if (vec.hasWord(good)) {
					double similiarity = vec.similarity(entity, good);
					System.out.println(good + "\t" + String.valueOf(Math.abs(similiarity)).replace(".", ","));
				}
			}

			for (String bad : badWords) {
				if (vec.hasWord(bad)) {
					double similiarity = vec.similarity(entity, bad);
					System.out.println(bad + "\t" + String.valueOf(Math.abs(similiarity)).replace(".", ","));
				}
			}
		}

	}

	/**
	 * An utility method for investigating which presentations contains specific
	 * words.
	 * 
	 * @param entity
	 * @param word
	 * @throws IOException
	 */
	public void findTalksWithWords(String entity, String word) throws IOException {
		DataProvider reader = new DataProvider();		
		List<Presentation> presentations = reader.getAll(DataFilter.EXCLUDES);
		System.out.println("Finding talks with entity " + entity + " and word " + word);

		for (Presentation presentation : presentations) {
			StringBuffer buffer = new StringBuffer();

			String summary = StringUtils.trim(presentation.getSummary());
			String description = StringUtils.trim(presentation.getDescription());
			if (!StringUtils.isEmpty(summary))
				buffer = buffer.append(summary).append("\n");
			if (!StringUtils.isEmpty(description))
				buffer = buffer.append(description).append("\n");
			String bios = "";
			for (Speaker speaker : presentation.getSpeakers()) {
				String bio = StringUtils.trim(speaker.getBiography());
				if (!StringUtils.isEmpty(bio))
					bios += bio + "\n";
			}

			if (!StringUtils.isEmpty(bios))
				buffer = buffer.append(bios);

			String content = buffer.toString().toLowerCase();

			if (StringUtils.contains(content, entity) && StringUtils.contains(content, word)) {
				System.out
						.println("Presentation (" + presentation.getYear() + ") has words: " + presentation.getTitle());
			}
		}

	}

	/**
	 * Dump a VSM to csv file.
	 * 
	 * @param input
	 * @param output
	 * @param include
	 *            if not null, only these words will be dumped.
	 * @throws Exception
	 */
	public void dumpModelToCsv(String input, String output, String include) throws Exception {
		Word2Vec vec = WordVectorSerializer.loadFullModel(input);

		HashSet<String> includeWords = new HashSet<>();
		List<String> includeWordsTmp = FileUtils.readLines(new File(include), StandardCharsets.UTF_8.name());
		for (String word : includeWordsTmp) {
			includeWords.add(StringUtils.trim(word));
		}

		StringBuffer buffer = new StringBuffer();

		// create header
		for (int i = 0; i < 300; i++) {
			buffer = buffer.append("col" + i).append(",");
		}
		buffer = new StringBuffer(StringUtils.substringBeforeLast(buffer.toString(), ",")).append("\n");

		// dump each word
		for (String word : vec.getVocab().words()) {

			if (!includeWordsTmp.isEmpty() && !includeWords.contains(word))
				continue;

			if (!StringUtils.isAlpha(word))
				continue;

			String row = "";

			double[] vector = vec.getWordVector(word);

			row = "\"" + word + "\",";
			for (int i = 0; i < 300; i++) {
				row = row + vector[i] + ",";
			}
			row = StringUtils.substringBeforeLast(row, ",");
			buffer = buffer.append(row).append("\n");
		}

		IOUtils.writeStringToFileNoExceptions(buffer.toString(), output, StandardCharsets.UTF_8.name());
	}

	/**
	 * Generates a tsne plot (2d view of a higher dimension). However, this does
	 * not work well enough to be used, so we have to resort to R
	 * implementation.
	 * 
	 * @param input
	 * @param output
	 * @throws Exception
	 */
	public void generateVisualization(String input, String output) throws Exception {
		Word2Vec vec = WordVectorSerializer.loadFullModel(input);

		BarnesHutTsne tsne = new BarnesHutTsne.Builder().setMaxIter(1000).stopLyingIteration(250).learningRate(500)
				.useAdaGrad(false).theta(0.5).setMomentum(0.5).normalize(true).usePca(false).build();

		vec.lookupTable().plotVocab(tsne, 50, new File(output));

		UiServer server = UiServer.getInstance();
		System.out.println("Started on port " + server.getPort());
	}

	/**
	 * This creates a VSM after converting input to lowercase.
	 * 
	 * @param input
	 * @param output
	 * @throws IOException
	 */
	public void createWord2VecModel(String input, String output) throws IOException {
		SentenceIterator sentIt = new LineSentenceIterator(new File(input));
		sentIt.setPreProcessor(new SentencePreProcessor() {
			@Override
			public String preProcess(String sentence) {
				return sentence.toLowerCase();
			}
		});

		TokenizerFactory tokenizer = new DefaultTokenizerFactory();
		tokenizer.setTokenPreProcessor(new CommonPreprocessor());

		Word2Vec vec = new Word2Vec.Builder().elementsLearningAlgorithm(new CBOW<VocabWord>()).minWordFrequency(5)
				.negativeSample(5).epochs(5).batchSize(1000).iterations(30).layerSize(300).seed(42).windowSize(5)
				.iterate(sentIt).tokenizerFactory(tokenizer).build();
		vec.fit();

		WordVectorSerializer.writeFullModel(vec, output);
	}

	/**
	 * This create a VSM after applying lemmatizing.
	 * 
	 * @param input
	 * @param output
	 * @throws IOException
	 */
	public void createWord2VecModelWithLemma(String input, String output) throws IOException {
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

		SentenceIterator sentIt = new LineSentenceIterator(new File(input));
		sentIt.setPreProcessor(new SentencePreProcessor() {
			@Override
			public String preProcess(String input) {
				Annotation annotation = pipeline.process(input);
				List<CoreMap> annotedSentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
				String words = "";

				for (CoreMap sentence : annotedSentences) {
					for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
						String lemma = token.get(LemmaAnnotation.class);
						words = words + " " + lemma.toLowerCase();
					}
				}

				return StringUtils.trim(words);
			}
		});

		TokenizerFactory tokenizer = new DefaultTokenizerFactory();
		tokenizer.setTokenPreProcessor(new CommonPreprocessor());

		Word2Vec vec = new Word2Vec.Builder().elementsLearningAlgorithm(new CBOW<VocabWord>()).minWordFrequency(5)
				.negativeSample(5).epochs(5).batchSize(1000).iterations(30).layerSize(300).seed(42).windowSize(5)
				.iterate(sentIt).tokenizerFactory(tokenizer).build();
		vec.fit();

		WordVectorSerializer.writeFullModel(vec, output);
	}
}

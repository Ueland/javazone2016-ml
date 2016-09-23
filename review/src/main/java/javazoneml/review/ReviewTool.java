package javazoneml.review;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameter;
import edu.berkeley.compbio.jlibsvm.ImmutableSvmParameterGrid;
import edu.berkeley.compbio.jlibsvm.binary.BinaryModel;
import edu.berkeley.compbio.jlibsvm.binary.C_SVC;
import edu.berkeley.compbio.jlibsvm.binary.MutableBinaryClassificationProblemImpl;
import edu.berkeley.compbio.jlibsvm.kernel.LinearKernel;
import edu.berkeley.compbio.jlibsvm.util.SparseVector;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import javazoneml.model.Presentation;
import javazoneml.tools.data.DataFilter;
import javazoneml.tools.data.DataProvider;
import javazoneml.tools.preprocessing.StopWords;

public class ReviewTool {

	public static void main(String[] args) throws IOException {
		new ReviewTool().runReviewAnalyze();
	}

	public void runReviewAnalyze() {
		// first fetch all data
		List<Presentation> presentations = getPresentations();
		List<String> presentationTitles = presentations.stream().map(p -> p.getTitle()).collect(Collectors.toList());
		List<String> otherTitles = getOtherTitles("src/main/resources/data/other_titles.txt");
		List<String> javazone2016Titles = getJavazoneTitles("src/main/resources/data/javazone-2016-lighttalks.txt");
		List<String> javazone2017Titles = getJavazoneTitles("src/main/resources/data/javazone-2017.txt");

		System.out.println("Fetched " + presentationTitles.size() + " presentation titles");
		System.out.println("Fetched " + otherTitles.size() + " other titles");
		System.out.println("Fetched " + javazone2016Titles.size() + " 2016 light titles");
		System.out.println("Fetched " + javazone2017Titles.size() + " 2017 generated titles");

		// extract bow features
		List<SparseVector> features1 = extractBoWFeactures(presentationTitles);
		List<SparseVector> features2 = extractBoWFeactures(otherTitles);
		List<SparseVector> features3 = extractBoWFeactures(javazone2016Titles);
		List<SparseVector> features4 = extractBoWFeactures(javazone2017Titles);

		// train a model on the features
		BinaryModel model = doSVM(features1, features2);

		// try to do prediction based on the trained model
		predictApproval(features3, model);
		predictApproval(features4, model);
	}

	public List<SparseVector> extractBoWFeactures(List<String> titles) {

		List<String> enStopWords = new ArrayList<>();
		enStopWords.addAll(StopWords.EN);
		enStopWords.addAll(StopWords.EN_SPECIFIC);

		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

		// extract features per title
		Bag<String> bow = new HashBag<>();
		Map<String, Bag<String>> bowTitles = new HashMap<>();
		for (String title : titles) {
			HashBag<String> bag = extractFeatures(pipeline, title);
			bag = removeStopWords(bag, enStopWords);
			bowTitles.put(title, bag);
			bow.addAll(bag);
		}

		// create a shared feature space
		List<SparseVector> features = new LinkedList<>();
		for (String title : bowTitles.keySet()) {

			int i = 0;
			float[] floats = new float[bow.size()];
			for (String label : bow.uniqueSet()) {
				int count = bowTitles.get(title).getCount(label);
				floats[i] = count;
				i++;
			}

			features.add(generateFeatures(floats));
		}

		System.out.println("Dimension of features is " + bow.size() + " x " + features.size());

		return features;
	}

	public void predictApproval(List<SparseVector> features, BinaryModel model) {
		System.out.println("Validation on javazone set: " + features.size());
		int correct = 0;
		for (SparseVector feature : features) {
			boolean result = (boolean) model.predictLabel(feature);
			if (result)
				correct++;
		}

		System.out.println("Correct: " + correct);
		System.out.println("Size: " + features.size());
		System.out.println("Correct %:" + (float) ((float) correct / (float) features.size()));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public BinaryModel doSVM(List<SparseVector> features1, List<SparseVector> features2) {
		C_SVC svm = new C_SVC();
		ImmutableSvmParameterGrid.Builder builder = ImmutableSvmParameterGrid.builder();

		HashSet<Float> cSet;
		HashSet<LinearKernel> kernelSet;

		cSet = new HashSet<Float>();
		cSet.add(1.0f);

		kernelSet = new HashSet<LinearKernel>();
		kernelSet.add(new LinearKernel());

		try {
			Class s = Class.forName("edu.berkeley.compbio.jlibsvm.kernel.LinearKernel");
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}

		builder.eps = 0.001f; // epsilon
		builder.Cset = cSet; // C values used
		builder.kernelSet = kernelSet; // Kernel used

		ImmutableSvmParameter params = builder.build();

		MutableBinaryClassificationProblemImpl<Boolean, SparseVector> problem = new MutableBinaryClassificationProblemImpl<>(
				String.class, 2);

		Map<SparseVector, Boolean> training_features = new HashMap<>();
		Map<SparseVector, Boolean> test_features = new HashMap<>();

		float split_percentage = 0.7f;

		features1.subList(0, (int) (features1.size() * split_percentage)).stream()
				.forEach(p -> training_features.put(p, true));
		features2.subList(0, (int) (features2.size() * split_percentage)).stream()
				.forEach(p -> training_features.put(p, false));

		features1.subList((int) (features1.size() * split_percentage), features1.size()).stream()
				.forEach(p -> test_features.put(p, true));
		features2.subList((int) (features2.size() * split_percentage), features2.size()).stream()
				.forEach(p -> test_features.put(p, false));

		if (training_features.size() + test_features.size() != features1.size() + features2.size()) {
			System.out.println(training_features.size());
			System.out.println(test_features.size());
			System.out.println(features1.size());
			System.out.println(features2.size());
			throw new RuntimeException("Not same size of training and test as features");
		}

		for (SparseVector feature : training_features.keySet()) {
			boolean expected = training_features.get(feature);
			problem.addExample(feature, expected);
		}

		BinaryModel model = svm.train(problem, params);

		int correct = 0;
		for (SparseVector feature : test_features.keySet()) {
			boolean expected = test_features.get(feature);
			boolean result = (boolean) model.predictLabel(feature);
			if (expected == result)
				correct++;
		}

		System.out.println("Correct: " + correct);
		System.out.println("Size: " + test_features.size());
		System.out.println("Correct %:" + (float) ((float) correct / (float) test_features.size()));

		return model;
	}

	private SparseVector generateFeatures(float[] floats) {
		SparseVector sparseVector = new SparseVector(floats.length);
		int[] indices = new int[floats.length];
		for (int i = 0; i < floats.length; i++) {
			indices[i] = new Integer(i);
		}
		sparseVector.indexes = indices;
		sparseVector.values = floats;
		return sparseVector;
	}

	private HashBag<String> removeStopWords(HashBag<String> bag, List<String> stopWords) {
		// System.out.println("Bag size before trimming: " + bag.size());
		Set<String> filteredBag = bag.stream().filter(p -> !stopWords.contains(p)).collect(Collectors.toSet());
		HashBag<String> newBag = new HashBag<>(filteredBag);
		// System.out.println("Bag size after trimming: " + newBag.size());

		return newBag;
	}

	private HashBag<String> extractFeatures(StanfordCoreNLP pipeline, String input) {
		HashBag<String> bag = new HashBag<>();

		if (StringUtils.isEmpty(input))
			return bag;

		Annotation annotation = pipeline.process(input);
		List<CoreMap> annotedSentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);

		for (CoreMap sentence : annotedSentences) {
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				String lemma = token.get(LemmaAnnotation.class);
				bag.add(lemma.toLowerCase());
			}
		}

		return bag;
	}

	private List<Presentation> getPresentations() {
		DataProvider reader = new DataProvider();
		return reader.getAll(DataFilter.EXCLUDES);
	}

	// extract titles from
	// https://github.com/mgree/tmpl/blob/master/www/backend/abstracts/docs.dat
	public List<String> getOtherTitles(String file) {
		try {
			List<String> titles = FileUtils.readLines(new File(file), StandardCharsets.UTF_8.name());
			titles = titles.stream().map(p -> StringUtils.substringBefore(p, " - ").toLowerCase())
					.collect(Collectors.toList());

			// titles.stream().forEach(p -> System.out.println(p));

			return titles;
		} catch (IOException e) {
			return Collections.emptyList();
		}
	}

	public List<String> getJavazoneTitles(String file) {
		try {
			return FileUtils.readLines(new File(file), StandardCharsets.UTF_8.name());
		} catch (IOException e) {
			return Collections.emptyList();
		}
	}
}

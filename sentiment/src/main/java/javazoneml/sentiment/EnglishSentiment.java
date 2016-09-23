package javazoneml.sentiment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import javazoneml.model.Presentation;
import javazoneml.model.Speaker;
import javazoneml.tools.data.DataFilter;
import javazoneml.tools.data.DataProvider;

public class EnglishSentiment {

	public static void main(String[] args) throws IOException {
		// new EnglishSentiment().dumpPresentationSentimentAnalysis("en");
		// new EnglishSentiment().dumpBioSentimentAnalysis("en");
		// new
		// EnglishSentiment().printSentimentStats("src/main/resources/data/sentiment-en-all.txt",
		// "en");
		// new
		// EnglishSentiment().printSentimentStats("src/main/resources/data/sentiment-bio-en-all.txt",
		// "en");
		new EnglishSentiment().lemmatizeText("src/main/resources/data/javazone-en-all.txt",
				"src/main/resources/data/javazone-en-all.lemmatized.txt");
	}

	public void lemmatizeText(String input, String output) throws IOException {

		List<String> lines = FileUtils.readLines(new File(input), StandardCharsets.UTF_8);
		String buffer = lemmatizeText(lines);
		IOUtils.writeStringToFileNoExceptions(buffer, output, StandardCharsets.UTF_8.name());
	}

	public void dumpPresentationSentimentAnalysis(String language) {
		DataProvider reader = new DataProvider();		
		List<Presentation> presentations = reader.getAll(DataFilter.EXCLUDES);

		presentations = presentations.stream().filter(p -> language.equalsIgnoreCase(p.getLanguage()))
				.collect(Collectors.toList());

		HashMap<Presentation, String> presentationSentiment = new HashMap<>();

		System.out.println("Processing " + presentations.size() + " presentations");
		int counter = 0;
		for (Presentation presentation : presentations) {
			HashMap<SENTIMENT, Integer> result = analyseSentiment(getPresentationContent(presentation));

			String sentiment = String.format("%s:%s:%s:%s:%s", result.get(SENTIMENT.VERY_NEGATIVE),
					result.get(SENTIMENT.NEGATIVE), result.get(SENTIMENT.NEUTRAL), result.get(SENTIMENT.POSITIVE),
					result.get(SENTIMENT.VERY_POSITIVE));
			presentationSentiment.put(presentation, sentiment);

			if (++counter % 10 == 0)
				System.out.println(counter + ".. done");
		}

		Path path = Paths.get("src/main/resources/data/sentiment-en-all.txt");
		try (BufferedWriter writer = Files.newBufferedWriter(path)) {
			writer.write("very negative:negative:neutral:positive:very positive, year, title");
			for (Presentation presentation : presentations) {
				writer.write(String.format("%s, %s, %s \n", presentationSentiment.get(presentation),
						presentation.getYear(), presentation.getTitle()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Sentiment scores written to " + path);
	}

	public void dumpBioSentimentAnalysis(String language) {
		DataProvider reader = new DataProvider();		
		List<Presentation> presentations = reader.getAll(DataFilter.EXCLUDES);

		presentations = presentations.stream().filter(p -> language.equalsIgnoreCase(p.getLanguage()))
				.collect(Collectors.toList());

		HashMap<Presentation, String> presentationSentiment = new HashMap<>();

		System.out.println("Processing " + presentations.size() + " presentations");
		int counter = 0;
		for (Presentation presentation : presentations) {
			HashMap<SENTIMENT, Integer> result = analyseSentiment(getBioContent(presentation));

			String sentiment = String.format("%s:%s:%s:%s:%s", result.get(SENTIMENT.VERY_NEGATIVE),
					result.get(SENTIMENT.NEGATIVE), result.get(SENTIMENT.NEUTRAL), result.get(SENTIMENT.POSITIVE),
					result.get(SENTIMENT.VERY_POSITIVE));
			presentationSentiment.put(presentation, sentiment);

			if (++counter % 10 == 0)
				System.out.println(counter + ".. done");
		}

		Path path = Paths.get("src/main/resources/data/sentiment-bio-en-all.txt");
		try (BufferedWriter writer = Files.newBufferedWriter(path)) {
			writer.write("very negative:negative:neutral:positive:very positive, year, title");
			for (Presentation presentation : presentations) {
				writer.write(String.format("%s, %s, %s \n", presentationSentiment.get(presentation),
						presentation.getYear(), presentation.getTitle()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Sentiment bio scores written to " + path);
	}

	public void printSentimentStats(String file, String language) {

		HashMap<Presentation, SENTIMENT> scores = getScores(file, language);

		HashMap<String, Long> stats = new HashMap<>();
		for (Presentation presentation : scores.keySet()) {

			SENTIMENT score = scores.get(presentation);

			stats.compute(score.toString() + " (" + presentation.getYear() + ")", (k, v) -> v == null ? 1 : v + 1);
			stats.compute(score.toString() + " (all)", (k, v) -> v == null ? 1 : v + 1);
		}

		System.out.println("=== Sentiment stats for " + file + " ===");
		stats.entrySet().stream().sorted(Map.Entry.<String, Long> comparingByKey()).forEach(System.out::println);
		System.out.println("---------------------");
	}

	private HashMap<Presentation, SENTIMENT> getScores(String file, String language) {
		DataProvider reader = new DataProvider();		
		List<Presentation> presentations = reader.getAll(DataFilter.EXCLUDES);

		presentations = presentations.stream().filter(p -> language.equalsIgnoreCase(p.getLanguage()))
				.collect(Collectors.toList());

		List<String> sentimentList = new ArrayList<>();
		Path path = Paths.get(file);
		try (Stream<String> stream = Files.lines(path)) {
			sentimentList = stream.filter(line -> !line.startsWith("very negative") && !line.isEmpty())
					.collect(Collectors.toList());
		} catch (IOException e) {
			e.printStackTrace();
		}

		HashMap<Presentation, SENTIMENT> scores = new HashMap<>();
		System.out.println("Scoring " + presentations.size() + " presentations");
		for (Presentation presentation : presentations) {

			// find sentiment score
			SENTIMENT score = null;
			for (String line : sentimentList) {
				String sentiment = StringUtils.substringBefore(line, ", ").trim();
				int year = Integer.valueOf(StringUtils.substringBetween(line, ", ", ", ").trim());
				String title = StringUtils.substringAfter(line, year + ", ").trim();

				if (presentation.getYear() == year
						&& presentation.getTitle().replace("\n", " ").trim().equalsIgnoreCase(title)) {
					score = getTotalSentimentScore(0, sentiment);
					break;
				}
			}

			if (score == null)
				throw new IllegalArgumentException("Could not find sentiment for presentation "
						+ presentation.getTitle() + " in year " + presentation.getYear());

			scores.put(presentation, score);
		}

		return scores;
	}

	SENTIMENT getTotalSentimentScore(int scoreThreshold, String input) {

		double very_negative = Double.valueOf(input.split(":")[0]);
		double negative = Double.valueOf(input.split(":")[1]);
		double neutral = Double.valueOf(input.split(":")[2]);
		double positive = Double.valueOf(input.split(":")[3]);
		double very_positive = Double.valueOf(input.split(":")[4]);

		double sentimentNegative = Math.pow(very_negative, 2) + negative;
		double sentimentNeutral = neutral;
		double sentimentPositive = Math.pow(very_positive, 2) + positive;

		double sentimentScore = -sentimentNegative + sentimentPositive;

		if (sentimentScore < -scoreThreshold && (sentimentNegative > sentimentPositive))
			return SENTIMENT.NEGATIVE;
		else if (sentimentScore > scoreThreshold && (sentimentPositive > sentimentNeutral))
			return SENTIMENT.POSITIVE;
		else
			return SENTIMENT.NEUTRAL;
	}

	String lemmatizeText(List<String> lines) {
		StringBuffer buffer = new StringBuffer();

		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

		for (String line : lines) {
			Annotation annotation = pipeline.process(line.toLowerCase());
			List<CoreMap> annotedSentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);

			for (CoreMap sentence : annotedSentences) {
				for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
					buffer = buffer.append(token.get(LemmaAnnotation.class)).append(" ");
				}
			}

			buffer = buffer.append("\n");
		}

		return StringUtils.trim(buffer.toString());
	}

	HashMap<SENTIMENT, Integer> analyseSentiment(String text) {
		HashMap<SENTIMENT, Integer> results = new HashMap<>();
		results.put(SENTIMENT.VERY_NEGATIVE, 0);
		results.put(SENTIMENT.NEGATIVE, 0);
		results.put(SENTIMENT.NEUTRAL, 0);
		results.put(SENTIMENT.POSITIVE, 0);
		results.put(SENTIMENT.VERY_POSITIVE, 0);

		// Reader reader = new StringReader(text);
		// DocumentPreprocessor dp = new DocumentPreprocessor(reader);
		// List<String> sentenceList = new ArrayList<String>();
		//
		// for (List<HasWord> sentence : dp) {
		// String sentenceString = Sentence.listToString(sentence);
		// sentenceList.add(sentenceString.toString());
		// }

		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse, sentiment");
		props.setProperty("parse.maxlen", "50");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

		System.out.print("Gonna process " + text.length() + " chunks of text");

		Annotation annotation = pipeline.process(text);
		List<CoreMap> annotedSentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);

		for (CoreMap annoted : annotedSentences) {
			String sentiment = annoted.get(SentimentCoreAnnotations.SentimentClass.class);
			results.compute(SENTIMENT.value(sentiment), (k, v) -> v == null ? 1 : v + 1);
		}

		System.out.println("... done");

		return results;
	}

	private String getPresentationContent(Presentation presentation) {
		return presentation.getDescription() + ". " + presentation.getDescription();
	}

	private String getBioContent(Presentation presentation) {
		StringBuffer buffer = new StringBuffer();
		for (Speaker speaker : presentation.getSpeakers()) {
			if (!StringUtils.isEmpty(speaker.getBiography()))
				buffer.append(speaker.getBiography()).append(". ");
		}

		return buffer.toString();
	}
}

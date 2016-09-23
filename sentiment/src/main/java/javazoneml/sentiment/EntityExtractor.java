package javazoneml.sentiment;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class EntityExtractor {
	public static void main(String[] args) throws IOException {
		new EntityExtractor().findEntities("src/main/resources/data/javazone-en-talk.txt",
				"src/main/resources/data/javazone-en-talk.entities.txt");

		new EntityExtractor().findEntities("src/main/resources/data/javazone-en-bio.txt",
				"src/main/resources/data/javazone-en-bio.entities.txt");
	}

	public void findEntities(String input, String output) throws IOException {
		List<String> lines = FileUtils.readLines(new File(input), StandardCharsets.UTF_8);
		String buffer = extractEntities(lines);
		IOUtils.writeStringToFileNoExceptions(buffer, output, StandardCharsets.UTF_8.name());
	}

	String extractEntities(List<String> lines) {
		StringBuffer buffer = new StringBuffer();

		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, regexner");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

		for (String line : lines) {
			if (StringUtils.isEmpty(line))
				continue;

			Annotation annotation = pipeline.process(line);
			List<CoreMap> annotedSentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);

			for (CoreMap sentence : annotedSentences) {
				boolean inEntity = false;
				String entity = "";
				String entityType = "";
				for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
					String type = token.get(NamedEntityTagAnnotation.class);
					if ("O".equals(type)) {
						if (inEntity) {
							buffer = buffer.append(entityType).append(":").append(StringUtils.trim(entity))
									.append("\n");
							System.out.println("Found entity (" + entityType + "): " + entity);
						}
						entity = "";
						entityType = "";
						inEntity = false;
					} else if ("PERSON".equalsIgnoreCase(type) || "ORGANIZATION".equalsIgnoreCase(type)
							|| "LOCATION".equalsIgnoreCase(type) || "MISC".equalsIgnoreCase(type)) {
						inEntity = true;
						entityType = type;
						entity = entity + " " + token.get(TextAnnotation.class);
					} else {
						// System.out.println("Ignoring (" + type + "): " +
						// token.get(TextAnnotation.class));
					}
				}
			}
		}

		return buffer.toString();
	}
}

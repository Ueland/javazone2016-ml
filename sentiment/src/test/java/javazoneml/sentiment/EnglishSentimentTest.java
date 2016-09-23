package javazoneml.sentiment;

import java.util.Collections;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

public class EnglishSentimentTest extends EnglishSentiment {

	@Test
	public void testLemmatizeText() {
		String result = lemmatizeText(Collections.singletonList("How could you be seeing into my eyes like open doors?"));
		
		Assert.assertEquals("how could you be see into my eye like open door ?", result);							 
	}
	
	@Test
	public void testText() {
		HashMap<SENTIMENT, Integer> result1 = analyseSentiment("So you think you know Java?");
		HashMap<SENTIMENT, Integer> result2 = analyseSentiment("This talk will help you answer that question.");
		HashMap<SENTIMENT, Integer> result3 = analyseSentiment(
				"Statistics show that real-life experience in Java programming alone is not sufficient for passing the certification exam.");
		HashMap<SENTIMENT, Integer> result4 = analyseSentiment(
				"The exam requires in depth knowledge of features and usage of the Java programming language.");
		HashMap<SENTIMENT, Integer> result5 = analyseSentiment(
				"The talk provides a roadmap for achieving the right level of proficiency demanded by the certification exam.");

		HashMap<SENTIMENT, Integer> result = analyseSentiment(
				"So you think you know Java? This talk will help you answer that question. Statistics show that real-life experience in Java programming alone is not sufficient for passing the certification exam. The exam requires in depth knowledge of features and usage of the Java programming language. The talk provides a roadmap for achieving the right level of proficiency demanded by the certification exam.");

		Assert.assertEquals(result1.get(SENTIMENT.VERY_NEGATIVE) + result2.get(SENTIMENT.VERY_NEGATIVE)
				+ result3.get(SENTIMENT.VERY_NEGATIVE) + result4.get(SENTIMENT.VERY_NEGATIVE)
				+ result5.get(SENTIMENT.VERY_NEGATIVE), result.get(SENTIMENT.VERY_NEGATIVE), 0);
		Assert.assertEquals(
				result1.get(SENTIMENT.NEGATIVE) + result2.get(SENTIMENT.NEGATIVE) + result3.get(SENTIMENT.NEGATIVE)
						+ result4.get(SENTIMENT.NEGATIVE) + result5.get(SENTIMENT.NEGATIVE),
				result.get(SENTIMENT.NEGATIVE), 0);
		Assert.assertEquals(
				result1.get(SENTIMENT.NEUTRAL) + result2.get(SENTIMENT.NEUTRAL) + result3.get(SENTIMENT.NEUTRAL)
						+ result4.get(SENTIMENT.NEUTRAL) + result5.get(SENTIMENT.NEUTRAL),
				result.get(SENTIMENT.NEUTRAL), 0);
		Assert.assertEquals(
				result1.get(SENTIMENT.POSITIVE) + result2.get(SENTIMENT.POSITIVE) + result3.get(SENTIMENT.POSITIVE)
						+ result4.get(SENTIMENT.POSITIVE) + result5.get(SENTIMENT.POSITIVE),
				result.get(SENTIMENT.POSITIVE), 0);
		Assert.assertEquals(result1.get(SENTIMENT.VERY_POSITIVE) + result2.get(SENTIMENT.VERY_POSITIVE)
				+ result3.get(SENTIMENT.VERY_POSITIVE) + result4.get(SENTIMENT.VERY_POSITIVE)
				+ result5.get(SENTIMENT.VERY_POSITIVE), result.get(SENTIMENT.VERY_POSITIVE), 0);
	}

	@Test
	public void testVeryNegative() {
		HashMap<SENTIMENT, Integer> result = analyseSentiment("This is a very disgusting and shitty sentence!");

		Assert.assertEquals(5, result.size());
		Assert.assertEquals(1, result.get(SENTIMENT.VERY_NEGATIVE), 0);
		Assert.assertEquals(0, result.get(SENTIMENT.NEGATIVE), 0);
		Assert.assertEquals(0, result.get(SENTIMENT.NEUTRAL), 0);
		Assert.assertEquals(0, result.get(SENTIMENT.POSITIVE), 0);
		Assert.assertEquals(0, result.get(SENTIMENT.VERY_POSITIVE), 0);
	}

	@Test
	public void testNegative() {
		HashMap<SENTIMENT, Integer> result = analyseSentiment("This is a very bad sentence");

		Assert.assertEquals(5, result.size());
		Assert.assertEquals(0, result.get(SENTIMENT.VERY_NEGATIVE), 0);
		Assert.assertEquals(1, result.get(SENTIMENT.NEGATIVE), 0);
		Assert.assertEquals(0, result.get(SENTIMENT.NEUTRAL), 0);
		Assert.assertEquals(0, result.get(SENTIMENT.POSITIVE), 0);
		Assert.assertEquals(0, result.get(SENTIMENT.VERY_POSITIVE), 0);
	}

	@Test
	public void testNeutral() {
		HashMap<SENTIMENT, Integer> result = analyseSentiment("This is a very normal sentence");

		Assert.assertEquals(5, result.size());
		Assert.assertEquals(0, result.get(SENTIMENT.VERY_NEGATIVE), 0);
		Assert.assertEquals(0, result.get(SENTIMENT.NEGATIVE), 0);
		Assert.assertEquals(1, result.get(SENTIMENT.NEUTRAL), 0);
		Assert.assertEquals(0, result.get(SENTIMENT.POSITIVE), 0);
		Assert.assertEquals(0, result.get(SENTIMENT.VERY_POSITIVE), 0);
	}

	@Test
	public void testPositive() {
		HashMap<SENTIMENT, Integer> result = analyseSentiment("This is a very happy sentence");

		Assert.assertEquals(5, result.size());
		Assert.assertEquals(0, result.get(SENTIMENT.VERY_NEGATIVE), 0);
		Assert.assertEquals(0, result.get(SENTIMENT.NEGATIVE), 0);
		Assert.assertEquals(0, result.get(SENTIMENT.NEUTRAL), 0);
		Assert.assertEquals(1, result.get(SENTIMENT.POSITIVE), 0);
		Assert.assertEquals(0, result.get(SENTIMENT.VERY_POSITIVE), 0);
	}

	@Test
	public void testVeryPositive() {
		HashMap<SENTIMENT, Integer> result = analyseSentiment("This is a very happy sentence!");

		Assert.assertEquals(5, result.size());
		Assert.assertEquals(0, result.get(SENTIMENT.VERY_NEGATIVE), 0);
		Assert.assertEquals(0, result.get(SENTIMENT.NEGATIVE), 0);
		Assert.assertEquals(0, result.get(SENTIMENT.NEUTRAL), 0);
		Assert.assertEquals(0, result.get(SENTIMENT.POSITIVE), 0);
		Assert.assertEquals(1, result.get(SENTIMENT.VERY_POSITIVE), 0);
	}

	@Test
	public void testNegativeScore() {
		SENTIMENT score = getTotalSentimentScore(0, "0:2:0:0:1");

		Assert.assertEquals(SENTIMENT.NEGATIVE, score);
	}

	@Test
	public void testNegativeScore2() {
		SENTIMENT score = getTotalSentimentScore(0, "0:10:11:4:0");

		Assert.assertEquals(SENTIMENT.NEGATIVE, score);
	}

	@Test
	public void testPositiveScore() {
		SENTIMENT score = getTotalSentimentScore(0, "0:2:0:0:3");

		Assert.assertEquals(SENTIMENT.POSITIVE, score);
	}

	@Test
	public void testNeutralScore() {
		SENTIMENT score = getTotalSentimentScore(0, "2:0:5:0:2");

		Assert.assertEquals(SENTIMENT.NEUTRAL, score);
	}

	@Test
	public void testNegativeScoreWithThreshold() {
		SENTIMENT score = getTotalSentimentScore(5, "0:2:0:0:1");

		Assert.assertEquals(SENTIMENT.NEUTRAL, score);
	}

	@Test
	public void testNegativeScoreWithThreshold2() {
		SENTIMENT score = getTotalSentimentScore(5, "2:3:0:0:1");

		Assert.assertEquals(SENTIMENT.NEGATIVE, score);
	}

	@Test
	public void testPositiveScoreWithThreshold() {
		SENTIMENT score = getTotalSentimentScore(5, "1:0:0:2:0");

		Assert.assertEquals(SENTIMENT.NEUTRAL, score);
	}

	@Test
	public void testPositiveScoreWithThreshold2() {
		SENTIMENT score = getTotalSentimentScore(5, "1:0:0:2:3");

		Assert.assertEquals(SENTIMENT.POSITIVE, score);
	}

}

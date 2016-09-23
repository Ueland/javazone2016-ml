package javazoneml.sentiment;

import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class EntityExtractorTest extends EntityExtractor {
	
	@Test
	public void testExtractEntities() {
		List<String> lines = new LinkedList<>();
		lines.add("I read about Microsoft in the newspaper today from an article by Bill Gates.");
		lines.add("I complained to Microsoft about Bill Gates.");		
		lines.add("President Barack Obama met Fidel Castro at the United Nations in New York.");
		lines.add("I love to work with Java.");
		String result = extractEntities(lines);
		
		Assert.assertEquals("ORGANIZATION:Microsoft\nPERSON:Bill Gates\nORGANIZATION:Microsoft\nPERSON:Bill Gates\nPERSON:Barack Obama\nPERSON:Fidel Castro\nORGANIZATION:United Nations\nLOCATION:New York\nMISC:Java\n", result);							 
	}

}

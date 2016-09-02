package javazoneml.tools.data;

import javazoneml.model.Presentation;
import javazoneml.model.Speaker;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class DataProviderTest extends DataProvider{
    private Presentation presentation1, presentation2, presentation3;

    @Before
    public void setUp(){
        presentation1 = new Presentation();
        presentation1.setLanguage("no");
        presentation1.setTitle("Java er kult!");
        List<Speaker> speakers1 = new ArrayList<>();
        Speaker speaker1 = new Speaker();
        speaker1.setName("Jens Jensen");
        speakers1.add(speaker1);
        presentation1.setSpeakers(speakers1);

        presentation2 = new Presentation();
        presentation2.setLanguage("no");
        presentation2.setTitle("Spring er det beste rammeverket");
        List<Speaker> speakers2 = new ArrayList<>();
        Speaker speaker2 = new Speaker();
        speaker2.setName("Ola Olsen");
        speakers2.add(speaker2);
        presentation2.setSpeakers(speakers2);

        presentation3 = new Presentation();
        presentation3.setLanguage("en");
        presentation3.setTitle("I Love Hibernate");
        List<Speaker> speakers3 = new ArrayList<>();
        Speaker speaker3 = new Speaker();
        speaker3.setName("Lisa Simpson");
        speakers3.add(speaker3);
        presentation3.setSpeakers(speakers3);
    }

    @Test
    public void testFilterOnlyNorwegian()
    {
        List<Presentation> filteredPresentations = getAll(Collections.singletonList(p -> p.getLanguage().equals("en")));
        Assert.assertEquals(2, filteredPresentations.size());
        Assert.assertTrue(filteredPresentations.contains(presentation1));
        Assert.assertTrue(filteredPresentations.contains(presentation2));
    }

    @Test
    public void testFilterOnlyNorwegianAndNameOla(){
        Predicate<Presentation> filterOla = p -> {
            for(Speaker s : p.getSpeakers()){
                if(s.getName().contains("Ola")){
                    return true;
                }
            }
            return false;
        };
        List<Presentation> filteredPresentations = getAll(Arrays.asList(p -> p.getLanguage().equals("en"), filterOla));
        Assert.assertEquals(1, filteredPresentations.size());
        Assert.assertTrue(filteredPresentations.contains(presentation1));
    }


    @Override
    public List<Presentation> getAll(){
        List<Presentation> presentations = new ArrayList<>();
        presentations.add(presentation1);
        presentations.add(presentation2);
        presentations.add(presentation3);
        return presentations;
    }
}

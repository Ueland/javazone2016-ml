package javazoneml.tools.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javazoneml.model.Presentation;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DataProvider {
    private static final String fileNameTemplate = "/data/json/javazone-%d.json";
    private static final List<Integer> years =
            Arrays.asList(2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015);
    private List<Presentation> presentations = new ArrayList<>();

    public List<Presentation> getAll() {
        if(presentations.isEmpty()){
            years.forEach(year -> presentations.addAll(readFile(year)));
        }
        return presentations;
    }

    /**
     * Exludes presentations that pass the given list of predicates
     * @param excludes list of predicates
     * @return presentations which do not pass the given filters
     */
    public List<Presentation> getAll(List<Predicate<Presentation>> excludes) {
        Predicate<Presentation> commonFilter = excludes.stream().reduce(p -> false, Predicate::or).negate();
        return getAll().stream().filter(commonFilter).collect(Collectors.toList());
    }

    public List<Presentation> get(int year) {
        if (years.contains(year)) {
            return readFile(year);
        }
        return new ArrayList<>();
    }

    private List<Presentation> readFile(int year) {
        try {
            String filename = String.format(fileNameTemplate, year);
            InputStream contentStream = this.getClass().getResourceAsStream(filename);
            String content = IOUtils.toString(contentStream, StandardCharsets.UTF_8.name());
            Gson gson = new GsonBuilder().create();
            return Arrays.asList(gson.fromJson(content, Presentation[].class));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

}

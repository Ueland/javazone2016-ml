package javazoneml.tools.data;

import edu.stanford.nlp.io.IOUtils;
import javazoneml.model.Presentation;
import javazoneml.model.Speaker;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class DumpContent {
    public static void main(String[] args) {
        new DumpContent().dumpContent("src/main/resources/data/javazone-en-talk.txt", false, true, Arrays.asList("en"));
        new DumpContent().dumpContent("src/main/resources/data/javazone-en-bio.txt", true, false, Arrays.asList("en"));
        new DumpContent().dumpContent("src/main/resources/data/javazone-en-all.txt", true, true, Arrays.asList("en"));
        new DumpContent().dumpContent("src/main/resources/data/javazone-no-talk.txt", false, true, Arrays.asList("no"));
        new DumpContent().dumpContent("src/main/resources/data/javazone-no-bio.txt", true, false, Arrays.asList("no"));
        new DumpContent().dumpContent("src/main/resources/data/javazone-no-all.txt", true, true, Arrays.asList("no"));
        new DumpContent().dumpContent("src/main/resources/data/javazone-all-talk.txt", false, true,
                Arrays.asList("en", "no"));
        new DumpContent().dumpContent("src/main/resources/data/javazone-all-bio.txt", true, false,
                Arrays.asList("en", "no"));
        new DumpContent().dumpContent("src/main/resources/data/javazone-all-all.txt", true, true,
                Arrays.asList("en", "no"));
    }

    public void dumpContent(String output, boolean dumpBio, boolean dumpPresentation, List<String> languages) {
        DataProvider reader = new DataProvider();
        List<Predicate<Presentation>> excludes = new ArrayList<>();
        excludes.addAll(DataFilter.EXCLUDES);
        excludes.add(p -> (p.getSummary() == null) && (p.getDescription() == null));
        List<Presentation> presentations = reader.getAll(excludes);

        StringBuffer buffer = new StringBuffer();

        for (Presentation presentation : presentations) {
            if (languages.contains(presentation.getLanguage())) {
                if (dumpPresentation) {
                    String summary = StringUtils.trim(presentation.getSummary());
                    String description = StringUtils.trim(presentation.getDescription());

                    if (!StringUtils.isEmpty(summary))
                        buffer = buffer.append(summary).append("\n");

                    if (!StringUtils.isEmpty(description))
                        buffer = buffer.append(description).append("\n");
                }

                if (dumpBio) {
                    String bios = "";
                    for (Speaker speaker : presentation.getSpeakers()) {
                        String bio = StringUtils.trim(speaker.getBiography());
                        if (!StringUtils.isEmpty(bio))
                            bios += bio + "\n";
                    }

                    if (!StringUtils.isEmpty(bios))
                        buffer = buffer.append(bios);
                }

                if (buffer.length() > 0)
                    buffer = buffer.append("\n");
            }
        }

        IOUtils.writeStringToFileNoExceptions(buffer.toString(), output, StandardCharsets.UTF_8.name());
    }
}

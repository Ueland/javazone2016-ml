package javazoneml.tools.data;

import javazoneml.model.Presentation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class DataFilter {
    public static final List<Predicate<Presentation>> EXCLUDES = new ArrayList<>();

    static{
        EXCLUDES.add(p -> "Mobispace - et distribuert tuplespace for j2me-omgivelser".equals(p.getTitle()) && p.getYear() == 2006);
        EXCLUDES.add(p -> "Domain driven pointcut design".equals(p.getTitle()) && p.getYear() == 2007);
        EXCLUDES.add(p -> "Building Real Swing Applications".equals(p.getTitle()) && p.getYear() == 2007);
        EXCLUDES.add(p -> "Top 5 plugins for Hudson and Chuck Norris".equals(p.getTitle()) && p.getYear() == 2010);
        EXCLUDES.add(p -> "Slett din JavaDoc".equals(p.getTitle()) && p.getYear() == 2010);
        EXCLUDES.add(p -> "Smidig åpenhet: Fra skyldfordeling til samarbeid".equals(p.getTitle()) && p.getYear() == 2010);
        EXCLUDES.add(p -> "Hjelp, jeg har tatt over en legacy applikasjon".equals(p.getTitle()) && p.getYear() == 2010);
        EXCLUDES.add(p -> "JavaScript design and architecture".equals(p.getTitle()) && p.getYear() == 2011);
        EXCLUDES.add(p -> "Strukturert refaktorering".equals(p.getTitle()) && p.getYear() == 2011);
        EXCLUDES.add(p -> "Context awareness with Android".equals(p.getTitle()) && p.getYear() == 2011);
        EXCLUDES.add(p -> "Erfaringer fra NAVs stillingssøk for mobil".equals(p.getTitle()) && p.getYear() == 2012);
        EXCLUDES.add(p -> "Scaling to billions".equals(p.getTitle()) && p.getYear() == 2012);
        EXCLUDES.add(p -> "Remote feature toggling for native mobile applications".equals(p.getTitle()) && p.getYear() == 2013);
        EXCLUDES.add(p -> "Build, deploy and test Enterprise JavaBeans in few seconds".equals(p.getTitle()) && p.getYear() == 2005);
    }
}

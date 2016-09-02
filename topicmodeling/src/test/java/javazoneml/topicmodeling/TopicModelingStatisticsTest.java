package javazoneml.topicmodeling;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TopicModelingStatisticsTest {
    private TopicModelingStatistics topicModelingStatistics;

    @Before
    public void setUp(){
        topicModelingStatistics = new TopicModelingStatistics();
    }

    @Test(expected = IllegalArgumentException.class)
    public void euclideanDistanceNullArray(){
        double x[] = {1.0, 2.0};
        topicModelingStatistics.euclideanDistance(x, null);

    }

    @Test(expected = IllegalArgumentException.class)
    public void euclideanDistanceArrayNotEqualLength(){
        double x[] = {1.0, 2.0};
        double y[] = {2.0, 3.0, 4.0};
        topicModelingStatistics.euclideanDistance(x, y);
    }

    @Test
    public void euclideanDistance(){
        double x[] = {4.0, 7.0};
        double y[] = {1.0, 3.0};
        double distance = topicModelingStatistics.euclideanDistance(x, y);
        Assert.assertEquals(5.0, distance, 0.0001);
    }

    @Test
    public void hellingerDistance(){
        double x[] = {16.0, 49.0};
        double y[] = {1.0, 9.0};
        double distance = topicModelingStatistics.hellingerDistance(x, y);
        Assert.assertEquals(5.0/Math.sqrt(2.0), distance, 0.0001);
    }
}

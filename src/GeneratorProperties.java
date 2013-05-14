import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: I827779
 * Date: 8/23/12
 * Time: 2:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class GeneratorProperties {
    private String baseLogFolder = "c:\\data\\";
    private float dailyRequestInMillions = 10;
    private String startDate = "2012-12-01";
    private String endDate = "2012-12-31";
    private int startYear = 2012;
    private int startMonth = 12;
    private int startDay = 1;

    private int endYear = 2012;
    private int endMonth = 12;
    private int endDay = 31;

    @Override
    public String toString() {
        return "GeneratorProperties{" +
                "baseLogFolder='" + baseLogFolder + '\'' +
                ", dailyRequestInMillions=" + dailyRequestInMillions +
                ", startDate='" + startDate + '\'' +
                ", endDate='" + endDate + '\'' +
                ", startYear=" + startYear +
                ", startMonth=" + startMonth +
                ", startDay=" + startDay +
                ", endYear=" + endYear +
                ", endMonth=" + endMonth +
                ", endDay=" + endDay +
                '}';
    }

    public GeneratorProperties (String filename) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(filename));
        } catch (IOException ff) {
            System.out.println("Cannot find " + filename + ", using default values on all properties now...\n" + toString());
        }

        baseLogFolder = properties.getProperty("baseLogFolder", baseLogFolder);
        if (!baseLogFolder.endsWith(File.separator)) {
            baseLogFolder = baseLogFolder + File.separator;
        }
        dailyRequestInMillions = Float.parseFloat(properties.getProperty("dailyRequestInMillions", String.valueOf(dailyRequestInMillions)));
        startDate = properties.getProperty("startDate", startDate);
        endDate = properties.getProperty("endDate", endDate);

        startYear = Integer.parseInt(startDate.substring(0, 4));
        startMonth = Integer.parseInt(startDate.substring(5, 7)) - 1;
        startDay = Integer.parseInt(startDate.substring(8, 10));

        endYear = Integer.parseInt(endDate.substring(0, 4));
        endMonth = Integer.parseInt(endDate.substring(5, 7)) - 1;
        endDay = Integer.parseInt(endDate.substring(8, 10));

        //System.out.println(toString());
    }

    public String getBaseLogFolder() {
        return baseLogFolder;
    }

    public float getDailyRequestInMillions() {
        return dailyRequestInMillions;
    }

    public int getStartYear() {
        return startYear;
    }

    public int getStartMonth() {
        return startMonth;
    }

    public int getStartDay() {
        return startDay;
    }

    public int getEndYear() {
        return endYear;
    }

    public int getEndMonth() {
        return endMonth;
    }

    public int getEndDay() {
        return endDay;
    }
}

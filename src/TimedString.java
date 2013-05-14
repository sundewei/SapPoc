import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * Created by IntelliJ IDEA.
 * User: I827779
 * Date: 10/21/11
 * Time: 4:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class TimedString implements Comparable {
    private final DateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH");

    public long ms;
    public String line;
    private static final Random RANDOM = new Random();

    public TimedString() {
    }

    public TimedString(long ms, String line) {
        this.ms = ms;
        this.line = line;
    }

    public int compareTo(Object other) {
        TimedString otherTimedString = (TimedString) other;
        return this.ms - otherTimedString.ms > 0 ? 1 : -1;
    }

    public String toString() {
        return this.line;
    }

    public String getYyyyMmDdLine() {
        String yyyy = simpleDateFormat.format(new Timestamp(ms)).substring(0, 4);
        String mm = simpleDateFormat.format(new Timestamp(ms)).substring(5, 7);
        String dd = simpleDateFormat.format(new Timestamp(ms)).substring(8, 10);
        String hh = simpleDateFormat.format(new Timestamp(ms)).substring(11, 13);
        //return new StringBuilder(yyyy).append(",").append(mm).append(",").append(dd).append(",").append((RANDOM.nextInt(50) + 50)).append(",").append(this.line).toString();
        //return new StringBuilder(yyyy).append(",").append(mm).append(",").append(dd).append(",").append(hh).append(",").append(this.line).toString();
        return new StringBuilder(this.line).toString();
    }

    public static void main(String[] arg) {
        TimedString ts = new TimedString();
        String dateString = ts.simpleDateFormat.format(new Date());
        String yyyy = dateString.substring(0, 4);
        String mm = dateString.substring(5, 7);
        String dd = dateString.substring(8, 10);
        System.out.println("yyyy="+yyyy);
        System.out.println("mm="+mm);
        System.out.println("dd="+dd);
    }
}

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.sql.*;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: I827779
 * Date: 10/18/11
 * Time: 4:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class Utility {
    public static String BASE_DEST_FOLDER;
    public static Random RANDOM = new Random();
    private static final Logger LOG = Logger.getLogger(Utility.class.getName());
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("[dd/MMM/yyyy:HH:mm:ss Z]");
    public static final DateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static final DateFormat SIMPLE_MONTH_FORMAT = new SimpleDateFormat("yyyy-MM");
    public static final DateFormat YYYY_MM_DATE_FORMAT = new SimpleDateFormat("yyyy-MM");
    public static final DateFormat MEDIUM_DATE_FORMAT = new SimpleDateFormat("MMMMM d, yyyy");
    public static final DecimalFormat MONEY_FORMATTER = new DecimalFormat("###.##");
    public static Map<String, String> STATE_REGION = new HashMap<String, String>();
    static String[] STATES;
    public static Map<String, String> STATE_ABBREVIATION = new HashMap<String, String>();
    public static Map<String, String> ABBREVIATION_STATE = new HashMap<String, String>();

    public static void main(String[] args) throws Exception {
        getAmazonProductMap();
    }

    public static Map<Integer, AmazonProduct> getAmazonProductMap() throws Exception {
        InputStream in = Utility.class.getResourceAsStream("/resources/amazonProductMap.csv" );
        Map<Integer, AmazonProduct> map = new HashMap<Integer, AmazonProduct>();
        List<String> lines = IOUtils.readLines(in);
        for (String line: lines) {
            String[] values = CSVUtils.parseLine(line);
            AmazonProduct amazonProduct = new AmazonProduct();
            amazonProduct.setId(Integer.parseInt(values[0]));
            amazonProduct.setAsin(values[1]);
            amazonProduct.setNumOfReviews(Integer.parseInt(values[2]));
            amazonProduct.setAvgRating(Float.parseFloat(values[3]));
            amazonProduct.setTopCategoryId(Integer.parseInt(values[4]));
            map.put(amazonProduct.getId(), amazonProduct);
        }
//System.out.println("map.size()="+map.size());
        in.close();
        return map;
    }

    public static int getFuzzyNumber(int base) {
        return getFuzzyNumber(base, 1);
    }

    public static long getFuzzyNumber(long base, long small, long big) {
        long a = nextLong(small);
        long b = big;
        long c = (small / 2 + big);
        //System.out.println("a="+a);
        //System.out.println("b="+b);
        //System.out.println("c="+c);
        //System.out.println("((double)(a + b) / c)="+((double)(a + b) / c));

        return (long) (base * ((double) (a + b) / c));
    }

    public static int getFuzzyNumber(int base, int min) {
        int value = 0;
        while (value < min) {
            value = (int) (base * 2 * RANDOM.nextGaussian());
        }
        return value;
    }

    public static Map<String, List<String>> getBrowserMap() throws Exception {
        String[] browserTypes = new String[]{"Chrome", "Firefox", "Mobile", "MSIE", "Opera", "Others", "Safari"};
        String baseName = "/resources/browsers/";
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        for (String browserType: browserTypes) {
            InputStream browserIn = Utility.class.getResourceAsStream(baseName + browserType + ".txt");
            List<String> lines = IOUtils.readLines(browserIn);
            browserIn.close();
            map.put(browserType, lines);
        }
        return map;
    }

    public static AccessEntry getAccessEntry(String line) {
        // An AccessData object will be created for each line if possible
        AccessEntry accessEntry = null;
        try {
            accessEntry = new AccessEntry();
            // Parse the value separated line using space as the delimiter
            CSVParser csvParser = new CSVParser(new StringReader(line));
            csvParser.getStrategy().setDelimiter(' ');

            // Now get all the values from the line
            String[] values = csvParser.getLine();

            // Get the IP
            accessEntry.ip = values[0];

            // The time is split into 2 values so they have to be combined
            // then sent to match the time regular expression
            // "[02/Aug/2011:00:00:04" + " -0700]" = "[02/Aug/2011:00:00:04 -0700]"
            accessEntry.timestamp = new Timestamp(DATE_FORMAT.parse(values[3] + " " + values[4]).getTime());

            // The resource filed has 3 fields (HTTP Method, Page and HTTP protocol)
            // so it has to be further split by spaces
            String reqInfo = values[5];
            String[] reqInfoArr = reqInfo.split(" ");

            // Get the HTTP method
            accessEntry.method = reqInfoArr[0];

            // Get the page requested
            accessEntry.resource = reqInfoArr[1];

            // Get the HTTP response code
            accessEntry.httpCode = Integer.parseInt(values[6]);

            // Try to get the response data size in bytes, if a hyphen shows up,
            // that means the client has a cache of this page and no data is
            // sent back
            try {
                accessEntry.dataLength = Long.parseLong(values[7]);
            } catch (NumberFormatException nfe) {
                accessEntry.dataLength = 0;
            }

            if (values.length >= 9) {
                accessEntry.referrer = values[8];
            }

            if (values.length >= 10) {
                accessEntry.userAgent = values[9];
            }

            return accessEntry;
        } catch (IOException ioe) {
            LOG.info(ioe);
            return null;
        } catch (ParseException pe) {
            LOG.info(pe);
            return null;
        } catch (NumberFormatException nfe) {
            LOG.info(nfe);
            return null;
        }
    }

    public static int getIndex(String column, List<String> columns) {
        for (int i = 0; i < columns.size(); i++) {
            if (column.equalsIgnoreCase(columns.get(i))) {
                return i;
            }
        }
        return -1;
    }

    public static List<Header> getHeaders(File filename) throws Exception {
        List<Header> headers = new ArrayList<Header>();
        List<String> lines = FileUtils.readLines(filename);
        List<String> lineList = new ArrayList<String>();
        Header header = new Header();
        for (String line : lines) {
            lineList.add(line);
            if (line.equals("----------------------------------------------------------")) {
                if (lineList.get(0).contains("amazon.com/")) {
                    header.init(lineList);
                    headers.add(header);
                }
                header = new Header();
                lineList = new ArrayList<String>();
            }
        }
        return headers;
    }

    public static String getPaddedNumberString(int num, int length, String padChar) {
        StringBuilder sb = new StringBuilder(String.valueOf(num));
        while (sb.length() < length) {
            sb.insert(0, padChar);
        }

        while (sb.length() > length) {
            sb.deleteCharAt(0);
        }

        return sb.toString();
    }

    public static String getPaddedNumberString(String source, int length, String padChar) {
        StringBuilder sb = new StringBuilder(source);
        while (sb.length() < length) {
            sb.insert(0, padChar);
        }
        return sb.toString();
    }

    public static long nextLong(long n) {
        // error checking and 2^x checking removed for simplicity.
        long bits, val;
        do {
            bits = (RANDOM.nextLong() << 1) >>> 1;
            val = bits % n;
        } while (bits - val + (n - 1) < 0L);
        return val;
    }

    public static long getIpGeoNum(String ip) throws IllegalArgumentException {
        String[] ipWxyz = ip.split("\\.");
        if (ipWxyz.length != 4) {
            throw new IllegalArgumentException(ip + " is not a valid IP address.");
        }
        long ipNum = 0;
        return 16777216 * Long.parseLong(ipWxyz[0]) + 65536 * Long.parseLong(ipWxyz[1]) +
                256 * Long.parseLong(ipWxyz[2]) + Long.parseLong(ipWxyz[3]);
    }

    public static long[] getIntervalMs(long baseMs, int size) {
        int min15 = 900000;
        int chance = RANDOM.nextInt(100);
        long startMs = 0;
        long endMs = 0;
        if (chance < 80) {
            // within 30 mins
            startMs = baseMs - RANDOM.nextInt(min15);
            endMs = baseMs + RANDOM.nextInt(min15);
        } else {
            // within 4 hours
            startMs = baseMs - RANDOM.nextInt(min15 * RANDOM.nextInt(8));
            endMs = baseMs + RANDOM.nextInt(min15 * RANDOM.nextInt(8));
        }
        Set<Long> neededMs = new HashSet<Long>();

        while (neededMs.size() < size) {
            neededMs.add(startMs + nextLong(endMs - startMs));
        }
        System.out.println("neededMs.size()=" + neededMs.size());
        long[] arr = new long[neededMs.size()];
        int i = 0;
        Iterator<Long> it = neededMs.iterator();
        while (it.hasNext()) {
            arr[i] = it.next();
            i++;
        }
        return arr;
    }

    public static String getAccessLogLine(String ip, Header header, long ms) {
        StringBuilder sb = new StringBuilder();
        sb.append(ip).append(" - - ");
        sb.append(DATE_FORMAT.format(new Timestamp(ms))).append(" ");
        // line :
        // "GET /Sony-BRAVIA-KDL-46V5100-46-Inch-1080p/dp/B001T9N0EO/ HTTP/1.1"
        sb.append("\"");
        sb.append(header.method).append(" ");
        sb.append(header.resource).append(" ");
        sb.append(header.http);
        sb.append("\" ");
        sb.append(header.httpStatusCode).append(" ");
        if (header.reqMap.get("Referer") != null) {
            sb.append("\"");
            sb.append(header.reqMap.get("Referer"));
            sb.append("\"");
            sb.append(" ");
        } else {
            sb.append("-").append(" ");
        }
        if (header.reqMap.get("User-Agent") != null) {
            sb.append("\"");
            sb.append(header.reqMap.get("User-Agent"));
            sb.append("\"");
            sb.append(" ");
        } else {
            sb.append("-").append(" ");
        }
        return sb.toString();
    }

    public static int[] getRangeStartEnd(int length) {
        while (true) {
            int a = RANDOM.nextInt(length);
            int b = RANDOM.nextInt(length);
            if (Math.abs(a - b) <= 15 && Math.abs(a - b) >= 2) {
                return new int[]{Math.min(a, b), Math.max(a, b)};
            }
        }
    }

    public static List<CssUrl> getCssUrls(String filename) throws Exception {
        InputStream in = Utility.class.getResourceAsStream(filename);
        List<String> lines = IOUtils.readLines(in);
        List<CssUrl> list = new ArrayList<CssUrl>();
        for (String line : lines) {
            String[] values = CSVUtils.parseLine(line);
            list.add(new CssUrl(Integer.parseInt(values[0]), values[1]));
        }
        in.close();
        return list;
    }

    public static List<ImageUrl> getImageUrls(String filename) throws Exception {
        InputStream in = Utility.class.getResourceAsStream(filename);
        List<String> lines = IOUtils.readLines(in);
        List<ImageUrl> list = new ArrayList<ImageUrl>();
        for (String line : lines) {
            String[] values = CSVUtils.parseLine(line);
            list.add(new ImageUrl(Integer.parseInt(values[0]), values[1]));
        }
        in.close();
        return list;
    }

    public static List<JsUrl> getJsUrls(String filename) throws Exception {
        InputStream in = Utility.class.getResourceAsStream(filename);
        List<String> lines = IOUtils.readLines(in);
        List<JsUrl> list = new ArrayList<JsUrl>();
        for (String line : lines) {
            String[] values = CSVUtils.parseLine(line);
            list.add(new JsUrl(Integer.parseInt(values[0]), values[1]));
        }
        in.close();
        return list;
    }

    public static Map<String, State> getStateMap() throws Exception {
        InputStream incomeIn = Utility.class.getResourceAsStream("/resources/state/stateIncome.csv");
        InputStream populationIn = Utility.class.getResourceAsStream("/resources/state/statePopulation.csv");
        List<String> incomeLines = IOUtils.readLines(incomeIn);
        List<String> populationLines = IOUtils.readLines(populationIn);
        incomeIn.close();
        populationIn.close();
        long totalIncome = 0l;
        long avgIncome = 0l;
        Map<String, State> stateMap = new HashMap<String, State>();

        for (String line : populationLines) {
            String[] values = CSVUtils.parseLine(line);
            State state = new State(STATE_ABBREVIATION.get(values[2]), values[2]);
            state.setPopulation(Long.parseLong(values[3]));
            state.setPopulationPercentage(Float.parseFloat(values[12].replace("%", "")));
            state.setRegion(STATE_REGION.get(state.getFullName()));
            stateMap.put(state.getAbbreviation(), state);
        }

        for (String line : incomeLines) {
            String[] values = CSVUtils.parseLine(line);
            State state = stateMap.get(STATE_ABBREVIATION.get(values[0]));
            long income = Long.parseLong(values[1]);
            totalIncome += income;
            state.setIncome(income);
        }
        avgIncome = totalIncome / stateMap.size();
        for (Map.Entry<String, State> entry : stateMap.entrySet()) {
            State state = entry.getValue();
            state.setIncomePercentage(((float) state.getIncome() / (float) avgIncome));
        }
        return stateMap;
    }

    public static long getViewingStartMs(Calendar calendar, int hour) throws ParseException {
        Calendar clearedCalendar = clearMinutes(calendar);
//System.out.println("111, " + clearedCalendar.getTime().toLocaleString());
        clearedCalendar.set(Calendar.HOUR, hour);
        long startMs = clearedCalendar.getTime().getTime();
//System.out.println("222, " + clearedCalendar.getTime().toLocaleString());
        clearedCalendar.add(Calendar.HOUR, 1);
        long endMs = clearedCalendar.getTime().getTime();
//System.out.println("333, " + clearedCalendar.getTime().toLocaleString());
        return startMs + nextLong(endMs - startMs);
    }

    public static long getNextMsWithin(long baseMs, long lengthMs) {
        return baseMs + nextLong(lengthMs);
    }

    private static Calendar clearMinutes(Calendar calendar) {
        Calendar clonedCalendar = (Calendar)calendar.clone();
        clonedCalendar.set(Calendar.HOUR_OF_DAY, 0);
        clonedCalendar.clear(Calendar.MINUTE);
        clonedCalendar.clear(Calendar.SECOND);
        clonedCalendar.clear(Calendar.MILLISECOND);
        return clonedCalendar;
    }

    static {
        STATES = new String[]{
                "MN", "CA", "WV", "UT", "WA",
                "TX", "FL", "GA", "OH", "NC",
                "TN", "MO", "IL", "AL", "PA",
                "IN", "OK", "LA", "MI", "KY",
                "VA", "AR", "SC", "AZ", "WI",
                "MS", "NY", "CO", "KS", "IA",
                "NM", "NE", "NV", "ME", "ID",
                "OR", "NH", "MD", "SD", "WY",
                "ND", "MT", "MA", "NJ", "DE",
                "CT", "AK", "RI", "HI", "VT",
                "PR"
        };


        STATE_ABBREVIATION.put("Alabama", "AL");
        STATE_ABBREVIATION.put("Alaska", "AK");
        STATE_ABBREVIATION.put("Arizona", "AZ");
        STATE_ABBREVIATION.put("Arkansas", "AR");
        STATE_ABBREVIATION.put("California", "CA");
        STATE_ABBREVIATION.put("Colorado", "CO");
        STATE_ABBREVIATION.put("Connecticut", "CT");
        STATE_ABBREVIATION.put("Delaware", "DE");
        STATE_ABBREVIATION.put("Washington DC", "DC");
        STATE_ABBREVIATION.put("Florida", "FL");
        STATE_ABBREVIATION.put("Georgia", "GA");
        STATE_ABBREVIATION.put("Hawaii", "HI");
        STATE_ABBREVIATION.put("Idaho", "ID");
        STATE_ABBREVIATION.put("Illinois", "IL");
        STATE_ABBREVIATION.put("Indiana", "IN");
        STATE_ABBREVIATION.put("Iowa", "IA");
        STATE_ABBREVIATION.put("Kansas", "KS");
        STATE_ABBREVIATION.put("Kentucky", "KY");
        STATE_ABBREVIATION.put("Louisiana", "LA");
        STATE_ABBREVIATION.put("Maine", "ME");
        STATE_ABBREVIATION.put("Montana", "MT");
        STATE_ABBREVIATION.put("Nebraska", "NE");
        STATE_ABBREVIATION.put("Nevada", "NV");
        STATE_ABBREVIATION.put("New Hampshire", "NH");
        STATE_ABBREVIATION.put("New Jersey", "NJ");
        STATE_ABBREVIATION.put("New Mexico", "NM");
        STATE_ABBREVIATION.put("New York", "NY");
        STATE_ABBREVIATION.put("North Carolina", "NC");
        STATE_ABBREVIATION.put("North Dakota", "ND");
        STATE_ABBREVIATION.put("Ohio", "OH");
        STATE_ABBREVIATION.put("Oklahoma", "OK");
        STATE_ABBREVIATION.put("Oregon", "OR");
        STATE_ABBREVIATION.put("Maryland", "MD");
        STATE_ABBREVIATION.put("Massachusetts", "MA");
        STATE_ABBREVIATION.put("Michigan", "MI");
        STATE_ABBREVIATION.put("Minnesota", "MN");
        STATE_ABBREVIATION.put("Mississippi", "MS");
        STATE_ABBREVIATION.put("Missouri", "MO");
        STATE_ABBREVIATION.put("Pennsylvania", "PA");
        STATE_ABBREVIATION.put("Rhode Island", "RI");
        STATE_ABBREVIATION.put("South Carolina", "SC");
        STATE_ABBREVIATION.put("South Dakota", "SD");
        STATE_ABBREVIATION.put("Tennessee", "TN");
        STATE_ABBREVIATION.put("Texas", "TX");
        STATE_ABBREVIATION.put("Utah", "UT");
        STATE_ABBREVIATION.put("Vermont", "VT");
        STATE_ABBREVIATION.put("Virginia", "VA");
        STATE_ABBREVIATION.put("Washington", "WA");
        STATE_ABBREVIATION.put("West Virginia", "WV");
        STATE_ABBREVIATION.put("Wisconsin", "WI");
        STATE_ABBREVIATION.put("Wyoming", "WY");
        STATE_ABBREVIATION.put("Puerto Rico", "PR");

        for (Map.Entry<String, String> entry : STATE_ABBREVIATION.entrySet()) {
            ABBREVIATION_STATE.put(entry.getValue(), entry.getKey());
        }

        //  Maine, New Hampshire, Vermont, Massachusetts, Rhode Island, Connecticut
        Utility.STATE_REGION.put("Maine", "Northeast");
        Utility.STATE_REGION.put("New Hampshire", "Northeast");
        Utility.STATE_REGION.put("Vermont", "Northeast");
        Utility.STATE_REGION.put("Massachusetts", "Northeast");
        Utility.STATE_REGION.put("Rhode Island", "Northeast");
        Utility.STATE_REGION.put("Connecticut", "Northeast");

        //  New York, Pennsylvania, New Jersey
        Utility.STATE_REGION.put("New York", "Northeast");
        Utility.STATE_REGION.put("Pennsylvania", "Northeast");
        Utility.STATE_REGION.put("New Jersey", "Northeast");

        // Wisconsin, Michigan, Illinois, Indiana, Ohio
        Utility.STATE_REGION.put("Wisconsin", "Midwest");
        Utility.STATE_REGION.put("Michigan", "Midwest");
        Utility.STATE_REGION.put("Illinois", "Midwest");
        Utility.STATE_REGION.put("Indiana", "Midwest");
        Utility.STATE_REGION.put("Ohio", "Midwest");

        //  Missouri, North Dakota, South Dakota, Nebraska, Kansas, Minnesota, Iowa
        Utility.STATE_REGION.put("Missouri", "Midwest");
        Utility.STATE_REGION.put("North Dakota", "Midwest");
        Utility.STATE_REGION.put("South Dakota", "Midwest");
        Utility.STATE_REGION.put("Nebraska", "Midwest");
        Utility.STATE_REGION.put("Kansas", "Midwest");
        Utility.STATE_REGION.put("Minnesota", "Midwest");
        Utility.STATE_REGION.put("Iowa", "Midwest");

        //  Delaware, Maryland, District of Columbia, Virginia, West Virginia, North Carolina, South Carolina, Georgia, Florida
        Utility.STATE_REGION.put("Delaware", "South");
        Utility.STATE_REGION.put("Maryland", "South");
        Utility.STATE_REGION.put("Washington DC", "South");
        Utility.STATE_REGION.put("Virginia", "South");
        Utility.STATE_REGION.put("West Virginia", "South");
        Utility.STATE_REGION.put("North Carolina", "South");
        Utility.STATE_REGION.put("South Carolina", "South");
        Utility.STATE_REGION.put("Georgia", "South");
        Utility.STATE_REGION.put("Florida", "South");

        // Kentucky, Tennessee, Mississippi, Alabama
        Utility.STATE_REGION.put("Kentucky", "South");
        Utility.STATE_REGION.put("Tennessee", "South");
        Utility.STATE_REGION.put("Mississippi", "South");
        Utility.STATE_REGION.put("Alabama", "South");

        //  Oklahoma, Texas, Arkansas, Louisiana
        Utility.STATE_REGION.put("Oklahoma", "South");
        Utility.STATE_REGION.put("Texas", "South");
        Utility.STATE_REGION.put("Arkansas", "South");
        Utility.STATE_REGION.put("Louisiana", "South");

        // Idaho, Montana, Wyoming, Nevada, Utah, Colorado, Arizona, New Mexico
        Utility.STATE_REGION.put("Idaho", "West");
        Utility.STATE_REGION.put("Montana", "West");
        Utility.STATE_REGION.put("Wyoming", "West");
        Utility.STATE_REGION.put("Nevada", "West");
        Utility.STATE_REGION.put("Utah", "West");
        Utility.STATE_REGION.put("Colorado", "West");
        Utility.STATE_REGION.put("Arizona", "West");
        Utility.STATE_REGION.put("New Mexico", "West");

        // Alaska, Washington, Oregon, California, Hawaii
        Utility.STATE_REGION.put("Alaska", "West");
        Utility.STATE_REGION.put("Washington", "West");
        Utility.STATE_REGION.put("Oregon", "West");
        Utility.STATE_REGION.put("California", "West");
        Utility.STATE_REGION.put("Hawaii", "West");
    }

    public static List<Session> getSessions(TreeMap<Long, String> sortedMap, int sessionInMin) {
        Session session = null;
        List<Session> sessionList = new LinkedList<Session>();
        long prevTs = -1l;
        long entryTs = -1l;
        long endTs = -1l;
        long length = sessionInMin * 60 * 1000;
        for (Map.Entry<Long, String> entry : sortedMap.entrySet()) {
            if (session == null) {
                session = new Session();
            }
            entryTs = entry.getKey();
            if (prevTs == -1l) {
                session.addItemLookup(entryTs, entry.getValue());
            } else {
                endTs = prevTs + length;
                if (entryTs <= endTs && entryTs > prevTs) {
                    // Within session
                    session.addItemLookup(entryTs, entry.getValue());
                } else {
                    sessionList.add(session);
                    session = new Session();
                    session.addItemLookup(entryTs, entry.getValue());

                }
            }
            prevTs = entryTs;
        }
        // Add the last session
        if (session.itemLookups.size() > 0) {
            sessionList.add(session);
        }
        return sessionList;
    }

    public static String getItemAsin(String line) {
        String key = "PPSID=";
        int dpIdx = line.indexOf("PPSID=");
        int idEnd = line.indexOf("&", dpIdx + 1);
        if (idEnd < 0) {
            line.indexOf("\"", dpIdx + 1);
            idEnd = line.length();
        }
        if (dpIdx >= 0) {
            return line.substring(dpIdx + key.length(), idEnd);
        }
        return null;
    }
}

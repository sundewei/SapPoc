import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: I827779
 * Date: 4/28/12
 * Time: 6:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class AccessLogGenerator {
    private static float MILLION_REQS = 0.01f;

    private static Random RANDOM = new Random();
    private static List<CssUrl> CSS_URLS;
    private static List<ImageUrl> IMAGE_URLS;
    private static List<JsUrl> JS_URLS;
    private static Map<String, List<String>> BROWSER_MAP;
    private static Map<String, TreeMap<Long, Long>> STATE_IP_RANGE_MAP;
    private static Map<Integer, Double> MONTHLY_SALES_ADJ_FACTORS = new HashMap<Integer, Double>();
    private static Map<Integer, Double> DAY_OF_WEEK_FACTORS = new HashMap<Integer, Double>();
    private static Map<Integer, Double> YEAR_ADJ_FACTOR = new HashMap<Integer, Double>();
    private static Map<Integer, Bag> CATEGORY_ITEM_BAG_MAP = new HashMap<Integer, Bag>();
    private static Bag CATEGORY_ITEM_COUNT_BAG = new Bag();
    private static Map<Integer, AmazonProduct> AMAZON_PRODUCT_MAP;
    private static Map<String, State> STATE_MAP;
    private static Bag STATE_POPULATION_BAG = new Bag();
    private static Map<Integer, Double> HOUR_LOG_VOLUME = new HashMap<Integer, Double>();
    private static Bag BROWSER_BAG;
    private static Bag REFERRER_BAG = new Bag();
    private static Map<Integer, Double> CATEGORY_CONTINUE_BROWSE_FACTOR = new HashMap<Integer, Double>();
    private static final int SESSION_LENGTH_IN_MINUTE = 30;
    private static int SESSION_LENGTH_IN_MS = SESSION_LENGTH_IN_MINUTE * 60 * 1000;

    private static final Calendar START_CALENDAR = Calendar.getInstance();
    private static final Calendar END_CALENDAR = Calendar.getInstance();

    // a request needs to finish loading within 5 second
    private static final int REQUEST_LOADING_LENGTH = 5000;

    private static int DAILY_REQUEST_NUM = (int)(1000000 * MILLION_REQS);


    static {
        init();
    }

    public static void init() {
        REFERRER_BAG.addBalls("https://www.google.com/", 666);
        REFERRER_BAG.addBalls("http://www.yahoo.com/", 160);
        REFERRER_BAG.addBalls("http://www.bing.com/", 120);
        REFERRER_BAG.addBalls("http://www.ask.com/?o=0&l=dir", 35);
        REFERRER_BAG.addBalls("http://www.aol.com/", 19);
        REFERRER_BAG.addBalls("http://www.incredibledeals.com/", 200);

        CATEGORY_ITEM_COUNT_BAG.addBalls("1", 383605);
        CATEGORY_ITEM_COUNT_BAG.addBalls("2", 90081);
        CATEGORY_ITEM_COUNT_BAG.addBalls("13", 85354);
        CATEGORY_ITEM_COUNT_BAG.addBalls("5", 24444);
        CATEGORY_ITEM_COUNT_BAG.addBalls("3", 7502);
        CATEGORY_ITEM_COUNT_BAG.addBalls("21", 1272);
        CATEGORY_ITEM_COUNT_BAG.addBalls("4", 897);
        CATEGORY_ITEM_COUNT_BAG.addBalls("47", 480);
        CATEGORY_ITEM_COUNT_BAG.addBalls("191", 246);
        CATEGORY_ITEM_COUNT_BAG.addBalls("36", 178);
        CATEGORY_ITEM_COUNT_BAG.addBalls("284", 134);
        CATEGORY_ITEM_COUNT_BAG.addBalls("48", 117);
        CATEGORY_ITEM_COUNT_BAG.addBalls("89", 108);
        CATEGORY_ITEM_COUNT_BAG.addBalls("280", 75);
        CATEGORY_ITEM_COUNT_BAG.addBalls("396", 39);
        CATEGORY_ITEM_COUNT_BAG.addBalls("210", 32);
        CATEGORY_ITEM_COUNT_BAG.addBalls("23", 28);
        CATEGORY_ITEM_COUNT_BAG.addBalls("317", 20);
        CATEGORY_ITEM_COUNT_BAG.addBalls("101", 14);
        CATEGORY_ITEM_COUNT_BAG.addBalls("290", 10);
        CATEGORY_ITEM_COUNT_BAG.addBalls("96", 9);
        CATEGORY_ITEM_COUNT_BAG.addBalls("61", 6);
        CATEGORY_ITEM_COUNT_BAG.addBalls("28", 4);
        CATEGORY_ITEM_COUNT_BAG.addBalls("276", 3);
        CATEGORY_ITEM_COUNT_BAG.addBalls("24", 2);

        CATEGORY_CONTINUE_BROWSE_FACTOR.put(1, 0.5D);
        CATEGORY_CONTINUE_BROWSE_FACTOR.put(4, 0.5D);
        CATEGORY_CONTINUE_BROWSE_FACTOR.put(11, 0.75D);
        CATEGORY_CONTINUE_BROWSE_FACTOR.put(78, 0.1D);

        try {
            STATE_MAP = Utility.getStateMap();
            CSS_URLS = Utility.getCssUrls("/resources/urls/cssUrls.csv");
            IMAGE_URLS = Utility.getImageUrls("/resources/urls/imageUrls.csv");
            JS_URLS = Utility.getJsUrls("/resources/urls/jsUrls.csv");
            BROWSER_MAP = Utility.getBrowserMap();
            STATE_IP_RANGE_MAP = IpGenerator.getAllStateRangeMap();
            AMAZON_PRODUCT_MAP = Utility.getAmazonProductMap();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Set<Integer> distTopCategory = new HashSet<Integer>();

        for (Map.Entry<Integer, AmazonProduct> entry: AMAZON_PRODUCT_MAP.entrySet()) {
            distTopCategory.add(entry.getValue().getTopCategoryId());
        }

        Collection<AmazonProduct> amazonProductCollection = AMAZON_PRODUCT_MAP.values();
        for (Integer topCatId: distTopCategory) {
            Bag bag = CATEGORY_ITEM_BAG_MAP.get(topCatId) != null ? CATEGORY_ITEM_BAG_MAP.get(topCatId) : new Bag();

            for (AmazonProduct amazonProduct: amazonProductCollection) {
                if (amazonProduct.getTopCategoryId() == topCatId) {
                    if (topCatId == 31 && amazonProduct.getAvgRating() < 3.5f) {
                        // Make the high rated DC num of review lower rate
                        bag.addBalls(String.valueOf(amazonProduct.getId()), (amazonProduct.getNumOfReviews() + 50) * 5);
                    } else if (topCatId == 31 && amazonProduct.getAvgRating() < 5f && amazonProduct.getAvgRating() >= 3.5f) {
                        // Make the high rated DC num of good review lower rate
                        bag.addBalls(String.valueOf(amazonProduct.getId()), (amazonProduct.getNumOfReviews() + 50) * 2);
                    } else {
                        // normal item count based on num of reviews
                        bag.addBalls(String.valueOf(amazonProduct.getId()), amazonProduct.getNumOfReviews() + 30);
                    }
                }
            }
            CATEGORY_ITEM_BAG_MAP.put(topCatId, bag);
        }

        for (Map.Entry<String, State> entry: STATE_MAP.entrySet()) {
            STATE_POPULATION_BAG.addBalls(entry.getValue().getAbbreviation(), entry.getValue().getPopulation());
        }

        BROWSER_BAG = getBrowserBag();

        START_CALENDAR.set(Calendar.YEAR, 2012);
        START_CALENDAR.set(Calendar.MONTH, Calendar.JULY);
        START_CALENDAR.set(Calendar.DAY_OF_MONTH, 1);
        START_CALENDAR.set(Calendar.HOUR_OF_DAY, 0);
        START_CALENDAR.set(Calendar.MINUTE, 0);
        START_CALENDAR.set(Calendar.SECOND, 0);
        START_CALENDAR.set(Calendar.MILLISECOND, 0);

        END_CALENDAR.set(Calendar.YEAR, 2012);
        END_CALENDAR.set(Calendar.MONTH, Calendar.JULY);
        END_CALENDAR.set(Calendar.DAY_OF_MONTH, 31);
        END_CALENDAR.set(Calendar.HOUR_OF_DAY, 23);
        END_CALENDAR.set(Calendar.MINUTE, 59);
        END_CALENDAR.set(Calendar.SECOND, 59);
        END_CALENDAR.set(Calendar.MILLISECOND, 999);

        DAY_OF_WEEK_FACTORS.put(Calendar.MONDAY, 0.92d);
        DAY_OF_WEEK_FACTORS.put(Calendar.TUESDAY, 0.93d);
        DAY_OF_WEEK_FACTORS.put(Calendar.WEDNESDAY, 0.91d);
        DAY_OF_WEEK_FACTORS.put(Calendar.THURSDAY, 0.98d);
        DAY_OF_WEEK_FACTORS.put(Calendar.FRIDAY, 1.10d);
        DAY_OF_WEEK_FACTORS.put(Calendar.SATURDAY, 1.14d);
        DAY_OF_WEEK_FACTORS.put(Calendar.SUNDAY, 1.12d);

        // Month adj factors
        MONTHLY_SALES_ADJ_FACTORS.put(Calendar.JANUARY, 0.871D);
        MONTHLY_SALES_ADJ_FACTORS.put(Calendar.FEBRUARY, 0.892D);
        MONTHLY_SALES_ADJ_FACTORS.put(Calendar.MARCH, 0.962D);
        MONTHLY_SALES_ADJ_FACTORS.put(Calendar.APRIL, 0.972D);
        MONTHLY_SALES_ADJ_FACTORS.put(Calendar.MAY, 0.997D);
        MONTHLY_SALES_ADJ_FACTORS.put(Calendar.JUNE, 0.978D);
        MONTHLY_SALES_ADJ_FACTORS.put(Calendar.JULY, 0.968D);
        MONTHLY_SALES_ADJ_FACTORS.put(Calendar.AUGUST, 0.981D);
        MONTHLY_SALES_ADJ_FACTORS.put(Calendar.SEPTEMBER, 0.912D);
        MONTHLY_SALES_ADJ_FACTORS.put(Calendar.OCTOBER, 0.962D);
        MONTHLY_SALES_ADJ_FACTORS.put(Calendar.NOVEMBER, 1.099D);
        MONTHLY_SALES_ADJ_FACTORS.put(Calendar.DECEMBER, 1.415D);

        YEAR_ADJ_FACTOR.put(2008, 1D);
        YEAR_ADJ_FACTOR.put(2009, 1.12D);
        YEAR_ADJ_FACTOR.put(2010, 1.232D);
        YEAR_ADJ_FACTOR.put(2011, 1.4784D);
        YEAR_ADJ_FACTOR.put(2012, 1.98D);

        HOUR_LOG_VOLUME.put(0, 0.035d);
        HOUR_LOG_VOLUME.put(1, 0.0175d);
        HOUR_LOG_VOLUME.put(2, 0.0175d);
        HOUR_LOG_VOLUME.put(3, 0.0175d);
        HOUR_LOG_VOLUME.put(4, 0.0175d);
        HOUR_LOG_VOLUME.put(5, 0.0175d);
        HOUR_LOG_VOLUME.put(6, 0.023d);
        HOUR_LOG_VOLUME.put(7, 0.0175d);
        HOUR_LOG_VOLUME.put(8, 0.0116d);
        HOUR_LOG_VOLUME.put(9, 0.07d);
        HOUR_LOG_VOLUME.put(10, 0.093d);
        HOUR_LOG_VOLUME.put(11, 0.085d);
        HOUR_LOG_VOLUME.put(12, 0.04d);
        HOUR_LOG_VOLUME.put(13, 0.0816d);
        HOUR_LOG_VOLUME.put(14, 0.093d);
        HOUR_LOG_VOLUME.put(15, 0.07d);
        HOUR_LOG_VOLUME.put(16, 0.0582d);
        HOUR_LOG_VOLUME.put(17, 0.0466d);
        HOUR_LOG_VOLUME.put(18, 0.023d);
        HOUR_LOG_VOLUME.put(19, 0.0757d);
        HOUR_LOG_VOLUME.put(20, 0.04d);
        HOUR_LOG_VOLUME.put(21, 0.029d);
        HOUR_LOG_VOLUME.put(22, 0.023d);
        HOUR_LOG_VOLUME.put(23, 0.035d);
    }

    public static void main(String[] arg) throws Exception {

        GeneratorProperties properties = new GeneratorProperties(arg[0]);
        Utility.BASE_DEST_FOLDER = properties.getBaseLogFolder();
        MILLION_REQS = properties.getDailyRequestInMillions();
        DAILY_REQUEST_NUM = (int)(1000000 * MILLION_REQS);
        START_CALENDAR.set(Calendar.YEAR, properties.getStartYear());
        START_CALENDAR.set(Calendar.MONTH, properties.getStartMonth());
        START_CALENDAR.set(Calendar.DAY_OF_MONTH, properties.getStartDay());
System.out.println("Start date: " + START_CALENDAR.getTime().toLocaleString());
        END_CALENDAR.set(Calendar.YEAR, properties.getEndYear());
        END_CALENDAR.set(Calendar.MONTH, properties.getEndMonth());
        END_CALENDAR.set(Calendar.DAY_OF_MONTH, properties.getEndDay());
System.out.println("End date: " + END_CALENDAR.getTime().toLocaleString());
        while (START_CALENDAR.before(END_CALENDAR)) {
            String dateString = Utility.SIMPLE_DATE_FORMAT.format(START_CALENDAR.getTime());
            //if (!flagFile.exists()) {
            //    flagFile.createNewFile();
                for (int hour = 0; hour < 24; hour++) {
                    int fileIndex = 0;
                    TreeSet<TimedString> hourlyAccessLogLines = new TreeSet<TimedString>();

System.out.println("Working on ----> " + dateString + ": " + hour);
                    long viewingStartMs = Utility.getViewingStartMs(START_CALENDAR, hour);
                    long hourlyReqCount =
                            (long) (HOUR_LOG_VOLUME.get(hour) *
                                    Utility.getFuzzyNumber(DAILY_REQUEST_NUM / 24) *
                                    YEAR_ADJ_FACTOR.get(START_CALENDAR.get(Calendar.YEAR)) *
                                    MONTHLY_SALES_ADJ_FACTORS.get(START_CALENDAR.get(Calendar.MONTH)) *
                                    DAY_OF_WEEK_FACTORS.get(START_CALENDAR.get(Calendar.DAY_OF_WEEK)));
                    long reqCountIndex = 0;
                    while (reqCountIndex < hourlyReqCount) {
                        int reqCount = getSessionRequestCount();
                        Collection<TimedString> allRequestsInSession = getSessionRequests(reqCount, viewingStartMs, hour != 23);
                        reqCountIndex += reqCount;
                        hourlyAccessLogLines.addAll(allRequestsInSession);
                        if (hourlyAccessLogLines.size() > 180000) {
                            String filename = getFilename(dateString, hour, fileIndex);
                            writeToFile(hourlyAccessLogLines, filename);
                            hourlyAccessLogLines.clear();
                            fileIndex++;
                        }
                    }
                    if (hourlyAccessLogLines.size() > 0) {
                        String filename = getFilename(dateString, hour, fileIndex);
                        //writeToFile(hourlyAccessLogLines, filename);
                        writeYyyyMmDdLineToFile(hourlyAccessLogLines, filename);
                    }
                    hourlyAccessLogLines.clear();
                }
            //}
            START_CALENDAR.add(Calendar.DATE, 1);

        }

    }

    private static String getFilename(String dateString, int hour, int fileIndex) {

        String folder = Utility.BASE_DEST_FOLDER;// + dateString + File.separator;
        File folderFile = new File(folder);
        if (!folderFile.exists()) {
            folderFile.mkdirs();
        }

        return folder + dateString + ".log";
        //return dateString + ".log";
        //return folder + "access_" + dateString + "_hr-" + getPaddedString(hour, 2, '0') + "-" + getPaddedString(fileIndex, 4, '0') + ".log";
        //return folder + "access.log";
    }

    private static String getPaddedString(String in, int len, char pad) {
        StringBuilder sb = new StringBuilder(in);
        while (sb.length() < len) {
            sb.insert(0, pad);
        }
        return sb.toString();
    }

    private static String getPaddedString(int in, int len, char pad) {
        return getPaddedString(String.valueOf(in), len, pad);
    }

    private static void writeToFile(Collection<TimedString> timedStrings, String logFile) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(logFile, true));
        for (TimedString ts : timedStrings) {
            out.write(ts.line);
            out.write("\n");
        }
        out.close();
    }

    private static void writeYyyyMmDdLineToFile(Collection<TimedString> timedStrings, String logFile) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(logFile, true));
        for (TimedString ts : timedStrings) {
            out.write(ts.getYyyyMmDdLine());
            out.write("\n");
        }
        out.close();
    }

    private static Collection<TimedString> getSessionRequests(int reqCount, long startMs, boolean canCrossHour) {
        Collection<TimedString> ts = new TreeSet<TimedString>();
        String ip = getIp();
        String userAgent = getUserAgent();
        String referrer = getReferer();

        Collection<Integer> itemIds = getItemIds(reqCount);
        if (!canCrossHour) {
            startMs = getSafeStartMs(startMs);
        }

        for (int itemId: itemIds) {
            long hitMs = Utility.getNextMsWithin(startMs, SESSION_LENGTH_IN_MS);
            ts.addAll(getItemReqTimedStrings(hitMs, AMAZON_PRODUCT_MAP.get(itemId).getAsin(), ip, userAgent, referrer));
        }
        return ts;
    }

    private static Collection<TimedString> getItemReqTimedStrings(long startMs, String asin, String ip, String userAgent, String referrer) {
        Set<TimedString> ts = new TreeSet<TimedString>();
        int resourceCount = getResourceCountPerRequest();
        // Add the cookie hash to the requests from the same ip
        Map<String, String> additioinalReqParameters = new HashMap<String, String>();
        additioinalReqParameters.put("cookieHash", String.valueOf(ip.hashCode()));
        Header productHeader = getProductReqHeader(asin, userAgent, additioinalReqParameters);

        productHeader.reqMap.put("Referer", referrer);
        ts.add(new TimedString(startMs, Utility.getAccessLogLine(ip, productHeader, startMs + RANDOM.nextInt(REQUEST_LOADING_LENGTH))));
        for (int i=0; i<resourceCount; i++) {
            long nowReqMs = startMs + RANDOM.nextInt(REQUEST_LOADING_LENGTH);
            int seed = RANDOM.nextInt(3);
            Header header = null;
            if (seed == 0) {
                header = getResourceHttpHeader(asin, CSS_URLS.get(RANDOM.nextInt(CSS_URLS.size())).getUrl(), userAgent, additioinalReqParameters);
            } else if (seed == 1) {
                header = getResourceHttpHeader(asin, IMAGE_URLS.get(RANDOM.nextInt(IMAGE_URLS.size())).getUrl(), userAgent, additioinalReqParameters);
            } else {
                header = getResourceHttpHeader(asin, JS_URLS.get(RANDOM.nextInt(JS_URLS.size())).getUrl(), userAgent, additioinalReqParameters);
            }
            ts.add(new TimedString(nowReqMs, Utility.getAccessLogLine(ip, header, nowReqMs)));
        }
        return ts;
    }

    private static String getReferer() {
        return REFERRER_BAG.drawBall();
    }

    private static long getSafeStartMs(long startMs) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(startMs));
        if (calendar.get(Calendar.MINUTE) > (60 - SESSION_LENGTH_IN_MINUTE)) {
            calendar.set(Calendar.MINUTE, (58 - SESSION_LENGTH_IN_MINUTE));
            return calendar.getTimeInMillis();
        }
        return startMs;
    }

    private static Collection<Integer> getItemIds(int size) {
        List<Integer> itemIds = new ArrayList<Integer>(size);
        for (int i=0; i<size; i++) {
            int categorId = Integer.parseInt(CATEGORY_ITEM_COUNT_BAG.drawBall());
            Bag categoryProductBag = CATEGORY_ITEM_BAG_MAP.get(categorId);
            int itemId = Integer.parseInt(categoryProductBag.drawBall());
            itemIds.add(itemId);
            if (CATEGORY_CONTINUE_BROWSE_FACTOR.get(categorId) != null &&
                    RANDOM.nextDouble() <= CATEGORY_CONTINUE_BROWSE_FACTOR.get(categorId)) {
                itemId = Integer.parseInt(categoryProductBag.drawBall());
                itemIds.add(itemId);
            }
        }
        return itemIds;
    }

    private static String getUserAgent() {
        List<String> userAgents = BROWSER_MAP.get(BROWSER_BAG.drawBall());
        return userAgents.get(RANDOM.nextInt(userAgents.size()));
    }

    private static String getIp() {
        String state = STATE_POPULATION_BAG.drawBall();
        TreeMap<Long, Long> stateIpRange = STATE_IP_RANGE_MAP.get(state);
        return IpGenerator.getIp(stateIpRange);
    }

    /**
     * Control how many product requests a session should have
     * @return number of request a session should have
     */
    private static int getSessionRequestCount() {
        return Utility.RANDOM.nextInt(10) + 5;
    }

    /**
     * Control how many css, js and image request a regular product request should have
     * @return number of resource requests
     */
    private static int getResourceCountPerRequest() {
        return Utility.RANDOM.nextInt(10) + 5;
    }

    private static String getParameterUrl(String url, Map<String, String> params, boolean questionMarkAdded) {
        StringBuilder sb = new StringBuilder(url);
        if (!questionMarkAdded) {
            sb.append("?");
        } else {
            sb.append("&");
        }
        for (Map.Entry<String, String> entry: params.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        // Remove the last "&"
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private static Header getResourceHttpHeader(String asin, String otherUrl, String userAgent, Map<String, String> params) {
        Header header = new Header();
        header.url = getParameterUrl(otherUrl, params, false);
        int lastSlash = otherUrl.lastIndexOf("/");
        header.resource = otherUrl.substring(lastSlash);
        header.method = "GET";
        header.httpStatusCode = 200;
        header.http = "HTTP/1.1";
        header.reqMap.put("Referer", getParameterUrl("http://www.incredibledeals.com/product/productDetails.jsp?PPSID=" + asin + "&parentPage=family", params, true));
        header.reqMap.put("User-Agent", userAgent);
        return header;
    }

    public static Header getProductReqHeader(String asin, String userAgent, Map<String, String> params) {
        Header header = new Header();
        header.url = getParameterUrl("http://www.incredibledeals.com/product/productDetails.jsp?PPSID=" + asin + "&parentPage=family", params, true);
        int lastSlash = header.url.lastIndexOf("/");
        header.resource = header.url.substring(lastSlash);
        header.method = "GET";
        header.httpStatusCode = 200;
        header.http = "HTTP/1.1";
        header.reqMap.put("User-Agent", userAgent);
        return header;
    }

    private static Bag getBrowserBag() {
        Bag bag = new Bag();
        bag.addBalls("MSIE", 343l);
        bag.addBalls("Firefox", 262l);
        bag.addBalls("Chrome", 222);
        bag.addBalls("Safari", 64);
        bag.addBalls("Opera", 24);
        bag.addBalls("Others", 70);
        bag.addBalls("Mobile", 15);
        return bag;
    }
}

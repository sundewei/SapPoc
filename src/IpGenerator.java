import org.apache.commons.csv.CSVUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: I827779
 * Date: 10/24/11
 * Time: 4:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class IpGenerator {
    private static String[] FIXED_IPS = new String[12];

    private static Random RANDOM = new Random();

    public static int FIXED_IP_SIZE = 0;

    public static TreeMap<Long, Long> US_IP_RANGE;

    static {
        FIXED_IPS[0] = "10.48.58.42";
        FIXED_IPS[1] = "10.48.101.113";
        FIXED_IPS[2] = "169.145.89.205";
        FIXED_IPS[3] = "74.125.225.81";
        FIXED_IPS[4] = "67.195.160.76";
        FIXED_IPS[5] = "198.93.34.21";
        FIXED_IPS[6] = "122.147.51.224";
        FIXED_IPS[7] = "64.208.126.34";
        FIXED_IPS[8] = "210.244.31.148";
        FIXED_IPS[9] = "64.208.126.49";
        FIXED_IPS[10] = "169.145.3.23";
        FIXED_IPS[11] = "74.125.227.4";

        FIXED_IP_SIZE = FIXED_IPS.length;

        try {
            US_IP_RANGE = getUsRange();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getUsIp() {
        String ip = null;
        int index = Utility.RANDOM.nextInt(1000);
        while (true) {
            if (Utility.RANDOM.nextInt(1000) < 7) {
                ip = FIXED_IPS[Math.abs(index % FIXED_IP_SIZE)];
            } else {
                ip = (RANDOM.nextInt(255) + 1) + "." + (RANDOM.nextInt(255) + 1) + "." + (RANDOM.nextInt(255) + 1) + "." + (RANDOM.nextInt(255) + 1);
            }
            if (isInRange(ip, US_IP_RANGE)) {
                return ip;
            }
        }
    }

    public static String getIp(TreeMap<Long, Long> map) {
        if (map == null) {
            map = US_IP_RANGE;
        }

        String ip = (RANDOM.nextInt(255) + 1) + "." + (RANDOM.nextInt(255) + 1) + "." + (RANDOM.nextInt(255) + 1) + "." + (RANDOM.nextInt(255) + 1);
        while (true) {
            if (isInRange(ip, map)) {
                return ip;
            }
            ip = (RANDOM.nextInt(255) + 1) + "." + (RANDOM.nextInt(255) + 1) + "." + (RANDOM.nextInt(255) + 1) + "." + (RANDOM.nextInt(255) + 1);
        }
    }

    public static boolean isInRange(String ip, TreeMap<Long, Long> map) {
        if (map == null) {
            return true;
        } else {
            long ipNum = Utility.getIpGeoNum(ip);
            if (map.containsKey(ipNum)) {
                return true;
            }
            Map.Entry<Long, Long> lowerEntry = map.floorEntry(ipNum);
            if (lowerEntry != null && ipNum >= lowerEntry.getValue()) {
                return true;
            }
            return false;
        }
    }

    private static TreeMap<Long, Long> getUsRange() throws Exception {
        InputStream in = Utility.class.getResourceAsStream("/resources/ipRange/usa.csv");
        List<String> lines = IOUtils.readLines(in);
        TreeMap<Long, Long> map = new TreeMap<Long, Long>();
        for (String line : lines) {
            String[] values = CSVUtils.parseLine(line);
            map.put(Long.parseLong(values[0]), Long.parseLong(values[1]));
        }
        in.close();
        return map;
    }


    public static Map<String, TreeMap<Long, Long>> getAllStateRangeMap() throws Exception {
        String baseName = "/resources/ipRange/";
        Map<String, TreeMap<Long, Long>> allMap = new HashMap<String, TreeMap<Long, Long>>();
        for (String state: Utility.STATES) {
            TreeMap<Long, Long> map = new TreeMap<Long, Long>();
            String resName = baseName + state + ".csv";
//System.out.println("Trying to open stream: " + resName);
            InputStream in = IpGenerator.class.getResourceAsStream(baseName + state + ".csv");
            if (in == null) {
//System.out.println(resName + " is null...skipping");
                continue;
            }
            List<String> lines = IOUtils.readLines(in);
            for (String line : lines) {
                String[] values = CSVUtils.parseLine(line);
                map.put(Long.parseLong(values[0]), Long.parseLong(values[1]));
            }
            allMap.put(state, map);
            in.close();
        }
        return allMap;
    }

    private static void writeMap(Map map, String filename) throws Exception {
        BufferedWriter out = new BufferedWriter(new FileWriter(filename));
        for (Object obj : map.entrySet()) {
            Map.Entry entry = (Map.Entry) obj;
            out.write("\"" + entry.getKey() + "\",\"" + entry.getValue() + "\"\n");
        }
        out.close();
    }

}

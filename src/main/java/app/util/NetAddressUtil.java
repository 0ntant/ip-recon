package app.util;


import java.util.concurrent.ThreadLocalRandom;

public class NetAddressUtil {

    private static final String IP_PATTERN =
            "^((25[0-5]|(2[0-4]|1\\d|[1-9]|\\d)\\d?)\\.){3}(25[0-5]|(2[0-4]|1\\d|[1-9]|\\d)\\d?)$";

    private static final String DOMAIN_PATTERN = "^([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$";

    public static boolean isIp(String address) {
        return address != null && address.matches(IP_PATTERN);
    }

    public static boolean isDomain(String addr) {
        return addr != null && addr.matches(DOMAIN_PATTERN);
    }

    public static String defang(String value) {
        if (isIp(value)) {
            String[] parts = value.split("\\.");
            return parts[0] + "." + parts[1] + "." + parts[2] + "[.]" + parts[3];
        }

        return value.replace(".", "[.]");
    }

    public static String cleanAddress(String input) {
        if (input == null) return null;

        return input
                .replaceAll("https?://", "")
                .replaceAll("[\\[\\]/]", "");
    }
}

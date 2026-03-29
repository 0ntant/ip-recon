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
            int dotToWrap = ThreadLocalRandom.current().nextInt(3);

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                sb.append(parts[i]);
                if (i < parts.length - 1) {
                    if (i == dotToWrap) {
                        sb.append("[.]");
                    } else {
                        sb.append(".");
                    }
                }
            }
            return sb.toString();
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

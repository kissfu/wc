package pub.utils;

/**
 * Created by able on 2017/4/14.
 */

public class StringUtil {
    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
}

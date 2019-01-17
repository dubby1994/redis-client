/**
 * @author daoxuan
 * @date 2018/12/4 11:13
 */
package cn.dubby.redis.client.util;

public class StringUtil {

    public static boolean isEmpty(String str) {
        return str == null || str.trim().length() == 0;
    }

}

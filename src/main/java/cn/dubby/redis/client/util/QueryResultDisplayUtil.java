package cn.dubby.redis.client.util;

import javafx.scene.control.TextArea;

/**
 * @author dubby
 * @date 2020/3/20 15:22
 */
public class QueryResultDisplayUtil {

    private static final int MAX_DISPLAY_LENGTH = 10000;

    public static void display(TextArea queryResult, String content) {

        queryResult.setText(content);

//        String originContent = queryResult.getText();
//        String newContent = originContent + System.lineSeparator() + content;
//        int length = newContent.length();
//        if (length > MAX_DISPLAY_LENGTH) {
//            queryResult.setText(newContent.substring(length - 10000, length));
//        } else {
//            queryResult.setText(newContent);
//        }
//
//        queryResult.setScrollTop(Double.MAX_VALUE);
    }

}

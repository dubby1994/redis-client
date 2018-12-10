/**
 * @author daoxuan
 * @date 2018/12/4 11:24
 */
package sample.util;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LogUtil {

    public static final Logger logger = Logger.getGlobal();

    static {
        FileHandler fileHandler = null;

        try {
            fileHandler = new FileHandler("D:\\log\\java-fx.log", 0, 1, false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        fileHandler.setLevel(Level.INFO);
        fileHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(fileHandler);
        logger.setLevel(Level.INFO);
    }

}

package cn.dubby.redis.client.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.stream.Collectors;

/**
 * @author dubby
 * @date 2019/1/17 19:38
 */
public class FileUtil {

    private static Logger logger = LoggerFactory.getLogger(FileUtil.class);

    private static final String URI_FILE = "conf/uri.txt";

    private static final String CMD_FILE = "conf/cmd.txt";

    private static final String DEFAULT_URI = "redis://:password@127.0.0.1:6379/0";

    public static void saveRedisURI(String uri) {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(new File(URI_FILE));
            fileWriter.write(uri);
        } catch (IOException e) {
            logger.error("saveRedisURI uri:{}", uri, e);
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.flush();
                } catch (IOException e) {
                    logger.error("saveRedisURI uri:{}", uri, e);
                }
            }
        }
    }

    public static String readRedisURI() {
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(new File(URI_FILE));
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            return bufferedReader.readLine();
        } catch (IOException e) {
            logger.error("readRedisURI", e);
            return DEFAULT_URI;
        } finally {
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (IOException e) {
                    logger.error("readRedisURI", e);
                }
            }
        }
    }

    public static void saveRedisCMD(String cmd) {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(new File(CMD_FILE));
            fileWriter.write(cmd);
        } catch (IOException e) {
            logger.error("saveRedisCMD cmd:{}", cmd, e);
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.flush();
                } catch (IOException e) {
                    logger.error("saveRedisCMD cmd:{}", cmd, e);
                }
            }
        }
    }

    public static String readRedisCMD() {
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(new File(CMD_FILE));
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            return bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            logger.error("readRedisCMD", e);
            return DEFAULT_URI;
        } finally {
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (IOException e) {
                    logger.error("readRedisCMD", e);
                }
            }
        }
    }

}

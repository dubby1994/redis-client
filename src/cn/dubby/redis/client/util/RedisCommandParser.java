package cn.dubby.redis.client.util;

import cn.dubby.redis.client.constant.RESPConstant;
import cn.dubby.redis.client.constant.RedisCommandConstant;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidParameterException;

/**
 * @author dubby
 * @date 2018/12/10 13:52
 */
public class RedisCommandParser {

    private static final Charset charset = Charset.forName("UTF-8");

    public static byte[] parse(String inputCommand) throws IOException {
        if (StringUtil.isEmpty(inputCommand)) {
            inputCommand = RedisCommandConstant.PING;
        }

        inputCommand = inputCommand.trim();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(RESPConstant.ASTERISK_BYTE);

        String[] cmdSplit = inputCommand.split(" ");
        int length = 0;
        for (String str : cmdSplit) {
            if (!StringUtil.isEmpty(str)) {
                length++;
            }
        }
        stream.write(intToByte(length));
        stream.write('\r');
        stream.write('\n');

        for (String str : cmdSplit) {
            if (StringUtil.isEmpty(str)) {
                continue;
            }
            stream.write(RESPConstant.DOLLAR_BYTE);
            stream.write(intToByte(str.getBytes(charset).length));
            stream.write('\r');
            stream.write('\n');
            stream.write(str.getBytes(charset));
            stream.write('\r');
            stream.write('\n');
        }

        return stream.toByteArray();
    }


    private final static byte[] Digit = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};


    private static byte[] intToByte(int i) {
        if (i <= 0) {
            return new byte[0];
        }
        if (i < 10) {
            byte[] bytes = new byte[1];
            bytes[0] = Digit[i];
            return bytes;
        }
        if (i < 100) {
            byte[] bytes = new byte[2];
            bytes[0] = Digit[i / 10];
            bytes[1] = Digit[i % 10];
            return bytes;
        }
        if (i < 1000) {
            byte[] bytes = new byte[3];
            bytes[0] = Digit[i / 100];
            i = i % 100;
            bytes[1] = Digit[i / 10];
            bytes[2] = Digit[i % 10];
            return bytes;
        }
        if (i < 10000) {
            byte[] bytes = new byte[4];
            bytes[0] = Digit[i / 1000];
            i = i % 1000;
            bytes[1] = Digit[i / 100];
            i = i % 100;
            bytes[2] = Digit[i / 10];
            bytes[3] = Digit[i % 10];
            return bytes;
        }
        throw new InvalidParameterException("redis command too long");
    }

}

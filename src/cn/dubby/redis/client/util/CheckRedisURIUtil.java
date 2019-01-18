package cn.dubby.redis.client.util;

import cn.dubby.redis.client.constant.RedisQueryConstant;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * @author dubby
 * @date 2019/1/18 11:47
 */
public class CheckRedisURIUtil {

    private static Logger logger = LoggerFactory.getLogger(CheckRedisURIUtil.class);

    public static boolean checkRedisURI(String host, int port, String password, int dbIndex) throws IOException {
        Socket socket = new Socket();
        socket.setReuseAddress(true);
        socket.setKeepAlive(true);
        socket.setTcpNoDelay(true);
        socket.setSoLinger(true, 0);
        socket.connect(new InetSocketAddress(host, port), 10 * 1000);
        socket.setSoTimeout(10 * 1000);


        OutputStream outputStream = socket.getOutputStream();
        InputStream inputStream = socket.getInputStream();
        byte[] byteBuffer = new byte[RedisQueryConstant.UNIT_SIZE];
        //AUTH password
        int length = 0;
        if (!StringUtil.isEmpty(password)) {
            String authCmd = "AUTH " + password;
            byte[] authCmdBytes = RedisCommandParser.parse(authCmd);
            outputStream.write(authCmdBytes);
            outputStream.flush();


            length = inputStream.read(byteBuffer);
            String authResult = new String(byteBuffer, 0, length, CharsetUtil.UTF_8);
            logger.info(authResult);
        }

        //SELECT dbIndex
        String selectCmd = "SELECT " + dbIndex;
        byte[] selectCmdBytes = RedisCommandParser.parse(selectCmd);
        outputStream.write(selectCmdBytes);
        outputStream.flush();

        byteBuffer = new byte[RedisQueryConstant.UNIT_SIZE];
        length = inputStream.read(byteBuffer);
        String selectResult = new String(byteBuffer, 0, length, CharsetUtil.UTF_8);
        logger.info(selectResult);

        //PING
        String pingCmd = "PING";
        byte[] pingCmdBytes = RedisCommandParser.parse(pingCmd);
        outputStream.write(pingCmdBytes);
        outputStream.flush();

        byteBuffer = new byte[RedisQueryConstant.UNIT_SIZE];
        length = inputStream.read(byteBuffer);
        String result = new String(byteBuffer, 0, length, CharsetUtil.UTF_8);
        logger.info(result);

        outputStream.close();
        inputStream.close();

        return !StringUtil.isEmpty(result);
    }

}

/**
 * @author daoxuan
 * @date 2018/12/4 14:09
 */
package sample.service;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import sample.constant.RedisCommandConstant;
import sample.context.RedisConnectionStatus;
import sample.util.LogUtil;
import sample.util.RedisCommandParser;
import sample.util.RedisURIHelper;
import sample.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.logging.Logger;

public class RedisOperationService {

    private static final Logger logger = LogUtil.logger;

    private static final Charset charset = Charset.forName("UTF-8");

    private String host;
    private int port;
    private String password;
    private int dbIndex;

    private static final int UNIT_SIZE = 500 * 1024 * 1024;

    public RedisOperationService(URI uri) {
        host = uri.getHost();
        port = uri.getPort();
        password = RedisURIHelper.getPassword(uri);
        dbIndex = RedisURIHelper.getDBIndex(uri);
    }

    public void checkRedisURI(RedisConnectionStatus redidConnectStatusOutput) throws IOException {
        new Thread(() -> {
            try {
                if (doCheckRedisURI()) {
                    redidConnectStatusOutput.setConnected(true);
                }
            } catch (IOException e) {
                logger.severe(String.format("doCheckRedisURI %s", e.toString()));
            }
        }).start();
    }

    private boolean doCheckRedisURI() throws IOException {
        Socket socket = new Socket();
        socket.setReuseAddress(true);
        socket.setKeepAlive(true);
        socket.setTcpNoDelay(true);
        socket.setSoLinger(true, 0);
        socket.connect(new InetSocketAddress(host, port), 10 * 1000);
        socket.setSoTimeout(10 * 1000);


        OutputStream outputStream = socket.getOutputStream();
        InputStream inputStream = socket.getInputStream();
        byte[] byteBuffer = new byte[UNIT_SIZE];
        //AUTH password
        int length = 0;
        if (!StringUtil.isEmpty(password)) {
            String authCmd = "AUTH " + password;
            byte[] authCmdBytes = RedisCommandParser.parse(authCmd);
            outputStream.write(authCmdBytes);
            outputStream.flush();


            length = inputStream.read(byteBuffer);
            String authResult = new String(byteBuffer, 0, length, charset);
            logger.info(authResult);
        }

        //SELECT dbIndex
        String selectCmd = "SELECT " + dbIndex;
        byte[] selectCmdBytes = RedisCommandParser.parse(selectCmd);
        outputStream.write(selectCmdBytes);
        outputStream.flush();

        byteBuffer = new byte[UNIT_SIZE];
        length = inputStream.read(byteBuffer);
        String selectResult = new String(byteBuffer, 0, length, charset);
        logger.info(selectResult);

        //PING
        String pingCmd = "PING";
        byte[] pingCmdBytes = RedisCommandParser.parse(pingCmd);
        outputStream.write(pingCmdBytes);
        outputStream.flush();

        byteBuffer = new byte[UNIT_SIZE];
        length = inputStream.read(byteBuffer);
        String result = new String(byteBuffer, 0, length, charset);
        logger.info(result);

        outputStream.close();
        inputStream.close();

        return !StringUtil.isEmpty(result);
    }

    public void query(String command, TextArea queryResult) {
        //处理注释
        new Thread(() -> {
            try {
                String result = doQuery(transformCommand(command));
                Platform.runLater(() -> {
                    queryResult.setText(result);
                });
            } catch (Exception e) {
                logger.severe(String.format("query %s", e.toString()));
            }
        }).start();
    }

    /**
     * 过滤掉注释
     */
    private String transformCommand(String command) {
        if (StringUtil.isEmpty(command)) {
            return "INFO";
        }

        StringBuilder sb = new StringBuilder();

        String[] strings = command.split("\n");
        int i = 0;
        for (String str : strings) {
            if (str.startsWith(RedisCommandConstant.COMMENT)) {
                continue;
            }
            if (i == 0) {
                sb.append(str);
            } else {
                sb.append(str);
                sb.append("\n");
            }
            ++i;
        }

        return sb.toString();
    }

    private String doQuery(String command) throws IOException {
        Socket socket = new Socket();
        socket.setReuseAddress(true);
        socket.setKeepAlive(true);
        socket.setTcpNoDelay(true);
        socket.setSoLinger(true, 0);
        socket.connect(new InetSocketAddress(host, port), 10 * 1000);
        socket.setSoTimeout(30 * 1000);


        OutputStream outputStream = socket.getOutputStream();
        InputStream inputStream = socket.getInputStream();

        //AUTH password
        String authCmd = "AUTH " + password;
        byte[] authCmdBytes = RedisCommandParser.parse(authCmd);
        outputStream.write(authCmdBytes);
        outputStream.flush();

        byte[] byteBuffer = new byte[UNIT_SIZE];
        int length = inputStream.read(byteBuffer);
        String authResult = new String(byteBuffer, 0, length, charset);
        logger.info(authResult);

        //SELECT dbIndex
        String selectCmd = "SELECT " + dbIndex;
        byte[] selectCmdBytes = RedisCommandParser.parse(selectCmd);
        outputStream.write(selectCmdBytes);
        outputStream.flush();

        byteBuffer = new byte[UNIT_SIZE];
        length = inputStream.read(byteBuffer);
        String selectResult = new String(byteBuffer, 0, length, charset);
        logger.info(selectResult);

        //COMMAND
        byte[] commandBytes = RedisCommandParser.parse(command);
        outputStream.write(commandBytes);
        outputStream.flush();

        ByteOutputStream byteOutputStream = new ByteOutputStream();
        while (true) {
            byteBuffer = new byte[UNIT_SIZE];
            length = inputStream.read(byteBuffer);
            if (length == -1) {
                break;
            }
            byteOutputStream.write(byteBuffer, 0, length);
            if (length < UNIT_SIZE) {
                break;
            }
        }

        String result = byteOutputStream.toString();
        logger.info(String.format("\ncommand:\n%s\nresult:\n%s", new String(commandBytes, charset), result));

        outputStream.close();
        inputStream.close();

        return result;
    }
}

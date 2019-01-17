package cn.dubby.redis.client.service;

import cn.dubby.redis.client.constant.RedisCommandConstant;
import cn.dubby.redis.client.context.RedisConnectionStatus;
import cn.dubby.redis.client.util.RedisCommandParser;
import cn.dubby.redis.client.util.RedisURIHelper;
import cn.dubby.redis.client.util.StringUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.redis.RedisArrayAggregator;
import io.netty.handler.codec.redis.RedisBulkStringAggregator;
import io.netty.handler.codec.redis.RedisDecoder;
import io.netty.handler.codec.redis.RedisEncoder;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GenericFutureListener;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class RedisOperationService {

    private static final Logger logger = LoggerFactory.getLogger(RedisOperationService.class);

    private static final int UNIT_SIZE = 1024 * 1024;

    private static final ExecutorService executorService = Executors.newFixedThreadPool(1, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("RedisCommandExecutor");
            return thread;
        }
    });

    private String host;

    private int port;

    private String password;

    private int dbIndex;

    private RedisConnectionStatus connectionStatus;

    private TextArea queryResult;

    private EventLoopGroup eventExecutors;

    private Channel channel;

    public RedisOperationService(URI uri, RedisConnectionStatus connectionStatus, TextArea queryResult) {
        host = uri.getHost();
        port = uri.getPort();
        password = RedisURIHelper.getPassword(uri);
        dbIndex = RedisURIHelper.getDBIndex(uri);
        this.connectionStatus = connectionStatus;
        this.queryResult = queryResult;
    }

    public void destroy() {
        if (eventExecutors != null) {
            eventExecutors.shutdownGracefully();
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    public void connect() {
        new Thread(() -> {
            try {
                if (doCheckRedisURI()) {
                    doConnect();
                    connectionStatus.setConnected(true);
                }
            } catch (Exception e) {
                logger.error("doCheckRedisURI", e);
            }
        }).start();
    }

    public void disconnect() {
        eventExecutors.shutdownGracefully();
        connectionStatus.setConnected(false);
    }

    public void query(String command) {
        executorService.submit(() -> {
            try {
                String realCmd = transformCommand(command);
                ChannelFuture lastWriteFuture = channel.writeAndFlush(realCmd);
                lastWriteFuture.addListener((GenericFutureListener<ChannelFuture>) future -> {
                    if (!future.isSuccess()) {
                        logger.error("query error");
                        Platform.runLater(() -> {
                            queryResult.setText("command query error");
                        });
                    }
                });
                logger.info("execute:{}", realCmd);
            } catch (Exception e) {
                logger.error("query command:{}", command, e);
            }
        });
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
            String authResult = new String(byteBuffer, 0, length, CharsetUtil.UTF_8);
            logger.info(authResult);
        }

        //SELECT dbIndex
        String selectCmd = "SELECT " + dbIndex;
        byte[] selectCmdBytes = RedisCommandParser.parse(selectCmd);
        outputStream.write(selectCmdBytes);
        outputStream.flush();

        byteBuffer = new byte[UNIT_SIZE];
        length = inputStream.read(byteBuffer);
        String selectResult = new String(byteBuffer, 0, length, CharsetUtil.UTF_8);
        logger.info(selectResult);

        //PING
        String pingCmd = "PING";
        byte[] pingCmdBytes = RedisCommandParser.parse(pingCmd);
        outputStream.write(pingCmdBytes);
        outputStream.flush();

        byteBuffer = new byte[UNIT_SIZE];
        length = inputStream.read(byteBuffer);
        String result = new String(byteBuffer, 0, length, CharsetUtil.UTF_8);
        logger.info(result);

        outputStream.close();
        inputStream.close();

        return !StringUtil.isEmpty(result);
    }

    private void doConnect() throws InterruptedException {
        eventExecutors = new NioEventLoopGroup(1);

        Bootstrap b = new Bootstrap();
        b.group(eventExecutors)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new RedisDecoder());
                        p.addLast(new RedisBulkStringAggregator());
                        p.addLast(new RedisArrayAggregator());
                        p.addLast(new RedisEncoder());
                        p.addLast(new RedisClientHandler(queryResult));
                    }
                });
        b.option(ChannelOption.TCP_NODELAY, true);

        channel = b.connect(host, port).sync().channel();
        if (StringUtil.isEmpty(password)) {
            query("INFO");
        } else {
            query("AUTH " + password);
        }
    }

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
}

package cn.dubby.redis.client.service;

import cn.dubby.redis.client.constant.RedisQueryConstant;
import cn.dubby.redis.client.context.RedisConnectionStatus;
import cn.dubby.redis.client.util.CheckRedisURIUtil;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static cn.dubby.redis.client.util.RedisCommandParser.transformCommand;

public class RedisOperationService {

    private static final Logger logger = LoggerFactory.getLogger(RedisOperationService.class);

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

    RedisConnectionStatus connectionStatus;

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
        executorService.shutdownNow();
    }

    public void connect() {
        new Thread(() -> {
            try {
                if (CheckRedisURIUtil.checkRedisURI(host, port, password, dbIndex)) {
                    if (connectionStatus.isKeepAlive()) {
                        doConnectWithNetty();
                    } else {
                        query("INFO");
                    }
                    connectionStatus.setConnected(true);
                }
            } catch (Exception e) {
                logger.error("doCheckRedisURI", e);
            }
        }).start();
    }

    public void disconnect() {
        if (eventExecutors!=null) {
            eventExecutors.shutdownGracefully();
        }
        connectionStatus.setConnected(false);
    }

    public void query(String cmd) {
        final String realCommand = transformCommand(cmd);
        if (connectionStatus.isKeepAlive()) {
            executorService.submit(() -> {
                try {
                    ChannelFuture lastWriteFuture = channel.writeAndFlush(realCommand);
                    lastWriteFuture.addListener((GenericFutureListener<ChannelFuture>) future -> {
                        if (!future.isSuccess()) {
                            logger.error("query command:{}", realCommand);
                            Platform.runLater(() -> {
                                queryResult.setText("query command error:" + realCommand);
                            });
                        }
                    });
                    logger.info("query command:{}", realCommand);
                } catch (Exception e) {
                    logger.error("query command:{}", realCommand, e);
                }
            });
        } else {
            executorService.submit(() -> {
                try {
                    String result = doQuery(realCommand);
                    Platform.runLater(() -> {
                        queryResult.setText(result);
                    });
                } catch (Exception e) {
                    logger.error("query command:{}", realCommand, e);
                }
            });
        }
    }

    private String doQuery(String command) throws IOException {
        Socket socket = new Socket();
        socket.setReuseAddress(true);
        socket.setKeepAlive(true);
        socket.setTcpNoDelay(true);
        socket.setSoLinger(true, 0);
        socket.connect(new InetSocketAddress(host, port), 10 * 1000);
        socket.setSoTimeout(10 * 1000);


        OutputStream outputStream = socket.getOutputStream();
        InputStream inputStream = socket.getInputStream();

        //AUTH password
        String authCmd = "AUTH " + password;
        byte[] authCmdBytes = RedisCommandParser.parse(authCmd);
        outputStream.write(authCmdBytes);
        outputStream.flush();

        byte[] byteBuffer = new byte[RedisQueryConstant.UNIT_SIZE];
        int length = inputStream.read(byteBuffer);
        String authResult = new String(byteBuffer, 0, length, CharsetUtil.UTF_8);
        logger.info(authResult);

        //SELECT dbIndex
        String selectCmd = "SELECT " + dbIndex;
        byte[] selectCmdBytes = RedisCommandParser.parse(selectCmd);
        outputStream.write(selectCmdBytes);
        outputStream.flush();

        byteBuffer = new byte[RedisQueryConstant.UNIT_SIZE];
        length = inputStream.read(byteBuffer);
        String selectResult = new String(byteBuffer, 0, length, CharsetUtil.UTF_8);
        logger.info(selectResult);

        //COMMAND
        byte[] commandBytes = RedisCommandParser.parse(command);
        outputStream.write(commandBytes);
        outputStream.flush();

        byteBuffer = new byte[RedisQueryConstant.UNIT_SIZE];
        length = inputStream.read(byteBuffer);
        String result = new String(byteBuffer, 0, length, CharsetUtil.UTF_8);
        logger.info(String.format("\ncommand:\n%s\nresult:\n%s", new String(commandBytes, CharsetUtil.UTF_8), result));

        outputStream.close();
        inputStream.close();

        return result;
    }

    private void doConnectWithNetty() throws InterruptedException, ExecutionException {
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
                        p.addLast(new RedisClientHandler(queryResult, RedisOperationService.this));
                    }
                });
        b.option(ChannelOption.TCP_NODELAY, true);
        b.option(ChannelOption.SO_KEEPALIVE, true);

        channel = b.connect(host, port).sync().channel();
        if (StringUtil.isEmpty(password)) {
            query("INFO");
        } else {
            query("AUTH " + password);
        }
        query("SELECT " + dbIndex);
    }
}

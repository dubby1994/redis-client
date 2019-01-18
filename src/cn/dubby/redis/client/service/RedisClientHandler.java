package cn.dubby.redis.client.service;

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.redis.ArrayRedisMessage;
import io.netty.handler.codec.redis.ErrorRedisMessage;
import io.netty.handler.codec.redis.FullBulkStringRedisMessage;
import io.netty.handler.codec.redis.IntegerRedisMessage;
import io.netty.handler.codec.redis.RedisMessage;
import io.netty.handler.codec.redis.SimpleStringRedisMessage;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class RedisClientHandler extends ChannelDuplexHandler {

    private Logger logger = LoggerFactory.getLogger(RedisClientHandler.class);

    private TextArea queryResult;

    private RedisOperationService redisOperationService;

    public RedisClientHandler(TextArea queryResult, RedisOperationService redisOperationService) {
        this.queryResult = queryResult;
        this.redisOperationService = redisOperationService;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        String[] commands = ((String) msg).split("\\s+");
        List<RedisMessage> children = new ArrayList<RedisMessage>(commands.length);
        for (String cmdString : commands) {
            children.add(new FullBulkStringRedisMessage(ByteBufUtil.writeUtf8(ctx.alloc(), cmdString)));
        }
        RedisMessage request = new ArrayRedisMessage(children);
        ctx.writeAndFlush(request, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        RedisMessage redisMessage = (RedisMessage) msg;
        StringBuilder sb = new StringBuilder();
        printAggregatedRedisResponse(redisMessage, sb);
        Platform.runLater(() -> {
            String result = sb.toString();
            logger.info("result:{}", result);
            queryResult.setText(result);
        });
        ReferenceCountUtil.release(redisMessage);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Platform.runLater(() -> {
            queryResult.setText(cause.getMessage());
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (redisOperationService.connectionStatus.isConnected()) {
            Platform.runLater(() -> {
                queryResult.setText("哦哦，我们和Redis的连接断开了，请检查网络是否不稳定，10s后我们也会尝试帮你重连");
            });
            final EventLoop eventLoop = ctx.channel().eventLoop();
            eventLoop.schedule(() -> {
                try {
                    redisOperationService.connect();
                    logger.error("reconnect success");
                    Platform.runLater(() -> {
                        queryResult.setText("刚才和Redis的连接断开了，不过我们帮你重连了");
                    });
                } catch (Exception e) {
                    logger.error("reconnect fail", e);
                }
            }, 10L, TimeUnit.SECONDS);
        }
        super.channelInactive(ctx);
    }

    private static void printAggregatedRedisResponse(RedisMessage msg, StringBuilder sb) {
        if (msg instanceof SimpleStringRedisMessage) {
            sb.append(((SimpleStringRedisMessage) msg).content());
        } else if (msg instanceof ErrorRedisMessage) {
            sb.append(((ErrorRedisMessage) msg).content());
        } else if (msg instanceof IntegerRedisMessage) {
            sb.append(((IntegerRedisMessage) msg).value());
        } else if (msg instanceof FullBulkStringRedisMessage) {
            sb.append(getString((FullBulkStringRedisMessage) msg));
        } else if (msg instanceof ArrayRedisMessage) {
            for (RedisMessage child : ((ArrayRedisMessage) msg).children()) {
                printAggregatedRedisResponse(child, sb);
            }
        } else {
            throw new CodecException("unknown message type: " + msg);
        }
        sb.append(System.lineSeparator());
    }

    private static String getString(FullBulkStringRedisMessage msg) {
        if (msg.isNull()) {
            return "(null)";
        }
        return msg.content().toString(CharsetUtil.UTF_8);
    }
}

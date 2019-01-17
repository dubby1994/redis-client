package cn.dubby.redis.client.service;

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
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

public class RedisClientHandler extends ChannelDuplexHandler {

    private Logger logger = LoggerFactory.getLogger(RedisClientHandler.class);

    private TextArea queryResult;

    public RedisClientHandler(TextArea queryResult) {
        this.queryResult = queryResult;
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

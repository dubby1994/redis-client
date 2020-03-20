package cn.dubby.redis.client.context;

/**
 * @author dubby
 * @date 2018/12/10 14:06
 */
public class RedisConnectionStatus {

    private boolean connected = false;

    private String uri;

    private boolean keepAlive;

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public synchronized boolean isConnected() {
        return connected;
    }

    public synchronized void setConnected(boolean connected) {
        this.connected = connected;
    }

    public synchronized String getUri() {
        return uri;
    }

    public synchronized void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public String toString() {
        return "RedisConnectionStatus{" +
                "connected=" + connected +
                ", uri='" + uri + '\'' +
                ", keepAlive=" + keepAlive +
                '}';
    }
}

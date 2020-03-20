package cn.dubby.redis.client;

import cn.dubby.redis.client.context.RedisConnectionStatus;
import cn.dubby.redis.client.service.RedisOperationService;
import cn.dubby.redis.client.util.FileUtil;
import cn.dubby.redis.client.util.StringUtil;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Controller {

    private static final Logger logger = LoggerFactory.getLogger(Controller.class);

    /**
     * redis://:password@127.0.0.1:6379/0
     */
    @FXML
    private TextField redisURIInput;

    @FXML
    private TextArea commandInput;

    @FXML
    private TextArea queryResult;

    @FXML
    private Button connectBtn;

    @FXML
    private Button queryBtn;

    @FXML
    private CheckBox keepAliveCheckBox;

    private Semaphore semaphore = new Semaphore(1);

    private final RedisConnectionStatus connectionStatus = new RedisConnectionStatus();

    private RedisOperationService redisOperationService;

    private EventHandler<KeyEvent> queryEventHandler = event -> {
        if ("Q".equals(event.getText()) || "q".equals(event.getText())) {
            doQuery();
        }
    };

    private static final ScheduledExecutorService refreshStatusExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName("refreshStatusThread");
        return thread;
    });

    public void init() {
        refreshStatusExecutor.scheduleAtFixedRate(() -> {
            if (connectionStatus.isConnected()) {
                Platform.runLater(() -> {
                    redisURIInput.setDisable(true);
                    connectBtn.setText("断开连接");
                    queryBtn.setDisable(false);
                    keepAliveCheckBox.setDisable(true);
                });
            } else {
                Platform.runLater(() -> {
                    redisURIInput.setDisable(false);
                    connectBtn.setText("连接");
                    queryBtn.setDisable(true);
                    keepAliveCheckBox.setDisable(false);
                });
            }
            FileUtil.saveRedisCMD(commandInput.getText());
        }, 100, 100, TimeUnit.MILLISECONDS);
        redisURIInput.setText(FileUtil.readRedisURI());
        commandInput.setText(FileUtil.readRedisCMD());
    }

    public void destroy() {
        disconnect();
        FileUtil.saveRedisCMD(commandInput.getText());
        if (redisOperationService != null) {
            redisOperationService.destroy();
        }
        if (refreshStatusExecutor != null) {
            refreshStatusExecutor.shutdownNow();
        }
    }

    @FXML
    protected void handleConnectButtonAction(ActionEvent event) {
        if (connectionStatus.isConnected()) {
            disconnect();
        } else {
            connect();
        }
    }

    @FXML
    protected void handleQueryButtonAction(ActionEvent event) {
        doQuery();
    }

    private void doQuery() {
        if (!semaphore.tryAcquire()) {
            return;
        }

        try {
            queryBtn.setDisable(true);
            queryBtn.setText("Loading...");
            if (redisOperationService == null) {
                return;
            }
            redisOperationService.query(commandInput.getText());
        } catch (Exception e) {
            logger.error("doQuery", e);
        } finally {
            queryBtn.setDisable(false);
            queryBtn.setText("查询");
            semaphore.release();
        }
    }

    private void connect() {
        String redisURI = redisURIInput.getText();
        if (StringUtil.isEmpty(redisURI)) {
            queryResult.setText("我劝你善良，不填URI，我连啥啊？");
            return;
        }

        logger.info("try to connect:{}", redisURI.trim());
        try {
            URI uri = new URI(redisURI.trim());
            connectionStatus.setUri(redisURI.trim());
            connectionStatus.setKeepAlive(keepAliveCheckBox.isSelected());
            redisOperationService = new RedisOperationService(uri, connectionStatus, queryResult);
            redisOperationService.connect();
            commandInput.setOnKeyPressed(queryEventHandler);
            FileUtil.saveRedisURI(redisURI.trim());
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    private void disconnect() {
        if (redisOperationService != null) {
            redisOperationService.disconnect();
        }
        commandInput.setOnKeyPressed(queryEventHandler);
    }

}

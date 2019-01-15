package sample;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import sample.context.RedisConnectionStatus;
import sample.service.RedisOperationService;
import sample.util.LogUtil;
import sample.util.StringUtil;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Controller {

    private static final Logger logger = LogUtil.logger;

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

    private final RedisConnectionStatus connectionStatus = new RedisConnectionStatus();

    private RedisOperationService redisOperationService;

    private static final ScheduledExecutorService refreshStatusExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName("refreshStatusThread");
        return thread;
    });

    public Controller() {
        refreshStatusExecutor.scheduleAtFixedRate(() -> {
            if (connectionStatus.isConnected()) {
                Platform.runLater(() -> {
                    redisURIInput.setDisable(true);
                    connectBtn.setText("断开连接");
                });
            } else {
                Platform.runLater(() -> {
                    redisURIInput.setDisable(false);
                    connectBtn.setText("连接");
                });
            }
        }, 100, 100, TimeUnit.MILLISECONDS);
    }

    @FXML
    protected void handleConnectButtonAction(ActionEvent event) {
        if (connectionStatus.isConnected()) {
            disconnect();
        } else {
            connect();
        }
        commandInput.setOnKeyTyped(e -> {
            if (e.isControlDown()) {
                doQuery();
            }
        });
    }

    @FXML
    protected void handleQueryButtonAction(ActionEvent event) {
        doQuery();
    }

    private void doQuery() {
        if (redisOperationService == null) {
            return;
        }
        redisOperationService.query(commandInput.getText(), queryResult);
        System.gc();
    }

    private void connect() {
        String redisURI = redisURIInput.getText();
        if (StringUtil.isEmpty(redisURI)) {
            queryResult.setText("我劝你善良，不填URI，我连啥啊？");
            return;
        }

        try {
            URI uri = new URI(redisURI.trim());
            connectionStatus.setUri(redisURI.trim());
            redisOperationService = new RedisOperationService(uri);
            redisOperationService.checkRedisURI(connectionStatus);
            redisOperationService.query("INFO", queryResult);
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
    }

    private void disconnect() {
        connectionStatus.setConnected(false);
    }

}

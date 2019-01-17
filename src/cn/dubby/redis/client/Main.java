package cn.dubby.redis.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

public class Main extends Application {

    private static Logger logger = LoggerFactory.getLogger(Main.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        URL location = getClass().getResource("sample.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(location);
        fxmlLoader.setBuilderFactory(new JavaFXBuilderFactory());
        Parent root = fxmlLoader.load();//FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("Redis Manager");
        primaryStage.setScene(new Scene(root, 1000, 800));
        primaryStage.getIcons().add(new Image(Main.class.getResourceAsStream("/redis.png")));
        primaryStage.show();

        //init
        Controller controller = fxmlLoader.getController();
        controller.init();

        primaryStage.setOnCloseRequest(event -> {
            controller.destroy();
            logger.info("===================================================================application end===================================================================");
        });
    }


    public static void main(String[] args) {
        logger.info("===================================================================application started===================================================================");
        launch(args);
    }
}

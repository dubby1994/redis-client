package cn.dubby.redis.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

public class Main extends Application {

    private static Logger logger = LoggerFactory.getLogger(Main.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        URL location = getClass().getResource("/sample.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(location);
        fxmlLoader.setBuilderFactory(new JavaFXBuilderFactory());
        Parent root = fxmlLoader.load();//FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("Redis Client");
        primaryStage.setScene(new Scene(root, 1000, 900));
        primaryStage.getIcons().add(new Image(Main.class.getResourceAsStream("/redis.png")));
        primaryStage.show();

        //init
        Controller controller = fxmlLoader.getController();
        controller.init();

        primaryStage.setOnCloseRequest(event -> {
            controller.destroy();
            logger.info("======application end======");
        });
    }


    public static void main(String[] args) throws IOException {
        logger.info("======application started======");
        initConfig();
        System.out.println("------");
        System.out.println(Font.getFamilies());
        System.out.println(Font.getFontNames());
        System.out.println("------");
        launch(args);
    }

    private static void initConfig() throws IOException {
        File config = new File("conf");
        if (!config.exists()) {
            boolean mkDirs = config.mkdirs();
            logger.info("conf dir create result:{}", mkDirs);
        }

        File cmd = new File("conf/cmd.txt");
        File uri = new File("conf/uri.txt");
        if (!cmd.exists()) {
            boolean createResult = cmd.createNewFile();
            if (createResult) {
                FileWriter fileWriter = new FileWriter(cmd);
                fileWriter.write("info");
                fileWriter.flush();
                fileWriter.close();
            }
        }
        if (!uri.exists()) {
            boolean createResult = uri.createNewFile();
            if (createResult) {
                FileWriter fileWriter = new FileWriter(uri);
                fileWriter.write("redis://:password@127.0.0.1:6379/0");
                fileWriter.flush();
                fileWriter.close();
            }
        }
    }
}

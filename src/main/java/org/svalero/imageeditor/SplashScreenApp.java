package org.svalero.imageeditor;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class SplashScreenApp extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        Stage splashStage = new Stage();
        VBox splashLayout = new VBox();
        splashLayout.setSpacing(10);
        splashLayout.setStyle("-fx-alignment: center; -fx-padding: 20;");

        Label splashLabel = new Label("Cargando Editor de Imágenes...");
        ProgressBar splashProgressBar = new ProgressBar();
        splashLayout.getChildren().addAll(splashLabel, splashProgressBar);

        Scene splashScene = new Scene(splashLayout, 300, 150);
        splashStage.setScene(splashScene);
        splashStage.initStyle(StageStyle.UNDECORATED);
        splashStage.show();

        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                for (int i = 0; i <= 100; i++) {
                    Thread.sleep(50); // Simula carga
                    updateProgress(i, 100);
                }
                return null;
            }
        };

        splashProgressBar.progressProperty().bind(loadTask.progressProperty());

        loadTask.setOnSucceeded(event -> {
            splashStage.close();
            try {
                new MainApp().start(primaryStage); // Lanza la app principal
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        new Thread(loadTask).start();
    }

    public static void main(String[] args) {
        launch();
    }
}

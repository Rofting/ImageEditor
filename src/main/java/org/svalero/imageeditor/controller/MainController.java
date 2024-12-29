package org.svalero.imageeditor.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.svalero.imageeditor.models.ImageProcessor;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class MainController {

    @FXML
    private TabPane tabPane;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private ChoiceBox<String> filterChoiceBox;

    @FXML
    private Button undoButton;

    @FXML
    private Button redoButton;

    private final ImageProcessor processor = new ImageProcessor();

    private static class TabContent {
        ImageView imageViewOriginal;
        ImageView imageViewProcessed;
        Stack<Image> undoStack = new Stack<>();
        Stack<Image> redoStack = new Stack<>();
    }

    private final List<TabContent> tabContents = new ArrayList<>();

    @FXML
    private void initialize() {
        filterChoiceBox.getItems().clear();
        filterChoiceBox.getItems().addAll("Escala de Grises", "Invertir Colores", "Aumentar Brillo");
        filterChoiceBox.setValue("Escala de Grises");

        undoButton.setDisable(true);
        redoButton.setDisable(true);
    }

    @FXML
    private void handleOpenImages() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(tabPane.getScene().getWindow());

        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            for (File file : selectedFiles) {
                createNewTab(file);
            }
        } else {
            showAlert("Error", "No se seleccionaron imágenes.");
        }
    }

    private void createNewTab(File file) {
        Image originalImage = new Image(file.toURI().toString());

        TabContent content = new TabContent();
        content.imageViewOriginal = new ImageView(originalImage);
        content.imageViewProcessed = new ImageView();
        content.imageViewOriginal.setFitHeight(277.0);
        content.imageViewOriginal.setFitWidth(300.0);
        content.imageViewOriginal.setPreserveRatio(true);
        content.imageViewProcessed.setFitHeight(277.0);
        content.imageViewProcessed.setFitWidth(300.0);
        content.imageViewProcessed.setPreserveRatio(true);

        tabContents.add(content);

        Tab tab = new Tab(file.getName());
        VBox tabLayout = new VBox(10, content.imageViewOriginal, content.imageViewProcessed);
        tab.setContent(tabLayout);

        Platform.runLater(() -> tabPane.getTabs().add(tab));
    }


    @FXML
    private void handleApplyFilter() {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == null) {
            showAlert("Error", "Seleccione una pestaña primero.");
            return;
        }

        int selectedIndex = tabPane.getTabs().indexOf(selectedTab);
        TabContent content = tabContents.get(selectedIndex);

        if (content.imageViewOriginal.getImage() == null) {
            showAlert("Error", "No hay imagen cargada en esta pestaña.");
            return;
        }

        if (content.imageViewProcessed.getImage() != null) {
            content.undoStack.push(content.imageViewProcessed.getImage());
            content.redoStack.clear();
            undoButton.setDisable(false);
            redoButton.setDisable(true);
        }

        String selectedFilter = filterChoiceBox.getValue();
        Task<Image> task = new Task<>() {
            @Override
            protected Image call() throws Exception {
                Image processedImage = content.imageViewOriginal.getImage();

                switch (selectedFilter) {
                    case "Escala de Grises":
                        return processor.applyGrayscale(processedImage);
                    case "Invertir Colores":
                        return processor.applyInvert(processedImage);
                    case "Aumentar Brillo":
                        return processor.applyBrightness(processedImage, 1.5);
                    default:
                        throw new IllegalArgumentException("Filtro no válido.");
                }
            }
        };

        task.setOnSucceeded(event -> content.imageViewProcessed.setImage(task.getValue()));
        task.setOnFailed(event -> showAlert("Error", "Error al aplicar el filtro."));

        progressBar.progressProperty().bind(task.progressProperty());
        new Thread(task).start();
    }

    @FXML
    private void handleUndo() {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == null) return;

        int selectedIndex = tabPane.getTabs().indexOf(selectedTab);
        TabContent content = tabContents.get(selectedIndex);

        if (!content.undoStack.isEmpty()) {
            Image lastImage = content.undoStack.pop();
            content.redoStack.push(content.imageViewProcessed.getImage());
            content.imageViewProcessed.setImage(lastImage);

            redoButton.setDisable(false);
            undoButton.setDisable(content.undoStack.isEmpty());
        }
    }

    @FXML
    private void handleRedo() {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == null) return;

        int selectedIndex = tabPane.getTabs().indexOf(selectedTab);
        TabContent content = tabContents.get(selectedIndex);

        if (!content.redoStack.isEmpty()) {
            Image nextImage = content.redoStack.pop();
            content.undoStack.push(content.imageViewProcessed.getImage());
            content.imageViewProcessed.setImage(nextImage);

            undoButton.setDisable(false);
            redoButton.setDisable(content.redoStack.isEmpty());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleExit() {
        System.out.println("Saliendo de la aplicación...");
        Platform.exit();
    }

}

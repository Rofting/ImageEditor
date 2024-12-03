package org.svalero.imageeditor.controller;

import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.svalero.imageeditor.models.ImageProcessor;

import java.io.File;

/**
 * MainController - Controlador principal para manejar eventos en la interfaz gráfica.
 */
public class MainController {

    @FXML
    private ImageView imageViewOriginal;

    @FXML
    private ImageView imageViewProcessed;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private ChoiceBox<String> filterChoiceBox;

    private Image originalImage;

    private final ImageProcessor processor = new ImageProcessor();

    @FXML
    private void initialize() {
        // Limpia las opciones existentes antes de agregar nuevas
        filterChoiceBox.getItems().clear();
        // Agrega las opciones al ChoiceBox
        filterChoiceBox.getItems().addAll("Escala de Grises", "Invertir Colores", "Aumentar Brillo");
        // Establece un valor por defecto
        filterChoiceBox.setValue("Escala de Grises");
    }


    @FXML
    private void handleOpenImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(imageViewOriginal.getScene().getWindow());
        if (file != null) {
            originalImage = new Image(file.toURI().toString());
            imageViewOriginal.setImage(originalImage);
            imageViewProcessed.setImage(null); // Reinicia la imagen procesada
        }
    }

    @FXML
    private void handleApplyFilter() {
        if (originalImage == null) {
            showAlert("Error", "Por favor, cargue una imagen primero.");
            return;
        }

        String selectedFilter = filterChoiceBox.getValue();
        if (selectedFilter == null) {
            showAlert("Error", "Por favor, seleccione un filtro.");
            return;
        }

        new Thread(() -> {
            try {
                Image processedImage = null;
                switch (selectedFilter) {
                    case "Escala de Grises":
                        processedImage = processor.applyGrayscale(originalImage);
                        break;
                    case "Invertir Colores":
                        processedImage = processor.applyInvert(originalImage);
                        break;
                    case "Aumentar Brillo":
                        processedImage = processor.applyBrightness(originalImage, 1.5);
                        break;
                }
                Image finalProcessedImage = processedImage;
                Platform.runLater(() -> imageViewProcessed.setImage(finalProcessedImage));
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Error", "Ocurrió un error durante el procesamiento de la imagen."));
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void handleApplyMultipleFilters() {
        if (originalImage == null) {
            showAlert("Error", "Por favor, cargue una imagen primero.");
            return;
        }

        new Thread(() -> {
            try {
                Image processedImage = originalImage;

                Platform.runLater(() -> progressBar.setProgress(0.33));
                processedImage = processor.applyGrayscale(processedImage);
                updateImageView(processedImage);

                Platform.runLater(() -> progressBar.setProgress(0.66));
                processedImage = processor.applyInvert(processedImage);
                updateImageView(processedImage);

                Platform.runLater(() -> progressBar.setProgress(1.0));
                processedImage = processor.applyBrightness(processedImage, 1.5);
                updateImageView(processedImage);

                Platform.runLater(() -> progressBar.setProgress(0));
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Error", "Ocurrió un error durante el procesamiento de la imagen."));
                e.printStackTrace();
            }
        }).start();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void updateImageView(Image image) {
        Platform.runLater(() -> imageViewProcessed.setImage(image));
    }

    @FXML
    private void handleExit() {
        System.out.println("Saliendo de la aplicación...");
        System.exit(0);
    }

    @FXML
    private void handleBatchProcessing() {
        System.out.println("Procesando imágenes por lotes...");
        // Aquí podrías implementar el procesamiento por lotes.
    }
}

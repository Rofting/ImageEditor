package org.svalero.imageeditor.controller;

import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.svalero.imageeditor.models.ImageProcessor;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Stack;

/**
 * MainController - Controlador principal para manejar eventos en la interfaz gráfica.
 */
public class MainController {

    @FXML
    private Button undoButton;

    @FXML
    private Button redoButton;

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

    private final Stack<Image> undoStack = new Stack<>();
    private final Stack<Image> redoStack = new Stack<>();

    @FXML
    private void initialize() {
        // Limpia las opciones existentes antes de agregar nuevas
        filterChoiceBox.getItems().clear();
        // Agrega las opciones al ChoiceBox
        filterChoiceBox.getItems().addAll("Escala de Grises", "Invertir Colores", "Aumentar Brillo");
        // Establece un valor por defecto
        filterChoiceBox.setValue("Escala de Grises");

        undoButton.setDisable(true);
        redoButton.setDisable(true);
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

            undoStack.clear();
            redoStack.clear();
            undoButton.setDisable(true);
            redoButton.setDisable(true);
        }
    }

    @FXML
    private void handleApplyFilter() {
        if (originalImage == null) {
            showAlert("Error", "Por favor, cargue una imagen primero.");
            return;
        }

        // Seleccionar el filtro desde el ChoiceBox
        String selectedFilter = filterChoiceBox.getValue();
        if (selectedFilter == null) {
            showAlert("Error", "Por favor, seleccione un filtro.");
            return;
        }
        //Guardar Imagen actual en el historico
        if (imageViewProcessed.getImage() != null) {
            undoStack.push(imageViewProcessed.getImage());
            redoStack.clear();
            undoButton.setDisable(false);
            redoButton.setDisable(true);
        }

        // Crear un Task para manejar el procesamiento
        Task<Image> task = new Task<>() {
            @Override
            protected Image call() throws Exception {
                Image processedImage = originalImage;

                // Actualizar el progreso en diferentes etapas
                updateProgress(0.1, 1.0); // Inicialización

                // Procesar imagen según el filtro seleccionado
                switch (selectedFilter) {
                    case "Escala de Grises":
                        processedImage = processor.applyGrayscale(originalImage);
                        updateProgress(1.0, 1.0); // Completar
                        break;
                    case "Invertir Colores":
                        processedImage = processor.applyInvert(originalImage);
                        updateProgress(1.0, 1.0); // Completar
                        break;
                    case "Aumentar Brillo":
                        processedImage = processor.applyBrightness(originalImage, 1.5);
                        updateProgress(1.0, 1.0); // Completar
                        break;
                }

                return processedImage;
            }
        };

        // Actualizar la interfaz al completar el procesamiento
        task.setOnSucceeded(event -> {
            Image processedImage = task.getValue();
            imageViewProcessed.setImage(processedImage);
            progressBar.progressProperty().unbind(); // Desvincular la barra
            progressBar.setProgress(0); // Reiniciar la barra
        });

        task.setOnFailed(event -> {
            showAlert("Error", "Ocurrió un error durante el procesamiento de la imagen.");
            progressBar.progressProperty().unbind(); // Desvincular la barra
            progressBar.setProgress(0); // Reiniciar la barra
        });

        // Vincular la barra de progreso al Task
        progressBar.progressProperty().bind(task.progressProperty());

        // Ejecutar el Task en un hilo separado
        new Thread(task).start();
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
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.png"));
        fileChooser.setTitle("Seleccione multiples imagenes para el procesamiento por lotes");

        //Permitir seleccionar multiples archivos
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(imageViewProcessed.getScene().getWindow());

        if (selectedFiles == null || selectedFiles.isEmpty()){
            showAlert("Error", "Por favor, cargue una imagen primero.");
            return;
        }

        new Thread(() -> {
            try {
                int totalImages = selectedFiles.size();
                int currentImageIndex = 0;

                for (File file : selectedFiles) {
                    currentImageIndex++;
                    Image image = new Image(file.toURI().toString());
                    Image processedImage = image;

                    processedImage = processor.applyGrayscale(processedImage);
                    processedImage = processor.applyInvert(processedImage);
                    processedImage = processor.applyBrightness(processedImage, 1.5);

                    double progress = (double) currentImageIndex / totalImages;
                    Platform.runLater(() -> progressBar.setProgress(progress));

                    saveProcessedImage(file, processedImage);
                }

                Platform.runLater(() -> {
                    progressBar.setProgress(0);
                    showAlert("Exito", "El procesamiento de la imagen ha sido procesado.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Error", "Ocurrio un error durante el proceso."));
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void handleUndo() {
        if (!undoStack.isEmpty()) {
            Image lastImage = undoStack.pop();
            redoStack.push(imageViewProcessed.getImage());
            imageViewProcessed.setImage(lastImage);

            redoButton.setDisable(false);
            if (undoStack.isEmpty()) {
                undoButton.setDisable(true);
            }
        }
    }

    @FXML
    private void handleRedo() {
        if (!redoStack.isEmpty()) {
            Image nextImage = redoStack.pop();
            undoStack.push(imageViewProcessed.getImage());
            imageViewProcessed.setImage(nextImage);

            undoButton.setDisable(false);
            if (redoStack.isEmpty()) {
                redoButton.setDisable(true);
            }
        }
    }

    private void saveProcessedImage(File originalFile, Image processedImage) {
        try {
            // Ruta donde se almacenará la imagen procesada
            String outputPath = originalFile.getParent() + "/processed_" + originalFile.getName();
            File outputFile = new File(outputPath);
            ImageIO.write(SwingFXUtils.fromFXImage(processedImage, null), "png", outputFile);
        } catch (IOException e) {
            Platform.runLater(() -> showAlert("Error", "Error al guardar la imagen procesada."));
            e.printStackTrace();
        }
    }



}

package org.svalero.imageeditor.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.svalero.imageeditor.models.ImageProcessor;
import org.svalero.imageeditor.models.ProcessedImageHistory;

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
        Image currentProcessedImage; // Imagen actual después de filtros
        ProgressBar progressBar;
    }


    private final List<TabContent> tabContents = new ArrayList<>();

    private String defaultSavePath = System.getProperty("user.home") + File.separator + "ProcessedImages";

    private void ensureDefaultSavePathExists() {
        File defaultDir = new File(defaultSavePath);
        if (!defaultDir.exists()) {
            defaultDir.mkdirs(); // Crea la carpeta si no existe
        }
    }

    @FXML
    private void initialize() {

        filterChoiceBox.getItems().clear();
        filterChoiceBox.getItems().addAll("Escala de Grises", "Invertir Colores", "Aumentar Brillo");
        filterChoiceBox.setValue("Escala de Grises");

        undoButton.setDisable(true);
        redoButton.setDisable(true);
        ensureDefaultSavePathExists();
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
        content.imageViewOriginal.setFitHeight(200.0);
        content.imageViewOriginal.setFitWidth(300.0);
        content.imageViewOriginal.setPreserveRatio(true);
        content.imageViewProcessed.setFitHeight(200.0);
        content.imageViewProcessed.setFitWidth(300.0);
        content.imageViewProcessed.setPreserveRatio(true);

        content.progressBar = new ProgressBar(0);
        content.progressBar.setPrefWidth(300.0);

        VBox tabLayout = new VBox(10);
        tabLayout.setAlignment(Pos.CENTER); // Alinea el contenido al centro
        tabLayout.getChildren().addAll(content.imageViewOriginal, content.imageViewProcessed, content.progressBar);


        tabContents.add(content);

        Tab tab = new Tab(file.getName());
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

        if (content.currentProcessedImage == null) {
            content.currentProcessedImage = content.imageViewOriginal.getImage(); // Usar original inicialmente
        }

        String selectedFilter = filterChoiceBox.getValue();
        Task<Image> task = new Task<>() {
            @Override
            protected Image call() throws Exception {
                updateProgress(0, 1); // Inicializa progreso

                int delaySteps = 100;
                for (int i = 1; i <= delaySteps; i++) {
                    Thread.sleep(200);
                    updateProgress(i, delaySteps);
                }

                Image processedImage = content.currentProcessedImage;
                switch (selectedFilter) {
                    case "Escala de Grises":
                        processedImage = processor.applyGrayscale(processedImage);
                        break;
                    case "Invertir Colores":
                        processedImage = processor.applyInvert(processedImage);
                        break;
                    case "Aumentar Brillo":
                        processedImage = processor.applyBrightness(processedImage, 1.5);
                        break;
                    default:
                        throw new IllegalArgumentException("Filtro no válido.");
                }
                return processedImage;
            }
        };

        progressBar.progressProperty().bind(task.progressProperty()); // Cambiar por la barra de progreso específica
        content.progressBar.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(event -> {
            content.progressBar.progressProperty().unbind();
            content.progressBar.setProgress(0);
            content.undoStack.push(content.currentProcessedImage); // Guarda el estado actual en undoStack
            content.currentProcessedImage = task.getValue(); // Actualiza la imagen procesada actual
            content.imageViewProcessed.setImage(content.currentProcessedImage);

            content.redoStack.clear(); // Limpia redoStack, ya que se aplicó un nuevo filtro

            undoButton.setDisable(content.undoStack.isEmpty());
            redoButton.setDisable(content.redoStack.isEmpty());

            String filterName = filterChoiceBox.getValue();
            updateHistory(selectedTab.getText(), filterName); // Actualiza historial

            showAlert("Éxito", "Filtro aplicado correctamente.");
        });



        // Manejo de error o cancelación
        task.setOnFailed(event -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);
            showAlert("Error", "Error al aplicar el filtro.");
        });

        task.setOnCancelled(event -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);
            showAlert("Cancelado", "El filtro fue cancelado.");
        });

        new Thread(task).start(); // Ejecutar tarea en segundo plano
    }




    @FXML
    private void handleSaveImage() {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == null) {
            showAlert("Error", "Seleccione una pestaña primero.");
            return;
        }

        int selectedIndex = tabPane.getTabs().indexOf(selectedTab);
        TabContent content = tabContents.get(selectedIndex);

        if (content.imageViewProcessed.getImage() == null) {
            showAlert("Error", "No hay imagen procesada para guardar.");
            return;
        }

        // Generar un nombre único para la imagen procesada
        String originalName = selectedTab.getText();
        String processedName = originalName.replaceAll("(\\.[^.]+)$", "_filtered$1");

        File saveFile = new File(defaultSavePath, processedName);

        try {
            ImageIO.write(SwingFXUtils.fromFXImage(content.imageViewProcessed.getImage(), null), "png", saveFile);
            showAlert("Éxito", "Imagen guardada en: " + saveFile.getAbsolutePath());
        } catch (IOException e) {
            showAlert("Error", "No se pudo guardar la imagen.");
        }
    }



    @FXML
    private void handleUndo() {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == null) return;

        int selectedIndex = tabPane.getTabs().indexOf(selectedTab);
        TabContent content = tabContents.get(selectedIndex);

        if (!content.undoStack.isEmpty()) {
            content.redoStack.push(content.currentProcessedImage); // Mueve la actual a redoStack
            content.currentProcessedImage = content.undoStack.pop(); // Recupera la última de undoStack
            content.imageViewProcessed.setImage(content.currentProcessedImage); // Actualiza la vista

            undoButton.setDisable(content.undoStack.isEmpty());
            redoButton.setDisable(content.redoStack.isEmpty());
        }
    }


    @FXML
    private void handleRedo() {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == null) return;

        int selectedIndex = tabPane.getTabs().indexOf(selectedTab);
        TabContent content = tabContents.get(selectedIndex);

        if (!content.redoStack.isEmpty()) {
            content.undoStack.push(content.currentProcessedImage); // Mueve la actual a undoStack
            content.currentProcessedImage = content.redoStack.pop(); // Recupera la siguiente de redoStack
            content.imageViewProcessed.setImage(content.currentProcessedImage); // Actualiza la vista

            undoButton.setDisable(content.undoStack.isEmpty());
            redoButton.setDisable(content.redoStack.isEmpty());
        }
    }


    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
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

    @FXML
    private ListView<String> historyListView;
    private final ProcessedImageHistory processedImageHistory = new ProcessedImageHistory();

    private void updateHistory(String imageName, String filterName) {
        processedImageHistory.addEntry(imageName, filterName);
        historyListView.getItems().add("Imagen: " + imageName + ", Filtro: " + filterName);
    }


    @FXML
    private Label currentSavePathLabel;

    @FXML
    private void handleChangeSavePath() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Seleccionar Carpeta de Guardado");
        File selectedDirectory = directoryChooser.showDialog(tabPane.getScene().getWindow());
        if (selectedDirectory != null) {
            defaultSavePath = selectedDirectory.getAbsolutePath();
            currentSavePathLabel.setText("Ruta Actual: " + defaultSavePath);
        }
    }

}

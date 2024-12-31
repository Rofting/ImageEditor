package org.svalero.imageeditor.models;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.*;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * ImageProcessor - Contiene métodos para aplicar filtros a imágenes.
 */
public class ImageProcessor {

    /**
     * Aplica un filtro de escala de grises a la imagen proporcionada.
     *
     * @param input La imagen de entrada.
     * @return La imagen procesada en escala de grises.
     */
    public Image applyGrayscale(Image input) {
        int width = (int) input.getWidth();
        int height = (int) input.getHeight();
        WritableImage output = new WritableImage(width, height);
        PixelReader reader = input.getPixelReader();
        PixelWriter writer = output.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);
                double grayscale = (color.getRed() + color.getGreen() + color.getBlue()) / 3.0;
                writer.setColor(x, y, new Color(grayscale, grayscale, grayscale, color.getOpacity()));
            }
        }
        return output;
    }

    /**
     * Aplica un filtro de inversión de color a la imagen proporcionada.
     *
     * @param input La imagen de entrada.
     * @return La imagen procesada con colores invertidos.
     */
    public Image applyInvert(Image input) {
        int width = (int) input.getWidth();
        int height = (int) input.getHeight();
        WritableImage output = new WritableImage(width, height);
        PixelReader reader = input.getPixelReader();
        PixelWriter writer = output.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);
                writer.setColor(x, y, new Color(1.0 - color.getRed(), 1.0 - color.getGreen(), 1.0 - color.getBlue(), color.getOpacity()));
            }
        }
        return output;
    }

    /**
     * Aplica un aumento de brillo a la imagen proporcionada.
     *
     * @param input La imagen de entrada.
     * @param factor Factor de brillo (1.0 = sin cambios, >1.0 aumenta brillo).
     * @return La imagen procesada con aumento de brillo.
     */
    public Image applyBrightness(Image input, double factor) {
        int width = (int) input.getWidth();
        int height = (int) input.getHeight();
        WritableImage output = new WritableImage(width, height);
        PixelReader reader = input.getPixelReader();
        PixelWriter writer = output.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);
                double red = Math.min(color.getRed() * factor, 1.0);
                double green = Math.min(color.getGreen() * factor, 1.0);
                double blue = Math.min(color.getBlue() * factor, 1.0);
                writer.setColor(x, y, new Color(red, green, blue, color.getOpacity()));
            }
        }
        return output;
    }

    private String outputPath = System.getProperty("user.home");

    public void setOutputPath(String path) {
        this.outputPath = path;
    }

    public void saveProcessedImage(Image image, String fileName) throws IOException {
        File outputFile = new File(outputPath, fileName);
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", outputFile);
    }

}

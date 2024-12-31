package org.svalero.imageeditor.models;

import java.util.ArrayList;
import java.util.List;

public class ProcessedImageHistory {
    private final List<String> history = new ArrayList<>();

    public void addEntry(String imageName, String filterName) {
        history.add("Imagen: " + imageName + ", Filtro: " + filterName);
    }

    public List<String> getHistory() {
        return history;
    }
}

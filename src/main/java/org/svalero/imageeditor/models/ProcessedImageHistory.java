package org.svalero.imageeditor.models;

import java.util.ArrayList;
import java.util.List;

public class ProcessedImageHistory {
    private final List<String> history = new ArrayList<>();

    public void addEntry(String entry) {
        history.add(entry); // Agrega un texto al historial
    }

    public List<String> getHistory() {
        return history; // Devuelve el historial completo
    }
}

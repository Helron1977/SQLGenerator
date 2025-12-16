package com.sqlgenerator.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Résultat d'un upload de fichier SQL.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadResult {
    private boolean success;
    private String message;
    private String filename;
    private String queryId;
    private int queriesLoaded;
    private List<String> warnings = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
}


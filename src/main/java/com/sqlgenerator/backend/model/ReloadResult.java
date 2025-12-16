package com.sqlgenerator.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Résultat d'un rechargement des queries.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReloadResult {
    private boolean success;
    private String message;
    private int queriesLoaded;
    private int queriesFailed;
    private List<String> errors = new ArrayList<>();
}


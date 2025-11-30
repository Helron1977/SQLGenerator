package com.sqlgenerator.backend.model;

import lombok.Data;

@Data
public class ParameterDefinition {
    private String name;
    private String type; // number, text, date, file
    private String label;
    private boolean required;
    private boolean isFile; // true si le paramètre nécessite un fichier upload
}

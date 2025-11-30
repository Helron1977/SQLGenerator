package com.sqlgenerator.backend.model;

import lombok.Data;
import java.util.List;

@Data
public class QueryDefinition {
    private String id;
    private String name;
    private String description;
    private List<String> tags;
    private String sqlFile; // Nom du fichier SQL dans resources/sql/ (ex: "update-person-name.sql")
    private List<ParameterDefinition> parameters;
}

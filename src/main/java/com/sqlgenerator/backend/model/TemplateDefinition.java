package com.sqlgenerator.backend.model;

import lombok.Data;
import java.util.List;

/**
 * Représente un template SQL avec ses métadonnées et paramètres.
 * 
 * Un template est un modèle de requête SQL paramétré qui peut être utilisé
 * pour générer des scripts SQL en remplaçant les placeholders par des valeurs.
 */
@Data
public class TemplateDefinition {
    private String id;
    private String name;
    private String description;
    private List<String> tags;
    /**
     * Nom du fichier source SQL dans resources/templates/ (ex: "update-person-name.sql").
     * 
     * Le fichier source contient le SQL avec ses métadonnées (commentaires -- @id:, -- @param:, etc.).
     * Le SQL pur (sans métadonnées) est extrait lors du chargement via TemplateService.loadSqlFromFile().
     */
    private String sqlFilename;
    private List<ParameterDefinition> parameters;
}

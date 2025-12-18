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
    
    /**
     * Indique si ce template doit être masqué dans la liste des formulaires.
     * 
     * Si true, le template ne sera pas exposé via GET /api/forms (mais reste disponible
     * pour les tests d'intégration et l'administration).
     * 
     * Défini via la métadonnée -- @hidden: true dans le fichier SQL.
     */
    private boolean hidden = false;
}

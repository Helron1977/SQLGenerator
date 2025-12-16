package com.sqlgenerator.backend.model;

import lombok.Data;
import java.util.Map;

/**
 * Schéma de structure JSON pour construire le body d'un template.
 * 
 * Contient la structure attendue (clés, types, exemples) sans valeurs réelles,
 * pour permettre au front de construire dynamiquement le formulaire et le body.
 */
@Data
public class RequestBodySchema {

    /**
     * Identifiant du template.
     */
    private String templateId;

    /**
     * Nom du template.
     */
    private String name;

    /**
     * Description du template.
     */
    private String description;

    /**
     * Structure JSON attendue pour le mode unitaire.
     * Clés = noms des champs, Valeurs = exemples ou types.
     * Exemple : { "ticket": "uuid-example", "executionType": "unitaire", "person_id": "001", "name": "John" }
     */
    private Map<String, Object> unitBodyStructure;

    /**
     * Structure JSON attendue pour le mode masse (null si non disponible).
     * Exemple : { "ticket": "uuid-example", "masseFile": "file" }
     */
    private Map<String, Object> massBodyStructure;

    /**
     * Indique si le mode masse est disponible.
     */
    private boolean massModeAvailable;
}


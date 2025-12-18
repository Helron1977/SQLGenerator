package com.sqlgenerator.backend.model;

import lombok.Data;

/**
 * Représente un champ d'un formulaire pour un template.
 *
 * Cette classe est indépendante de Swagger / OpenAPI.
 * Elle sert de contrat simple entre le backend et un front (React, autre)
 * pour la génération dynamique de formulaires.
 */
@Data
public class FormField {

    /**
     * Nom technique du champ (clé dans le body HTTP).
     * Exemple : ticket, executionType, person_id, masseFile, contrat_ide, ...
     */
    private String name;

    /**
     * Type de champ :
     * - text     : champ texte libre
     * - number   : nombre (integer/decimal)
     * - date     : date au format métier (ex: 30/11/25)
     * - file     : upload de fichier
     * - select   : liste de valeurs prédéfinies (voir options)
     */
    private String type;

    /**
     * Label affichable dans l'UI.
     * Pour les paramètres SQL, provient de ParameterDefinition.label.
     */
    private String label;

    /**
     * Champ obligatoire ou non.
     */
    private boolean required;

    /**
     * Liste d'options pour les champs de type select (ex: executionType).
     * Peut être null ou vide si non applicable.
     */
    private java.util.List<String> options;

    /**
     * Texte d'aide optionnel pour l'utilisateur (placeholder, info).
     */
    private String helpText;

    /**
     * Indique si ce champ est purement technique (non métier).
     * <p>
     * Exemple typique : {@code executionType}, utilisé pour piloter le mode
     * d'exécution (unitaire/masse) côté backend, mais qui ne doit pas être
     * affiché comme champ de saisie explicite dans le formulaire.
     * <p>
     * Par défaut à false (champ métier).
     */
    private boolean technical;
}



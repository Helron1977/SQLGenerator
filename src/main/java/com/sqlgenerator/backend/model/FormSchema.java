package com.sqlgenerator.backend.model;

import lombok.Data;

import java.util.List;

/**
 * Schéma de formulaire pour un template donné.
 *
 * Ce modèle décrit les champs à afficher côté front
 * pour les différents modes d'exécution (unitaire, masse).
 */
@Data
public class FormSchema {

    /**
     * Identifiant technique du template (id).
     */
    private String templateId;

    /**
     * Nom lisible du template (name) si présent, sinon id.
     */
    private String name;

    /**
     * Description fonctionnelle du template.
     */
    private String description;

    /**
     * Tags éventuels (catégorisation).
     */
    private List<String> tags;

    /**
     * Indique si le template utilise un paramètre fichier @param-file (clause IN).
     */
    private boolean hasInParameter;

    /**
     * Modes d'exécution supportés par ce template.
     * Exemples :
     * - ["unitaire"] pour les templates avec IN
     * - ["unitaire","masse"] pour les templates sans IN
     */
    private List<String> modes;

    /**
     * Champs du formulaire pour le mode unitaire.
     * Contient toujours au moins le ticket et l'executionType,
     * plus les paramètres du template.
     */
    private List<FormField> unitFields;

    /**
     * Champs du formulaire pour le mode masse.
     * Null si le mode masse n'est pas supporté pour ce template
     * (ex: templates avec IN).
     */
    private List<FormField> massFields;
}



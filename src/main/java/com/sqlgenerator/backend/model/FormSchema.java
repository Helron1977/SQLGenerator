package com.sqlgenerator.backend.model;

import lombok.Data;

import java.util.List;

/**
 * Schéma de formulaire pour une query donnée.
 *
 * Ce modèle décrit les champs à afficher côté front
 * pour les différents modes d'exécution (unitaire, masse).
 */
@Data
public class FormSchema {

    /**
     * Identifiant technique de la query (id).
     */
    private String queryId;

    /**
     * Nom lisible de la query (name) si présent, sinon id.
     */
    private String name;

    /**
     * Description fonctionnelle de la query.
     */
    private String description;

    /**
     * Tags éventuels (catégorisation).
     */
    private List<String> tags;

    /**
     * Indique si la query utilise un paramètre fichier @param-file (clause IN).
     */
    private boolean hasInParameter;

    /**
     * Modes d'exécution supportés par cette query.
     * Exemples :
     * - ["unitaire"] pour les requêtes avec IN
     * - ["unitaire","masse"] pour les requêtes sans IN
     */
    private List<String> modes;

    /**
     * Champs du formulaire pour le mode unitaire.
     * Contient toujours au moins le ticket et l'executionType,
     * plus les paramètres de la query.
     */
    private List<FormField> unitFields;

    /**
     * Champs du formulaire pour le mode masse.
     * Null si le mode masse n'est pas supporté pour cette query
     * (ex: requêtes avec IN).
     */
    private List<FormField> massFields;
}



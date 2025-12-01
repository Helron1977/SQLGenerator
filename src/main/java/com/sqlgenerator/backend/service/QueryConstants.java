package com.sqlgenerator.backend.service;

/**
 * Constantes utilisées dans le traitement des requêtes SQL.
 * Centralisées ici pour faciliter la maintenance et éviter les valeurs magiques.
 */
public final class QueryConstants {

    private QueryConstants() {
        // Classe utilitaire, pas d'instanciation
    }

    /**
     * Limite Oracle pour les clauses IN : Oracle ne supporte pas plus de 1000 éléments dans un IN.
     * On utilise 999 pour laisser une marge de sécurité et éviter les erreurs de dépassement.
     */
    public static final int ORACLE_IN_MAX_SIZE = 999;

    /**
     * Type d'exécution : mode unitaire (une requête à la fois).
     */
    public static final String EXECUTION_TYPE_UNITAIRE = "unitaire";

    /**
     * Type d'exécution : mode masse (plusieurs requêtes depuis un fichier CSV).
     */
    public static final String EXECUTION_TYPE_MASSE = "masse";

    /**
     * Chemin du répertoire où sont stockés les fichiers SQL générés.
     */
    public static final String REPO_PATH = "./svn_repo_mock/";

    /**
     * Format de date attendu pour les champs de type date dans les requêtes SQL.
     * Format utilisé car les dates sont souvent stockées en CHAR dans Oracle.
     */
    public static final String DATE_FORMAT = "DD/MM/YY";
}


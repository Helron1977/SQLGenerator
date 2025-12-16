package com.sqlgenerator.backend.service;

/**
 * Constantes utilisées dans le traitement des templates SQL.
 * Centralisées ici pour faciliter la maintenance et éviter les valeurs magiques.
 */
public final class TemplateConstants {

    private TemplateConstants() {
        // Classe utilitaire, pas d'instanciation
    }

    // ========== Chemins et Répertoires ==========
    
    /**
     * Répertoire contenant les templates SQL dans les ressources.
     * Utilisé pour le chargement des fichiers SQL au démarrage.
     */
    public static final String TEMPLATES_DIR = "templates/";
    
    /**
     * Pattern classpath pour scanner tous les fichiers SQL des templates.
     */
    public static final String TEMPLATES_CLASSPATH_PATTERN = "classpath:templates/*.sql";
    
    /**
     * Chemin du répertoire où sont stockés les scripts SQL générés.
     * 
     * ⚠️ DEPRECATED : Utilisez AppProperties.getOutputScriptsPath() à la place.
     * Cette constante est conservée pour compatibilité mais la valeur par défaut
     * peut être surchargée via application.properties (sql.generator.output.path).
     */
    @Deprecated
    public static final String OUTPUT_SCRIPTS_PATH = "./svn_repo_mock/";
    
    // ========== Extensions de Fichiers ==========
    
    /**
     * Extension des fichiers SQL.
     */
    public static final String SQL_FILE_EXTENSION = ".sql";
    
    // ========== Modes d'Exécution ==========

    /**
     * Type d'exécution : mode unitaire (une requête à la fois).
     */
    public static final String EXECUTION_TYPE_UNITAIRE = "unitaire";

    /**
     * Type d'exécution : mode masse (plusieurs requêtes depuis un fichier CSV).
     */
    public static final String EXECUTION_TYPE_MASSE = "masse";
    
    // ========== Limites SQL ==========

    /**
     * Limite Oracle pour les clauses IN : Oracle ne supporte pas plus de 1000 éléments dans un IN.
     * On utilise 999 pour laisser une marge de sécurité et éviter les erreurs de dépassement.
     */
    public static final int ORACLE_IN_MAX_SIZE = 999;
    
    // ========== Formats ==========

    /**
     * Format de date attendu pour les champs de type date dans les requêtes SQL.
     * Format utilisé car les dates sont souvent stockées en CHAR dans Oracle.
     */
    public static final String DATE_FORMAT = "DD/MM/YY";
}


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

    // ========== Métadonnées SQL ==========

    /**
     * Préfixe des lignes de métadonnées dans les fichiers SQL.
     * Format : "-- @"
     */
    public static final String METADATA_PREFIX = "-- @";

    /**
     * Longueur du préfixe des métadonnées.
     */
    public static final int METADATA_PREFIX_LENGTH = 4;

    /**
     * Préfixe des lignes de paramètres normaux dans les fichiers SQL.
     * Format : "-- @param:"
     */
    public static final String PARAM_PREFIX = "-- @param:";

    /**
     * Longueur du préfixe des paramètres normaux.
     */
    public static final int PARAM_PREFIX_LENGTH = 10;

    /**
     * Préfixe des lignes de paramètres fichier dans les fichiers SQL.
     * Format : "-- @param-file:"
     */
    public static final String PARAM_FILE_PREFIX = "-- @param-file:";

    /**
     * Longueur du préfixe des paramètres fichier.
     */
    public static final int PARAM_FILE_PREFIX_LENGTH = 15;

    /**
     * Nombre minimum de parties requises pour un paramètre (nom|type|label).
     */
    public static final int MINIMUM_PARAMETER_PARTS = 3;

    // ========== Patterns et Expressions Régulières ==========

    /**
     * Pattern regex pour extraire les placeholders du format {{nom_param}}.
     */
    public static final String PLACEHOLDER_PATTERN = "\\{\\{([^}]+)\\}\\}";

    /**
     * Pattern regex pour valider le format de date DD/MM/YY.
     */
    public static final String DATE_PATTERN_DDMMYY = "\\d{2}/\\d{2}/\\d{2}";

    /**
     * Pattern regex pour valider le format de date YYYY-MM-DD.
     */
    public static final String DATE_PATTERN_YYYYMMDD = "\\d{4}-\\d{2}-\\d{2}";

    // ========== Noms de Paramètres Spéciaux ==========

    /**
     * Nom du paramètre pour le fichier CSV en mode masse.
     */
    public static final String MASSE_FILE_PARAM = "masseFile";

    /**
     * Nom du paramètre pour le ticket (obligatoire dans tous les formulaires).
     */
    public static final String TICKET_PARAM = "ticket";

    /**
     * Nom du paramètre pour le type d'exécution.
     */
    public static final String EXECUTION_TYPE_PARAM = "executionType";

    // ========== Types de Champs de Formulaire ==========

    /**
     * Type de champ formulaire : texte.
     */
    public static final String FIELD_TYPE_TEXT = "text";

    /**
     * Type de champ formulaire : nombre.
     */
    public static final String FIELD_TYPE_NUMBER = "number";

    /**
     * Type de champ formulaire : date.
     */
    public static final String FIELD_TYPE_DATE = "date";

    /**
     * Type de champ formulaire : sélection (select).
     */
    public static final String FIELD_TYPE_SELECT = "select";

    /**
     * Type de champ formulaire : fichier.
     */
    public static final String FIELD_TYPE_FILE = "file";
}


package com.sqlgenerator.backend.controller;

import com.sqlgenerator.backend.model.QueryDefinition;
import com.sqlgenerator.backend.model.ReloadResult;
import com.sqlgenerator.backend.model.UploadResult;
import com.sqlgenerator.backend.service.QueryMetadataParser;
import com.sqlgenerator.backend.service.QueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Controller pour les opérations d'administration :
 * - Upload de fichiers SQL
 * - Hot-reload des queries
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    private static final String UPLOADS_DIR = "./sql_uploads/";

    @Autowired
    private QueryService queryService;

    @Autowired
    private QueryMetadataParser metadataParser;

    /**
     * Upload un fichier SQL, le valide et recharge les queries.
     * 
     * IMPORTANT : Le nom du fichier (sans extension .sql) doit correspondre à l'ID dans les métadonnées (@id:).
     * Exemple : Si le fichier s'appelle "update-person.sql", alors -- @id: update-person
     * 
     * @param file Le fichier SQL à uploader (format: {id}.sql où {id} correspond à -- @id: dans le fichier)
     * @param overwrite Si true, écrase un fichier existant avec le même nom
     * @return Résultat de l'upload avec détails
     */
    @PostMapping(value = "/upload-sql", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @io.swagger.v3.oas.annotations.Operation(
        summary = "Upload un fichier SQL",
        description = "Upload un fichier SQL avec validation. Le nom du fichier (sans .sql) doit correspondre à l'ID dans les métadonnées (@id:)."
    )
    public ResponseEntity<UploadResult> uploadSqlFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "overwrite", defaultValue = "false") boolean overwrite) {
        
        UploadResult result = new UploadResult();
        
        try {
            // 1. Validation du fichier
            if (file == null || file.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("Fichier vide ou manquant");
                result.getErrors().add("Le fichier est requis");
                return ResponseEntity.badRequest().body(result);
            }
            
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.endsWith(".sql")) {
                result.setSuccess(false);
                result.setMessage("Format de fichier invalide");
                result.getErrors().add("Le fichier doit avoir l'extension .sql");
                return ResponseEntity.badRequest().body(result);
            }
            
            // Normaliser le nom de fichier (sécurité)
            String filename = sanitizeFilename(originalFilename);
            
            // Extraire l'ID attendu depuis le nom de fichier (sans extension .sql)
            String expectedId = filename.substring(0, filename.length() - 4); // Retirer ".sql"
            
            // 2. Lire et valider le contenu
            String sqlContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            QueryDefinition query;
            try {
                query = metadataParser.parseSqlContent(sqlContent, filename);
            } catch (Exception e) {
                result.setSuccess(false);
                result.setMessage("Erreur de validation des métadonnées");
                result.getErrors().add(e.getMessage());
                return ResponseEntity.badRequest().body(result);
            }
            
            // 3. Valider que le nom du fichier correspond à l'ID (depuis métadonnées ou nom de fichier)
            // Si -- @id: est présent dans les métadonnées, il doit correspondre au nom du fichier
            // Sinon, l'ID est extrait du nom du fichier (déjà fait dans parseSqlContent)
            if (!expectedId.equals(query.getId())) {
                result.setSuccess(false);
                result.setMessage("Le nom du fichier ne correspond pas à l'ID");
                result.getErrors().add(
                    String.format(
                        "Le nom du fichier doit correspondre à l'ID.\n" +
                        "   Nom du fichier (sans extension) : '%s'\n" +
                        "   ID déterminé : '%s'\n" +
                        "   Solution : Renommez le fichier en '%s.sql' ou supprimez -- @id: dans le fichier pour utiliser le nom du fichier",
                        expectedId, query.getId(), query.getId()
                    )
                );
                return ResponseEntity.badRequest().body(result);
            }
            
            // 4. Vérifier l'unicité de l'ID (sauf si overwrite)
            if (!overwrite) {
                QueryDefinition existing = queryService.getQueryById(query.getId());
                if (existing != null) {
                    result.setSuccess(false);
                    result.setMessage("ID déjà existant");
                    result.getErrors().add(
                        String.format("L'ID '%s' existe déjà. Utilisez overwrite=true pour écraser.", query.getId())
                    );
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
                }
            }
            
            // 5. Valider les placeholders
            try {
                String sqlWithoutMetadata = removeMetadataComments(sqlContent);
                queryService.validatePlaceholders(query, sqlWithoutMetadata, filename);
            } catch (IllegalArgumentException e) {
                result.setSuccess(false);
                result.setMessage("Erreur de validation des placeholders");
                result.getErrors().add(e.getMessage());
                return ResponseEntity.badRequest().body(result);
            }
            
            // 6. Sauvegarder le fichier
            Path uploadsDir = Paths.get(UPLOADS_DIR);
            Files.createDirectories(uploadsDir);
            Path filePath = uploadsDir.resolve(filename);
            Files.write(filePath, sqlContent.getBytes(StandardCharsets.UTF_8));
            logger.info("Fichier SQL uploadé : {}", filename);
            
            // 7. Recharger les queries
            try {
                queryService.reloadQueries();
                int queriesLoaded = queryService.getAllQueries().size();
                
                result.setSuccess(true);
                result.setMessage("Fichier uploadé et queries rechargées avec succès");
                result.setFilename(filename);
                result.setQueryId(query.getId());
                result.setQueriesLoaded(queriesLoaded);
                
                return ResponseEntity.ok(result);
            } catch (Exception e) {
                logger.error("Erreur lors du rechargement des queries : {}", e.getMessage(), e);
                result.setSuccess(false);
                result.setMessage("Fichier uploadé mais erreur lors du rechargement");
                result.getErrors().add(e.getMessage());
                result.getWarnings().add("Le fichier a été sauvegardé mais les queries n'ont pas été rechargées");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
            
        } catch (IOException e) {
            logger.error("Erreur lors de l'upload du fichier : {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("Erreur serveur lors de l'upload");
            result.getErrors().add(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * Recharge toutes les queries depuis les fichiers SQL.
     */
    @PostMapping("/reload-queries")
    public ResponseEntity<ReloadResult> reloadQueries() {
        ReloadResult result = new ReloadResult();
        
        try {
            queryService.reloadQueries();
            int afterCount = queryService.getAllQueries().size();
            
            result.setSuccess(true);
            result.setMessage(String.format("Queries rechargées : %d query(s) disponible(s)", afterCount));
            result.setQueriesLoaded(afterCount);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Erreur lors du rechargement des queries : {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("Erreur lors du rechargement");
            result.getErrors().add(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * Nettoie le nom de fichier pour éviter les paths relatifs malveillants.
     */
    private String sanitizeFilename(String filename) {
        // Garder seulement le nom de fichier (pas de path)
        String name = Paths.get(filename).getFileName().toString();
        // Remplacer les caractères dangereux
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Retire les métadonnées des commentaires SQL.
     */
    private String removeMetadataComments(String sqlContent) {
        StringBuilder sql = new StringBuilder();
        String[] lines = sqlContent.split("\n");
        boolean metadataSection = true;
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("-- @")) {
                continue;
            }
            if (trimmed.isEmpty() && metadataSection) {
                continue;
            }
            metadataSection = false;
            sql.append(line).append("\n");
        }
        
        return sql.toString().trim();
    }
}


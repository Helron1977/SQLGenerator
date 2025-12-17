package com.sqlgenerator.backend.controller;

import com.sqlgenerator.backend.config.AppProperties;
import com.sqlgenerator.backend.model.TemplateDefinition;
import com.sqlgenerator.backend.service.FormSchemaService;
import com.sqlgenerator.backend.service.TemplateConstants;
import com.sqlgenerator.backend.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Sous-domaine "Administration" - Contrôleur d'administration pour les opérations système.
 * 
 * <p><b>Responsabilité</b> : Opérations d'administration et tests d'intégration système.</p>
 * 
 * <p><b>Opérations</b> : POST (opérations d'administration)</p>
 * <p><b>Clients</b> : DevOps, administrateurs, pipelines CI/CD</p>
 * 
 * <p>Ce contrôleur sépare les opérations d'administration (tests, validations système) des opérations métier
 * (exposition de schémas, génération de scripts). Les clients sont différents (opérateurs vs utilisateurs finaux),
 * les permissions peuvent être distinctes, et l'évolution est indépendante.</p>
 * 
 * <p><b>Endpoints disponibles</b> :</p>
 * <ul>
 *   <li>POST /api/admin/integration-test : Test d'intégration complet</li>
 * </ul>
 * 
 * <p><b>Note</b> : La supervision (health check, métriques) est gérée au niveau supérieur du projet.</p>
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
@Tag(name = "Administration", description = "Opérations d'administration et tests d'intégration système")
public class AdminController {

    @Autowired
    private TemplateService templateService;

    @Autowired
    private FormSchemaService formSchemaService;

    @Autowired
    private AppProperties appProperties;

    /**
     * Test d'intégration complet.
     * 
     * Exécute une série de tests pour valider le bon fonctionnement :
     * 1. Chargement des templates
     * 2. Parsing des métadonnées
     * 3. Génération d'un script simple (si templates disponibles)
     * 4. Validation des schémas
     * 5. Vérification des endpoints
     */
    @PostMapping("/integration-test")
    @Operation(
            summary = "Test d'intégration",
            description = "Exécute une série de tests d'intégration pour valider le bon fonctionnement de l'application"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tests d'intégration réussis"),
            @ApiResponse(responseCode = "500", description = "Échec d'un ou plusieurs tests")
    })
    public ResponseEntity<Map<String, Object>> integrationTest() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> tests = new ArrayList<>();
        boolean allPassed = true;
        
        // Test 1 : Chargement des templates
        Map<String, Object> test1 = new HashMap<>();
        test1.put("name", "Chargement des templates");
        test1.put("description", "Vérifie que les templates sont chargés au démarrage");
        try {
            List<TemplateDefinition> templates = templateService.getAllTemplates();
            test1.put("status", "PASSED");
            test1.put("details", String.format("%d template(s) chargé(s)", templates.size()));
            if (templates.isEmpty()) {
                test1.put("status", "WARNING");
                test1.put("details", "Aucun template chargé - vérifiez src/main/resources/templates/");
                allPassed = false;
            }
        } catch (Exception e) {
            test1.put("status", "FAILED");
            test1.put("details", "Erreur: " + e.getMessage());
            allPassed = false;
        }
        tests.add(test1);
        
        // Test 2 : Parsing des métadonnées
        Map<String, Object> test2 = new HashMap<>();
        test2.put("name", "Parsing des métadonnées");
        test2.put("description", "Vérifie que les métadonnées SQL sont correctement parsées");
        try {
            List<TemplateDefinition> templates = templateService.getAllTemplates();
            int validTemplates = 0;
            List<String> errors = new ArrayList<>();
            
            for (TemplateDefinition template : templates) {
                if (template.getId() == null || template.getId().isEmpty()) {
                    errors.add("Template sans ID: " + template.getSqlFilename());
                } else if (template.getParameters() == null) {
                    errors.add("Template sans paramètres: " + template.getId());
                } else {
                    validTemplates++;
                }
            }
            
            test2.put("status", errors.isEmpty() ? "PASSED" : "FAILED");
            test2.put("details", String.format("%d/%d template(s) valide(s)", validTemplates, templates.size()));
            if (!errors.isEmpty()) {
                test2.put("errors", errors);
                allPassed = false;
            }
        } catch (Exception e) {
            test2.put("status", "FAILED");
            test2.put("details", "Erreur: " + e.getMessage());
            allPassed = false;
        }
        tests.add(test2);
        
        // Test 3 : Génération d'un script (si templates disponibles)
        Map<String, Object> test3 = new HashMap<>();
        test3.put("name", "Génération d'un script SQL");
        test3.put("description", "Teste la génération d'un script SQL avec un template simple");
        try {
            List<TemplateDefinition> templates = templateService.getAllTemplates();
            if (templates.isEmpty()) {
                test3.put("status", "SKIPPED");
                test3.put("details", "Aucun template disponible pour tester");
            } else {
                // Trouver un template simple (sans IN, avec au moins 1 paramètre)
                TemplateDefinition testTemplate = templates.stream()
                        .filter(t -> t.getParameters() != null && !t.getParameters().isEmpty())
                        .filter(t -> t.getParameters().stream().noneMatch(p -> p.isFile()))
                        .findFirst()
                        .orElse(templates.get(0));
                
                // Préparer des paramètres de test
                Map<String, Object> testParams = new HashMap<>();
                testParams.put("ticket", "TEST-" + System.currentTimeMillis());
                testParams.put("executionType", TemplateConstants.EXECUTION_TYPE_UNITAIRE);
                
                // Ajouter des valeurs de test pour les paramètres
                if (testTemplate.getParameters() != null) {
                    for (var param : testTemplate.getParameters()) {
                        if (!param.isFile() && !"ticket".equals(param.getName()) && !"executionType".equals(param.getName())) {
                            switch (param.getType().toLowerCase()) {
                                case "number":
                                    testParams.put(param.getName(), "42");
                                    break;
                                case "date":
                                    testParams.put(param.getName(), "01/01/25");
                                    break;
                                default:
                                    testParams.put(param.getName(), "test-value");
                            }
                        }
                    }
                }
                
                // Générer le script (test d'intégration complet)
                // Le fichier sera supprimé après vérification pour ne pas polluer le répertoire
                String fileName = templateService.generateScriptFile(
                        testTemplate.getId(),
                        TemplateConstants.EXECUTION_TYPE_UNITAIRE,
                        testParams
                );
                
                // Vérifier que la génération a retourné un nom de fichier valide
                if (fileName != null && !fileName.isEmpty() && fileName.endsWith(TemplateConstants.SQL_FILE_EXTENSION)) {
                    // Vérifier que le fichier a bien été créé (test d'intégration complet)
                    Path filePath = Paths.get(appProperties.getOutputScriptsPath(), fileName);
                    if (Files.exists(filePath)) {
                        long fileSize = Files.size(filePath);
                        test3.put("status", "PASSED");
                        test3.put("details", String.format("Script généré avec succès: %s (%d bytes)", fileName, fileSize));
                        
                        // Supprimer le fichier de test pour ne pas polluer le répertoire
                        try {
                            Files.delete(filePath);
                            test3.put("note", "Fichier de test supprimé automatiquement");
                        } catch (Exception e) {
                            test3.put("warning", "Impossible de supprimer le fichier de test: " + e.getMessage());
                        }
                    } else {
                        test3.put("status", "FAILED");
                        test3.put("details", "Fichier non trouvé après génération");
                        allPassed = false;
                    }
                } else {
                    test3.put("status", "FAILED");
                    test3.put("details", "Nom de fichier invalide retourné: " + fileName);
                    allPassed = false;
                }
            }
        } catch (Exception e) {
            test3.put("status", "FAILED");
            test3.put("details", "Erreur: " + e.getMessage());
            allPassed = false;
        }
        tests.add(test3);
        
        // Test 4 : Validation des schémas
        Map<String, Object> test4 = new HashMap<>();
        test4.put("name", "Validation des schémas");
        test4.put("description", "Vérifie que les schémas de formulaire sont générés correctement");
        try {
            List<TemplateDefinition> templates = templateService.getAllTemplates();
            int validSchemas = 0;
            
            for (TemplateDefinition template : templates) {
                try {
                    var schema = formSchemaService.getFormSchema(template.getId());
                    if (schema != null && schema.getTemplateId() != null) {
                        validSchemas++;
                    }
                } catch (Exception e) {
                    // Ignorer les erreurs individuelles
                }
            }
            
            test4.put("status", validSchemas == templates.size() ? "PASSED" : "FAILED");
            test4.put("details", String.format("%d/%d schéma(s) valide(s)", validSchemas, templates.size()));
            if (validSchemas != templates.size()) {
                allPassed = false;
            }
        } catch (Exception e) {
            test4.put("status", "FAILED");
            test4.put("details", "Erreur: " + e.getMessage());
            allPassed = false;
        }
        tests.add(test4);
        
        // Test 5 : Vérification du répertoire de sortie
        Map<String, Object> test5 = new HashMap<>();
        test5.put("name", "Répertoire de sortie");
        test5.put("description", "Vérifie que le répertoire de sortie est accessible");
        try {
            String outputPathStr = appProperties.getOutputScriptsPath();
            Path outputPath = Paths.get(outputPathStr);
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }
            
            boolean writable = Files.isWritable(outputPath);
            test5.put("status", writable ? "PASSED" : "FAILED");
            test5.put("details", String.format("Répertoire: %s (accessible: %s)", 
                    outputPathStr, writable));
            if (!writable) {
                allPassed = false;
            }
        } catch (Exception e) {
            test5.put("status", "FAILED");
            test5.put("details", "Erreur: " + e.getMessage());
            allPassed = false;
        }
        tests.add(test5);
        
        result.put("timestamp", LocalDateTime.now());
        result.put("overallStatus", allPassed ? "PASSED" : "FAILED");
        result.put("testsCount", tests.size());
        result.put("testsPassed", tests.stream().filter(t -> "PASSED".equals(t.get("status"))).count());
        result.put("testsFailed", tests.stream().filter(t -> "FAILED".equals(t.get("status"))).count());
        result.put("testsSkipped", tests.stream().filter(t -> "SKIPPED".equals(t.get("status"))).count());
        result.put("tests", tests);
        
        return allPassed 
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(500).body(result);
    }
}


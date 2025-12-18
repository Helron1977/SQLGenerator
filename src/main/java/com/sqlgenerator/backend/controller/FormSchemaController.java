package com.sqlgenerator.backend.controller;

import com.sqlgenerator.backend.model.FormSchema;
import com.sqlgenerator.backend.service.FormSchemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Sous-domaine "Form Schema" - Contrôleur REST pour exposer les schémas de formulaires.
 * 
 * <p><b>Responsabilité</b> : Exposer les schémas de formulaires (FormSchema) construits à partir des templates SQL.</p>
 * 
 * <p><b>Ressources</b> : FormSchema (schémas JSON pour le frontend, dérivés des TemplateDefinition)</p>
 * <p><b>Opérations</b> : GET uniquement (lecture des schémas)</p>
 * <p><b>Clients</b> : Frontend (construction de formulaires dynamiques)</p>
 * 
 * <p>Ce contrôleur fournit les métadonnées nécessaires au frontend pour construire
 * dynamiquement les formulaires de génération de scripts SQL, sans déclencher de génération.</p>
 * 
 * <p>Note : Les schémas sont construits à partir des templates SQL gérés par {@link com.sqlgenerator.backend.service.TemplateService}.</p>
 * <p>Voir aussi : {@link ScriptController} pour la génération effective des scripts.</p>
 */
@RestController
@RequestMapping("/api/forms")
@Tag(name = "Form Schema", description = "Schémas de formulaires - Exposition des métadonnées pour construire les formulaires frontend")
public class FormSchemaController {

    @Autowired
    private FormSchemaService formSchemaService;

    /**
     * Retourne les schémas de formulaires pour tous les templates disponibles.
     *
     * Utilisation côté front :
     * 1. Appeler GET /api/forms pour récupérer la liste des FormSchema
     * 2. Afficher la liste des templates et les champs de formulaire
     * 3. Construire ensuite le body pour POST /api/scripts/{id} (avec mode unitaire ou masse)
     */
    @GetMapping
    @Operation(
            summary = "Lister les schémas de formulaires disponibles",
            description = """
                    Retourne, pour chaque template SQL disponible, la description complète des schémas de formulaires
                    (modes unitaire et masse) à utiliser côté front pour appeler les endpoints /api/scripts/{id}.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Liste des schémas de formulaires disponibles",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = FormSchema.class)))
            )
    })
    public ResponseEntity<List<FormSchema>> getAllTemplates() {
        return ResponseEntity.ok(formSchemaService.getAllFormSchemas());
    }

    /**
     * Retourne le schéma de formulaire pour un template donné.
     *
     * Utilisation côté front :
     * 1. Appeler GET /api/forms/{id} pour récupérer le FormSchema de ce template
     * 2. Construire dynamiquement le formulaire (modes unitaire / masse)
     * 3. Poster les données vers /api/scripts/{id} (avec mode unitaire ou masse)
     *
     * @param id identifiant du template
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Obtenir le schéma de formulaire d'un template",
            description = """
                    Retourne la description du schéma de formulaire (modes unitaire et masse) pour le template spécifié.
                    Permet au front de savoir quels champs afficher et comment appeler les endpoints de génération.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Schéma de formulaire trouvé",
                    content = @Content(schema = @Schema(implementation = FormSchema.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Schéma de formulaire introuvable (template inexistant)"
            )
    })
    public ResponseEntity<FormSchema> getTemplate(@PathVariable String id) {
        FormSchema schema = formSchemaService.getFormSchema(id);
        if (schema == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(schema);
    }

    /**
     * Retourne uniquement le JSON du body avec les paramètres (pour copier-coller dans Swagger).
     * 
     * ⚠️ Usage : Uniquement pour les tests manuels dans Swagger ou les clients HTTP.
     * Pour le frontend, utilisez plutôt GET /api/forms/{id} qui retourne le schéma complet.
     *
     * @param id identifiant du template
     * @param mode mode d'exécution : 'unitaire' (défaut) ou 'masse'
     */
    @GetMapping("/{id}/request-body")
    @Operation(
            summary = "Obtenir le JSON du body à copier-coller (pour tests manuels)",
            description = """
                    ⚠️ **Usage : Tests manuels uniquement** (Swagger, Postman, etc.)
                    
                    Retourne uniquement le JSON avec les paramètres et leurs valeurs d'exemple,
                    prêt à être copié-collé dans le body de POST /api/scripts/{id}.
                    
                    Le mode peut être spécifié via le query parameter 'mode' (unitaire par défaut, masse pour le mode masse).
                    
                    **Pour le frontend** : Utilisez plutôt GET /api/forms/{id} qui retourne le schéma complet
                    avec tous les détails (labels, types, validations, etc.) pour construire le formulaire.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Schéma JSON de la requête",
                    content = @Content(mediaType = "application/json", schema = @Schema(type = "object"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Schéma de formulaire introuvable (template inexistant)"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Mode masse non disponible pour ce template (présence de paramètre IN)"
            )
    })
    public ResponseEntity<Map<String, Object>> getRequestBody(
            @Parameter(description = "Identifiant du template", required = true, example = "update-person-name")
            @PathVariable String id,
            @Parameter(description = "Mode d'exécution : 'unitaire' (défaut) ou 'masse'", required = false, example = "unitaire")
            @RequestParam(required = false, defaultValue = "unitaire") String mode) {
        
        Map<String, Object> bodyStructure;
        
        if ("masse".equals(mode)) {
            bodyStructure = formSchemaService.getMassBodyStructure(id);
            if (bodyStructure == null) {
                // Mode masse non disponible (template avec IN)
                return ResponseEntity.badRequest().build();
            }
        } else {
            bodyStructure = formSchemaService.getUnitBodyStructure(id);
        }
        
        if (bodyStructure == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(bodyStructure);
    }
}


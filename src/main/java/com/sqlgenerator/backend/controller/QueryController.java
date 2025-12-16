package com.sqlgenerator.backend.controller;

import com.sqlgenerator.backend.model.FormSchema;
import com.sqlgenerator.backend.service.FormSchemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST pour exposer les queries SQL et leurs schémas
 * construits à partir des métadonnées SQL.
 *
 * Objectif :
 * - Fournir au front un contrat simple (JSON) décrivant les queries disponibles
 *   et les schémas pour construire les requêtes (modes unitaire et masse).
 */
@RestController
@RequestMapping("/api/queries")
@CrossOrigin(origins = "*")
@io.swagger.v3.oas.annotations.tags.Tag(name = "SQL Queries", description = "Gestion des queries SQL et leurs schémas")
public class QueryController {

    @Autowired
    private FormSchemaService formSchemaService;

    /**
     * Retourne les schémas de queries pour toutes les queries disponibles.
     *
     * Utilisation côté front :
     * 1. Appeler GET /api/queries pour récupérer la liste des FormSchema
     * 2. Afficher la liste des queries et les champs de formulaire
     * 3. Construire ensuite le body pour POST /api/patches/{id} (avec mode unitaire ou masse)
     */
    @GetMapping
    @Operation(
            summary = "Lister les queries SQL disponibles",
            description = "Retourne, pour chaque query SQL disponible, la description complète des schémas "
                    + "(modes unitaire et masse) à utiliser côté front pour appeler les endpoints /api/patches/{id}."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Liste des queries disponibles",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = FormSchema.class)))
            )
    })
    public ResponseEntity<List<FormSchema>> getAllQueries() {
        return ResponseEntity.ok(formSchemaService.getAllFormSchemas());
    }

    /**
     * Retourne le schéma d'une query donnée.
     *
     * Utilisation côté front :
     * 1. Appeler GET /api/queries/{id} pour récupérer le FormSchema de cette query
     * 2. Construire dynamiquement le formulaire (modes unitaire / masse)
     * 3. Poster les données vers /api/patches/{id} (avec mode unitaire ou masse)
     *
     * @param id identifiant de la query
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Obtenir le schéma d'une query",
            description = "Retourne la description de la query (modes unitaire et masse) pour la query spécifiée. "
                    + "Permet au front de savoir quels champs afficher et comment appeler les endpoints de génération."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Schéma de query trouvé",
                    content = @Content(schema = @Schema(implementation = FormSchema.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Query introuvable"
            )
    })
    public ResponseEntity<FormSchema> getQuery(@PathVariable String id) {
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
     * Pour le frontend, utilisez plutôt GET /api/queries/{id} qui retourne le schéma complet.
     *
     * @param id identifiant de la query
     * @param mode mode d'exécution : 'unitaire' (défaut) ou 'masse'
     */
    @GetMapping("/{id}/request-body")
    @Operation(
            summary = "Obtenir le JSON du body à copier-coller (pour tests manuels)",
            description = "⚠️ **Usage : Tests manuels uniquement** (Swagger, Postman, etc.)\n\n" +
                    "Retourne uniquement le JSON avec les paramètres et leurs valeurs d'exemple, " +
                    "prêt à être copié-collé dans le body de POST /api/patches/{id}. " +
                    "\n\n" +
                    "Le mode peut être spécifié via le query parameter 'mode' (unitaire par défaut, masse pour le mode masse). " +
                    "\n\n" +
                    "**Pour le frontend** : Utilisez plutôt GET /api/queries/{id} qui retourne le schéma complet " +
                    "avec tous les détails (labels, types, validations, etc.) pour construire le formulaire."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Schéma JSON de la requête",
                    content = @Content(mediaType = "application/json", schema = @Schema(type = "object"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Query introuvable"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Mode masse non disponible pour cette query (présence de paramètre IN)"
            )
    })
    public ResponseEntity<Map<String, Object>> getRequestBody(
            @Parameter(description = "Identifiant de la query", required = true, example = "update-person-name")
            @PathVariable String id,
            @Parameter(description = "Mode d'exécution : 'unitaire' (défaut) ou 'masse'", required = false, example = "unitaire")
            @RequestParam(required = false, defaultValue = "unitaire") String mode) {
        
        Map<String, Object> bodyStructure;
        
        if ("masse".equals(mode)) {
            bodyStructure = formSchemaService.getMassBodyStructure(id);
            if (bodyStructure == null) {
                // Mode masse non disponible (query avec IN)
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


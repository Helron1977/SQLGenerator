package com.sqlgenerator.backend.config;

import com.sqlgenerator.backend.model.ParameterDefinition;
import com.sqlgenerator.backend.model.QueryDefinition;
import com.sqlgenerator.backend.service.QueryService;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PatchOpenApiCustomizer implements OpenApiCustomizer {

    @Autowired
    private QueryService queryService;

    @Override
    public void customise(OpenAPI openApi) {
        if (queryService == null) {
            return;
        }

        List<QueryDefinition> queries = queryService.getAllQueries();
        if (queries == null || queries.isEmpty()) {
            return;
        }

        for (QueryDefinition query : queries) {
            if (query == null || query.getId() == null) {
                continue;
            }

            // Endpoint unitaire (ou avec IN)
            String path = "/api/patch/" + query.getId();
            PathItem pathItem = openApi.getPaths().computeIfAbsent(path, k -> new PathItem());
            pathItem.setPost(createOperation(query));
            
            // Endpoint masse (uniquement pour les requêtes sans IN)
            if (!hasInParameter(query)) {
                String massePath = "/api/patch/" + query.getId() + "/masse";
                PathItem massePathItem = openApi.getPaths().computeIfAbsent(massePath, k -> new PathItem());
                massePathItem.setPost(createMasseOperation(query));
            }
        }
    }

    private Operation createOperation(QueryDefinition query) {
        Operation operation = new Operation();
        operation.setSummary(query.getName() != null ? query.getName() : query.getId());
        operation.setDescription(query.getDescription());
        
        if (query.getTags() != null && !query.getTags().isEmpty()) {
            operation.setTags(query.getTags());
        }
        
        operation.setRequestBody(createRequestBody(query));
        operation.setResponses(createResponses());
        
        return operation;
    }

    private RequestBody createRequestBody(QueryDefinition query) {
        ObjectSchema schema = new ObjectSchema();
        boolean hasInParameter = hasInParameter(query);
        
        addTicketParameter(schema);
        addExecutionTypeParameter(schema, hasInParameter);
        addQueryParameters(schema, query);
        
        RequestBody requestBody = buildRequestBody(schema, hasInParameter);
        return requestBody;
    }

    private boolean hasInParameter(QueryDefinition query) {
        return query.getParameters() != null && 
                query.getParameters().stream().anyMatch(ParameterDefinition::isFile);
    }

    private void addTicketParameter(ObjectSchema schema) {
        StringSchema ticketSchema = new StringSchema();
        ticketSchema.setDescription("Numéro du ticket");
        ticketSchema.setExample("dc905fff-27a6-452f-aa0d-360c6c37b94a");
        schema.addProperty("ticket", ticketSchema);
        schema.addRequiredItem("ticket");
    }

    private void addExecutionTypeParameter(ObjectSchema schema, boolean hasInParameter) {
        StringSchema executionTypeSchema = new StringSchema();
        executionTypeSchema.setDescription("Type d'exécution");
        executionTypeSchema.setExample("unitaire");
        executionTypeSchema.setDefault("unitaire");
        executionTypeSchema.addEnumItem("unitaire");
        // Plus d'option "masse" ici, c'est un endpoint séparé maintenant
        
        schema.addProperty("executionType", executionTypeSchema);
    }

    /**
     * Crée une opération spécifique pour le mode masse.
     */
    private Operation createMasseOperation(QueryDefinition query) {
        Operation operation = new Operation();
        operation.setSummary((query.getName() != null ? query.getName() : query.getId()) + " (Mode Masse)");
        operation.setDescription(query.getDescription() + "\n\n**Mode Masse** : Upload un fichier CSV avec une ligne par requête. " +
                "Les valeurs doivent être séparées par virgule dans l'ordre des paramètres.");
        
        if (query.getTags() != null && !query.getTags().isEmpty()) {
            operation.setTags(query.getTags());
        }
        
        operation.setRequestBody(createMasseRequestBody(query));
        operation.setResponses(createResponses());
        
        return operation;
    }

    /**
     * Crée le RequestBody pour le mode masse : seulement ticket + fichier CSV.
     */
    private RequestBody createMasseRequestBody(QueryDefinition query) {
        ObjectSchema schema = new ObjectSchema();
        
        addTicketParameter(schema);
        
        // Fichier CSV obligatoire pour le mode masse
        StringSchema masseFileSchema = new StringSchema();
        masseFileSchema.setFormat("binary");
        masseFileSchema.setDescription("Fichier CSV (requis). " +
                "Une ligne par requête, valeurs séparées par virgule dans l'ordre des paramètres : " +
                getParameterOrderDescription(query));
        schema.addProperty("masseFile", masseFileSchema);
        schema.addRequiredItem("masseFile");
        
        RequestBody requestBody = new RequestBody();
        requestBody.setDescription("Paramètres pour générer le patch SQL en mode masse");
        requestBody.setRequired(true);
        
        io.swagger.v3.oas.models.media.Content content = new io.swagger.v3.oas.models.media.Content();
        addMultipartMediaType(content, schema);
        addFormUrlEncodedMediaType(content, schema);
        requestBody.setContent(content);
        
        return requestBody;
    }

    /**
     * Génère une description de l'ordre des paramètres pour l'aide utilisateur.
     */
    private String getParameterOrderDescription(QueryDefinition query) {
        if (query.getParameters() == null || query.getParameters().isEmpty()) {
            return "Aucun paramètre";
        }
        
        List<String> paramNames = query.getParameters().stream()
                .filter(p -> !p.isFile())
                .map(ParameterDefinition::getName)
                .collect(java.util.stream.Collectors.toList());
        
        return String.join(", ", paramNames);
    }

    private void addQueryParameters(ObjectSchema schema, QueryDefinition query) {
        if (query.getParameters() != null) {
            for (ParameterDefinition param : query.getParameters()) {
                if (param != null && param.getName() != null) {
                    Schema<?> paramSchema = createParameterSchema(param);
                    schema.addProperty(param.getName(), paramSchema);
                    if (param.isRequired()) {
                        schema.addRequiredItem(param.getName());
                    }
                }
            }
        }
    }

    private RequestBody buildRequestBody(ObjectSchema schema, boolean hasFileParam) {
        RequestBody requestBody = new RequestBody();
        requestBody.setDescription("Paramètres pour générer le patch SQL");
        requestBody.setRequired(true);
        
        io.swagger.v3.oas.models.media.Content content = new io.swagger.v3.oas.models.media.Content();
        
        if (hasFileParam) {
            addMultipartMediaType(content, schema);
        }
        
        addFormUrlEncodedMediaType(content, schema);
        requestBody.setContent(content);
        
        return requestBody;
    }

    private void addMultipartMediaType(io.swagger.v3.oas.models.media.Content content, ObjectSchema schema) {
        MediaType multipartMediaType = new MediaType();
        multipartMediaType.setSchema(schema);
        content.addMediaType("multipart/form-data", multipartMediaType);
    }

    private void addFormUrlEncodedMediaType(io.swagger.v3.oas.models.media.Content content, ObjectSchema schema) {
        MediaType formMediaType = new MediaType();
        formMediaType.setSchema(schema);
        content.addMediaType("application/x-www-form-urlencoded", formMediaType);
    }

    private Schema<?> createParameterSchema(ParameterDefinition param) {
        Schema<?> schema;
        
        // Si c'est un paramètre fichier, utiliser un schéma de type file
        if (param.isFile()) {
            schema = new StringSchema().format("binary");
            schema.setDescription((param.getLabel() != null ? param.getLabel() : "") 
                    + " (Fichier texte, 1 valeur par ligne)");
        } else {
            String type = param.getType() != null ? param.getType().toLowerCase() : "text";
            
            switch (type) {
                case "number":
                case "integer":
                    schema = new IntegerSchema();
                    break;
                case "date":
                    schema = new StringSchema().format("date");
                    break;
                default:
                    schema = new StringSchema();
            }
            
            if (param.getLabel() != null) {
                schema.setDescription(param.getLabel());
            }
        }
        
        return schema;
    }

    private ApiResponses createResponses() {
        ApiResponses responses = new ApiResponses();
        responses.addApiResponse("200", new ApiResponse().description("Fichier SQL généré"));
        responses.addApiResponse("404", new ApiResponse().description("Query non trouvée"));
        responses.addApiResponse("500", new ApiResponse().description("Erreur serveur"));
        return responses;
    }
}

package com.sqlgenerator.backend.service;

import com.sqlgenerator.backend.model.QueryDefinition;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class SqlFileBuilder {

    private static final String REPO_PATH = "./svn_repo_mock/";

    /**
     * Construit le contenu complet du fichier SQL (en-tête + SQL).
     */
    public String buildCompleteFile(QueryDefinition query, String executionType, 
                                    Map<String, Object> params, String sql) {
        StringBuilder content = new StringBuilder();
        content.append(buildHeader(query, executionType, params));
        content.append("\n");
        content.append(sql);
        return content.toString();
    }

    /**
     * Construit l'en-tête du fichier SQL.
     */
    public String buildHeader(QueryDefinition query, String executionType, Map<String, Object> params) {
        StringBuilder header = new StringBuilder();
        header.append("-- Fichier de Patch Généré le ").append(LocalDateTime.now()).append("\n");
        header.append("-- Requête: ").append(query.getName()).append("\n");
        header.append("-- ID: ").append(query.getId()).append("\n");
        header.append("-- Ticket: ").append(params.get("ticket")).append("\n");
        header.append("-- Type: ").append(executionType).append("\n");
        return header.toString();
    }

    /**
     * Génère le nom de fichier avec timestamp.
     */
    public String generateFileName(String queryId, String executionType) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return String.format("%s_%s_%s.sql", queryId, executionType, timestamp);
    }

    /**
     * Écrit le fichier SQL sur le disque.
     */
    public void writeFile(String fileName, String content) throws IOException {
        Path path = Paths.get(REPO_PATH, fileName);
        Files.write(path, content.getBytes());
    }

    /**
     * Construit et écrit le fichier SQL complet.
     * Retourne le nom du fichier généré.
     */
    public String buildAndWriteFile(QueryDefinition query, String executionType, 
                                    Map<String, Object> params, String sql) throws IOException {
        String fileContent = buildCompleteFile(query, executionType, params, sql);
        String fileName = generateFileName(query.getId(), executionType);
        writeFile(fileName, fileContent);
        return fileName;
    }
}


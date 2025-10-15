package org.acme.evolv.factory.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.acme.evolv.entity.AiScenarioEntity;
import org.acme.evolv.entity.AiScenarioRunEntity;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@ApplicationScoped
public class PdfAnalyzeService {

    @Inject
    ObjectMapper mapper;

    @ConfigProperty(name = "ai.api.url")
    String pdfApiUrl;

    @Transactional
    public AiScenarioRunEntity analyzeAndSave(AiScenarioEntity scenario, String fileName, String model)
            throws Exception {
        var payload = mapper.createObjectNode();
        if (model != null && !model.isBlank())
            payload.put("model", model);
        payload.put("file_name", fileName);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(pdfApiUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            JsonNode root = mapper.readTree(resp.body());

            AiScenarioRunEntity run = new AiScenarioRunEntity();
            run.id = UUID.randomUUID();
            run.scenario = scenario;
            run.companyId = scenario.companyId;
            run.fileName = fileName;
            run.modelUsed = model;
            run.resultMarkdown = root.path("result_markdown").asText();
            run.promptUsed = root.path("prompt_used").asText();
            run.status = "done";
            run.createdAt = OffsetDateTime.now();
            run.persist();
            return run;
        }

        JsonNode root = mapper.readTree(resp.body());

        AiScenarioRunEntity run = new AiScenarioRunEntity();
        run.id = UUID.randomUUID();
        run.scenario = scenario;
        run.companyId = scenario.companyId;
        run.fileName = fileName;
        run.modelUsed = model;
        run.resultMarkdown = root.path("result_markdown").asText();
        run.promptUsed = root.path("prompt_used").asText();
        run.status = "done";
        run.createdAt = OffsetDateTime.now();
        run.persist();
        return run;
    }
}
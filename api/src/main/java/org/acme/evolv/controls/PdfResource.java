package org.acme.evolv.controls;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import org.acme.evolv.entity.AiScenarioEntity;
import org.acme.evolv.forms.PdfForm;
import org.acme.evolv.utils.PathUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.*;

@Path("/api/pdf")
@Produces(MediaType.APPLICATION_JSON)
public class PdfResource {

    @Inject
    @ConfigProperty(name = "ai.api.url")
    String pythonApiUrl;  // e.x. http://localhost:8000/api/pdf/analyze
    
    @POST
    @Path("/analyze")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Map<String, Object> analyzePdf(PdfForm form) throws Exception {
        if (form == null || form.file == null) {
            throw new BadRequestException("no files");
        }

        byte[] pdfBytes = Files.readAllBytes(form.file.filePath());
        String fileName = form.file.fileName();
        String contentType = form.file.contentType();

        String customPrompt = "";
        String model = null;

        UUID uuidCompanyId = UUID.fromString(form.companyid);
        UUID uuidId = UUID.fromString(form.id);

        AiScenarioEntity s = AiScenarioEntity.findById(uuidId);
        
        if (s == null || !Objects.equals(s.companyId, uuidCompanyId))
            throw new NotFoundException();

        customPrompt = s.promptTemplate;
        
        HttpRequest request = buildMultipartRequest(
                pythonApiUrl + PathUtils.getFullPath(getClass(), "analyzePdf", PdfForm.class).replace("/api/", ""),
                Map.of(
                        "custom_prompt", customPrompt == null ? "" : customPrompt,
                        "model", model == null ? "" : model
                ),
                "file",
                fileName,
                contentType,
                pdfBytes
        );

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Python API call failture: " + response.statusCode() + " → " + response.body());
        }

        com.fasterxml.jackson.databind.JsonNode root =
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.body());

        return Map.of(
                "file", root.path("file_name").asText(),
                "markdown", root.path("result_markdown").asText(),
                "model", root.path("model").asText(),
                "note", root.path("extra").path("note").asText(),
                "prompt_used", root.path("prompt_used").asText(),
                "text_bytes", root.path("text_bytes").asInt()
        );
    }

    private HttpRequest buildMultipartRequest(
            String url,
            Map<String, String> formFields,
            String fileField,
            String fileName,
            String contentType,
            byte[] fileBytes
    ) throws IOException {
        String boundary = "----Boundary" + UUID.randomUUID();
        var builder = new StringBuilder();

        for (Map.Entry<String, String> e : formFields.entrySet()) {
            if (e.getValue() == null || e.getValue().isEmpty()) continue;
            builder.append("--").append(boundary).append("\r\n");
            builder.append("Content-Disposition: form-data; name=\"").append(e.getKey()).append("\"\r\n\r\n");
            builder.append(e.getValue()).append("\r\n");
        }

        builder.append("--").append(boundary).append("\r\n");
        builder.append("Content-Disposition: form-data; name=\"").append(fileField)
                .append("\"; filename=\"").append(fileName).append("\"\r\n");
        builder.append("Content-Type: ").append(contentType != null ? contentType : "application/pdf").append("\r\n\r\n");

        byte[] prefix = builder.toString().getBytes();
        byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes();

        byte[] all = new byte[prefix.length + fileBytes.length + suffix.length];
        System.arraycopy(prefix, 0, all, 0, prefix.length);
        System.arraycopy(fileBytes, 0, all, prefix.length, fileBytes.length);
        System.arraycopy(suffix, 0, all, prefix.length + fileBytes.length, suffix.length);

        return HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(all))
                .build();
    }
}

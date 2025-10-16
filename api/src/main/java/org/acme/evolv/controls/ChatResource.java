package org.acme.evolv.controls;

import org.acme.evolv.entity.AiScenarioEntity;
import org.acme.evolv.forms.MultiForm;
import org.acme.evolv.utils.PathUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;

@Path("/api/pdf")
@Produces(MediaType.APPLICATION_JSON)
public class ChatResource {

    @Inject
    @ConfigProperty(name = "ai.api.url")
    String pythonApiUrl;
    
    @POST
    @Path("/analyze-multi")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> analyzeMulti(MultiForm form) throws Exception {
        if (form == null || form.files == null || form.files.isEmpty()) {
            throw new BadRequestException("no files");
        }

        // default question
        String question = form.question == null ? "" : form.question.trim();
        String model = (form.model == null || form.model.isBlank()) ? null : form.model.trim();

        // get custom_prompt
        UUID uuidCompanyId = UUID.fromString(form.companyid);
        UUID uuidId = UUID.fromString(form.id);

        AiScenarioEntity s = AiScenarioEntity.findById(uuidId);
        if (s == null || !Objects.equals(s.companyId, uuidCompanyId)) {
            throw new NotFoundException();
        }
        String customPrompt = s.promptTemplate == null ? "" : s.promptTemplate;

        List<FilePart> parts = new ArrayList<>();
        for (FileUpload fu : form.files) {
            String fileName = fu.fileName();
            String contentType = fu.contentType();
            if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
                throw new BadRequestException("is only PDF files: " + fileName);
            }
            byte[] bytes = Files.readAllBytes(fu.filePath());
            parts.add(new FilePart("files", fileName, contentType, bytes));
        }

        // Python /analyze_multi
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("custom_prompt", customPrompt);
        fields.put("question", question);
        fields.put("model", model == null ? "" : model);

        HttpRequest request = buildMultipartRequestMulti(
                pythonApiUrl + PathUtils.getFullPath(ChatResource.class, "analyzeMulti", MultiForm.class).replace("/api/", ""),
                fields,
                parts
        );

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Python API call failture: " + response.statusCode() + " → " + response.body());
        }

        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode root = om.readTree(response.body());
        List<String> files = new ArrayList<>();
        if (root.path("file_names").isArray()) {
            for (var n : root.path("file_names")) files.add(n.asText());
        }

        String markdown = root.path("answer_markdown").asText(""); 
        if (markdown.isEmpty()) {
            markdown = root.path("result_markdown").asText("");
        }

        String usedModel = root.path("model").asText("");
        String note = root.path("extra").path("note").asText("");

        return Map.of(
                "files", files,
                "markdown", markdown,
                "model", usedModel,
                "note", note
        );
    }

    /* ---------------- multipart ---------------- */

    static class FilePart {
        final String fieldName;    // e.g. "files"
        final String fileName;
        final String contentType;  // e.g. "application/pdf"
        final byte[] bytes;

        FilePart(String fieldName, String fileName, String contentType, byte[] bytes) {
            this.fieldName = fieldName;
            this.fileName = fileName;
            this.contentType = contentType == null || contentType.isBlank()
                    ? "application/octet-stream"
                    : contentType;
            this.bytes = bytes;
        }
    }

    private static HttpRequest buildMultipartRequestMulti(
            String url,
            Map<String, String> fields,
            List<FilePart> files
    ) {
        String boundary = "----JavaBoundary" + UUID.randomUUID();
        var CRLF = "\r\n";
        var byteOut = new java.io.ByteArrayOutputStream();

        try {
            for (var e : fields.entrySet()) {
                String name = e.getKey();
                String value = e.getValue() == null ? "" : e.getValue();

                byteOut.write(("--" + boundary + CRLF).getBytes());
                byteOut.write(("Content-Disposition: form-data; name=\"" + name + "\"" + CRLF).getBytes());
                byteOut.write(("Content-Type: text/plain; charset=UTF-8" + CRLF + CRLF).getBytes());
                byteOut.write(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                byteOut.write(CRLF.getBytes());
            }

            for (FilePart fp : files) {
                byteOut.write(("--" + boundary + CRLF).getBytes());
                byteOut.write(("Content-Disposition: form-data; name=\"" + fp.fieldName + "\"; filename=\"" + fp.fileName + "\"" + CRLF).getBytes());
                byteOut.write(("Content-Type: " + fp.contentType + CRLF + CRLF).getBytes());
                byteOut.write(fp.bytes);
                byteOut.write(CRLF.getBytes());
            }

            byteOut.write(("--" + boundary + "--" + CRLF).getBytes());

        } catch (Exception ex) {
            throw new RuntimeException("multipart request's failture: " + ex.getMessage(), ex);
        }

        var bodyPublisher = HttpRequest.BodyPublishers.ofByteArray(byteOut.toByteArray());

        return HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(bodyPublisher)
                .build();
    }
}

package org.acme.evolv.controls;

import io.quarkus.panache.common.Sort;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.OffsetDateTime;
import java.util.*;

import org.acme.evolv.dto.AnalyzeProxyReq;
import org.acme.evolv.dto.RunResponseDTO;
import org.acme.evolv.dto.SampleDTO;
import org.acme.evolv.dto.ScenarioBasicDTO;
import org.acme.evolv.dto.ScenarioCreateDTO;
import org.acme.evolv.dto.ScenarioOptionsDTO;
import org.acme.evolv.dto.ScenarioPromptDTO;
import org.acme.evolv.entity.AiScenarioEntity;
import org.acme.evolv.entity.AiScenarioOptionEntity;
import org.acme.evolv.entity.AiScenarioRunEntity;
import org.acme.evolv.entity.AiScenarioSampleEntity;
import org.acme.evolv.factory.services.PdfAnalyzeService;

@Path("/api/scenarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ScenarioResource {

    @Inject
    PdfAnalyzeService analyzeService;

    // ---------- Basic ----------
    @GET
    public List<ScenarioBasicDTO> list(@QueryParam("companyId") String companyId) {
        if (companyId == null || companyId.isBlank())
            throw new BadRequestException("companyId required");

        UUID uuidCompanyId = UUID.fromString(companyId);
        List<AiScenarioEntity> entities = AiScenarioEntity.list(
                "companyId = ?1",
                Sort.by("createdAt").descending(),
                uuidCompanyId);

        if (entities == null || entities.isEmpty()) {
            return List.of(); // 返回 []
        }

        return entities.stream()
                .map(s -> new ScenarioBasicDTO(
                        s.id,
                        s.companyId.toString(),
                        s.name,
                        s.type,
                        s.iconUrl,
                        s.tags == null ? List.of() : Arrays.asList(s.tags),
                        s.description,
                        s.promptTemplate))
                .toList();
    }

    @POST
    @Transactional
    public ScenarioBasicDTO create(ScenarioCreateDTO dto) {
        if (dto.companyId() == null || dto.companyId().isBlank())
            throw new BadRequestException("companyId required");

        UUID companyId = UUID.fromString(dto.companyId());
        long exists = AiScenarioEntity.count("companyId=?1 and name=?2", companyId, dto.name());
        if (exists > 0)
            throw new ClientErrorException("name already exists in this company", 409);

        AiScenarioEntity s = new AiScenarioEntity();
        s.id = UUID.randomUUID();
        s.companyId = UUID.fromString(dto.companyId());
        s.name = dto.name();
        s.type = dto.type();
        s.iconUrl = dto.iconUrl();
        s.tags = dto.tags() == null ? null : dto.tags().toArray(new String[0]);
        s.description = dto.description();
        s.promptTemplate = dto.promptTemplate();
        s.createdAt = s.updatedAt = OffsetDateTime.now();
        s.persist();

        AiScenarioOptionEntity opt = new AiScenarioOptionEntity();
        opt.scenarioId = s.id;
        opt.scenario = s;
        opt.saveHistory = true;
        opt.allowUpload = true;
        opt.persist();

        return new ScenarioBasicDTO(s.id, s.companyId.toString(), s.name, s.type, s.iconUrl,
                s.tags == null ? List.of() : Arrays.asList(s.tags), s.description, s.promptTemplate);
    }

    @GET
    @Path("/{id}")
    public ScenarioBasicDTO get(@PathParam("id") UUID id, @QueryParam("companyId") UUID companyId) {
        AiScenarioEntity s = AiScenarioEntity.findById(id);
        if (s == null || !Objects.equals(s.companyId, companyId))
            throw new NotFoundException();
        return new ScenarioBasicDTO(s.id, s.companyId.toString(), s.name, s.type, s.iconUrl,
                s.tags == null ? List.of() : Arrays.asList(s.tags), s.description,s.promptTemplate);
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public ScenarioBasicDTO update(@PathParam("id") UUID id, @QueryParam("companyId") UUID companyId,
            ScenarioCreateDTO dto) {
        AiScenarioEntity s = AiScenarioEntity.findById(id);
        if (s == null || !Objects.equals(s.companyId, companyId))
            throw new NotFoundException();

        long exists = AiScenarioEntity.count("companyId=?1 and name=?2 and id<>?3",
                companyId, dto.name(), id);
        if (exists > 0)
            throw new ClientErrorException("name already exists in this company", 409);

        s.name = dto.name();
        s.type = dto.type();
        s.iconUrl = dto.iconUrl();
        s.tags = dto.tags() == null ? null : dto.tags().toArray(new String[0]);
        s.description = dto.description();
        s.promptTemplate = dto.promptTemplate();
        s.updatedAt = OffsetDateTime.now();

        return new ScenarioBasicDTO(s.id, s.companyId.toString(), s.name, s.type, s.iconUrl,
                s.tags == null ? List.of() : Arrays.asList(s.tags), s.description,s.promptTemplate);
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Map<String, Object> delete(@PathParam("id") UUID id, @QueryParam("companyId") String companyId) {
        long n = AiScenarioEntity.delete("id=?1 and companyId=?2", id, companyId);
        return Map.of("deleted", n > 0);
    }

    // ---------- Prompt ----------
    @GET
    @Path("/{id}/prompt")
    public ScenarioPromptDTO getPrompt(@PathParam("id") UUID id) {
        AiScenarioEntity s = AiScenarioEntity.findById(id);
        if (s == null)
            throw new NotFoundException();
        return new ScenarioPromptDTO(s.promptTemplate);
    }

    @PUT
    @Path("/{id}/prompt")
    @Transactional
    public ScenarioPromptDTO putPrompt(@PathParam("id") UUID id, ScenarioPromptDTO dto) {
        AiScenarioEntity s = AiScenarioEntity.findById(id);
        if (s == null)
            throw new NotFoundException();
        s.promptTemplate = dto.promptTemplate();
        s.updatedAt = java.time.OffsetDateTime.now();
        return new ScenarioPromptDTO(s.promptTemplate);
    }

    // ---------- Samples ----------
    @GET
    @Path("/{id}/samples")
    public List<SampleDTO> listSamples(@PathParam("id") UUID id) {
        AiScenarioEntity s = AiScenarioEntity.findById(id);
        if (s == null)
            throw new NotFoundException();
        return AiScenarioSampleEntity.<AiScenarioSampleEntity>list("scenario = ?1", Sort.by("position"), s).stream()
                .map(x -> new SampleDTO(x.id, x.position, x.sampleQ, x.sampleA)).toList();
    }

    @POST
    @Path("/{id}/samples")
    @Transactional
    public SampleDTO addSample(@PathParam("id") UUID id, SampleDTO dto) {
        AiScenarioEntity s = AiScenarioEntity.findById(id);
        if (s == null)
            throw new NotFoundException();
        AiScenarioSampleEntity x = new AiScenarioSampleEntity();
        x.id = UUID.randomUUID();
        x.scenario = s;
        x.position = dto.position();
        x.sampleQ = dto.sampleQ();
        x.sampleA = dto.sampleA();
        x.persist();
        return new SampleDTO(x.id, x.position, x.sampleQ, x.sampleA);
    }

    @DELETE
    @Path("/{id}/samples/{sampleId}")
    @Transactional
    public Map<String, Object> removeSample(@PathParam("id") UUID id, @PathParam("sampleId") UUID sampleId) {
        boolean ok = AiScenarioSampleEntity.delete("id=?1 and scenario.id=?2", sampleId, id) > 0;
        return Map.of("deleted", ok);
    }

    // ---------- Options ----------
    @GET
    @Path("/{id}/options")
    public ScenarioOptionsDTO getOptions(@PathParam("id") UUID id) {
        AiScenarioOptionEntity opt = AiScenarioOptionEntity.findById(id);
        if (opt == null)
            throw new NotFoundException();
        return new ScenarioOptionsDTO(opt.saveHistory, opt.allowUpload);
    }

    @PUT
    @Path("/{id}/options")
    @Transactional
    public ScenarioOptionsDTO putOptions(@PathParam("id") UUID id, ScenarioOptionsDTO dto) {
        AiScenarioEntity s = AiScenarioEntity.findById(id);
        if (s == null)
            throw new NotFoundException();
        AiScenarioOptionEntity opt = AiScenarioOptionEntity.findById(id);
        if (opt == null) {
            opt = new AiScenarioOptionEntity();
            opt.scenarioId = id;
            opt.scenario = s;
        }
        opt.saveHistory = dto.saveHistory();
        opt.allowUpload = dto.allowUpload();
        opt.persist();
        return new ScenarioOptionsDTO(opt.saveHistory, opt.allowUpload);
    }

    @POST
    @Path("/{id}/analyze")
    public RunResponseDTO analyze(@PathParam("id") UUID id, @QueryParam("companyId") UUID companyId,
            AnalyzeProxyReq req) throws Exception {
        AiScenarioEntity s = AiScenarioEntity.findById(id);
        if (s == null || !Objects.equals(s.companyId, companyId))
            throw new NotFoundException();

        AiScenarioRunEntity run = analyzeService.analyzeAndSave(s, req.fileName(), req.model());
        if (run.companyId == null) {
            run.companyId = s.companyId;
            run.persist();
        }
        return new RunResponseDTO(run.id, run.fileName, run.modelUsed,
                run.resultMarkdown, run.promptUsed, run.status);
    }

    @GET
    @Path("/{id}/runs")
    public List<RunResponseDTO> runs(@PathParam("id") UUID id, @QueryParam("companyId") UUID companyId) {
        AiScenarioEntity s = AiScenarioEntity.findById(id);
        if (s == null || !Objects.equals(s.companyId, companyId))
            throw new NotFoundException();
        return AiScenarioRunEntity.<AiScenarioRunEntity>list("scenario=?1 and companyId=?2",
                Sort.by("createdAt").descending(), s, companyId)
                .stream()
                .map(r -> new RunResponseDTO(r.id, r.fileName, r.modelUsed, r.resultMarkdown, r.promptUsed, r.status))
                .toList();
    }
}

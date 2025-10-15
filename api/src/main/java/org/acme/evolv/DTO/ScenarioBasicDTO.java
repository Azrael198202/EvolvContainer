package org.acme.evolv.dto;

import java.util.List;
import java.util.UUID;

public record ScenarioBasicDTO(
        UUID id, 
        String companyId,
        String name, 
        String type, 
        String iconUrl,
        List<String> tags, 
        String description,
        String prompt
) {}
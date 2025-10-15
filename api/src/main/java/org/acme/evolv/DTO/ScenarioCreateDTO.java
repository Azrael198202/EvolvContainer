package org.acme.evolv.dto;

import java.util.List;

public record ScenarioCreateDTO(
        String companyId,
        String name, 
        String type, 
        String iconUrl,
        List<String> tags, 
        String description,
        String promptTemplate
) {}
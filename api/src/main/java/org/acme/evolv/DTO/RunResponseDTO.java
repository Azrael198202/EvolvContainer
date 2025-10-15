package org.acme.evolv.dto;

import java.util.UUID;

public record RunResponseDTO(
        UUID runId, String fileName, String modelUsed,
        String resultMarkdown, String promptUsed, String status
) {}
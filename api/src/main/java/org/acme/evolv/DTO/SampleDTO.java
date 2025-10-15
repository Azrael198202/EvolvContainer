package org.acme.evolv.dto;

import java.util.UUID;

public record SampleDTO(UUID id, Integer position, String sampleQ, String sampleA) {}

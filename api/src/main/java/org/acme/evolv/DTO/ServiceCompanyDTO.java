package org.acme.evolv.dto;
import java.util.UUID;

public record ServiceCompanyDTO (UUID id, String name, String url, String desc) {}

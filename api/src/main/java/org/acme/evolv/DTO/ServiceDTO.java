package org.acme.evolv.dto;
import java.util.UUID;

public record ServiceDTO (UUID id, String name, String url, String desc, Long price, Long category_id, Long supplier_id, Boolean is_active) {}

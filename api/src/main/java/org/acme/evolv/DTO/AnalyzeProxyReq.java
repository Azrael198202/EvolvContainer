package org.acme.evolv.dto;

public record AnalyzeProxyReq(
        String companyId,
        String fileName,            // 仅用于记录
        String model                // 透传给 Python，可选
) {}
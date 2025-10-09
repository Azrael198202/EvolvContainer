package org.acme.evolv.factory;

import java.nio.file.*;
import java.util.*;
import org.jboss.logging.Logger;

public class VueProjectService {
    private static final Logger LOG = Logger.getLogger(VueProjectService.class);

    // ---- Copy template ----
    public void copyTemplate(Path src, Path dst, boolean overwrite) throws Exception {
        final Set<String> excludeDirs = Set.of(".git", "node_modules", "dist", "build", ".cache");
        final Set<String> excludeFiles = Set.of(".gitignore", ".DS_Store");

        Files.createDirectories(dst);
        try (var stream = Files.walk(src)) {
            stream.forEach(p -> {
                try {
                    Path rel = src.relativize(p);
                    if (rel.toString().isEmpty()) return;
                    for (Path part : rel)
                        if (excludeDirs.contains(part.getFileName().toString())) return;
                    if (excludeFiles.contains(rel.getFileName().toString())) return;

                    Path target = dst.resolve(rel);
                    if (Files.isDirectory(p)) {
                        Files.createDirectories(target);
                    } else {
                        Files.createDirectories(target.getParent());
                        Files.copy(p, target,
                                overwrite ? new CopyOption[] { StandardCopyOption.REPLACE_EXISTING } : new CopyOption[0]);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("copy failed at: " + p + " -> " + e.getMessage(), e);
                }
            });
        }
    }

    // ---- Patch ChatComponent.tsx ----
    public void patchChatComponent(Path appDir, String apiUrl) throws Exception {
        Path p = appDir.resolve("src").resolve("ChatComponent.tsx");
        if (!Files.exists(p)) {
            Path alt = appDir.resolve("src").resolve("components").resolve("ChatComponent.tsx");
            if (Files.exists(alt)) p = alt; else return;
        }
        String text = Files.readString(p);
        String safeUrl = apiUrl.replace("\\", "\\\\").replace("\"", "\\\"");
        String patternApi = "const\\s+API_URL\\s*=\\s*[\"'][^\"']*[\"'];?";
        String replacementApi = "const API_URL = \"" + safeUrl + "\";";
        text = text.replaceAll(patternApi, replacementApi);
        text = text.replaceAll("fetch\\(\\s*[\"']\\/api\\/ask[\"']\\s*,", "fetch(API_URL,");
        text = text.replaceAll("\\$\\{\\s*import\\.meta\\.env\\.VITE_API_BASE_URL\\s*\\}/ask", apiUrl);
        Files.writeString(p, text);
        LOG.info("[patchChatComponent] Updated ChatComponent.tsx with API_URL=" + apiUrl);
    }

    // ---- Relax TypeScript strict rules ----
    public void relaxTypeChecks(Path appDir) throws Exception {
        patchPackageJson(appDir);
        patchTsConfig(appDir);
    }

    private void patchPackageJson(Path appDir) throws Exception {
        Path pkg = appDir.resolve("package.json");
        if (!Files.exists(pkg)) return;
        String t = Files.readString(pkg);
        String r = t.replaceAll("\"build\"\\s*:\\s*\"[^\"]*tsc[^\"]*&&\\s*vite build[^\"]*\"", "\"build\": \"vite build\"");
        if (!r.equals(t)) Files.writeString(pkg, r);
    }

    private void patchTsConfig(Path appDir) throws Exception {
        for (String f : List.of("tsconfig.json", "tsconfig.app.json", "tsconfig.build.json")) {
            Path cfg = appDir.resolve(f);
            if (!Files.exists(cfg)) continue;
            String t = Files.readString(cfg);
            if (!t.contains("noUnusedLocals")) {
                t = t.replaceFirst("\\{", "{\n  \"compilerOptions\": {\"noUnusedLocals\": false, \"noUnusedParameters\": false},");
                Files.writeString(cfg, t);
            }
            break;
        }
    }
}

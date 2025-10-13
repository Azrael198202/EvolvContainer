package org.acme.evolv.factory.services;

import java.nio.file.*;
import java.util.*;

import org.acme.evolv.DTO.ChatConfig;
import org.acme.evolv.factory.patcher.ChatComponentPatcher;
import org.acme.evolv.factory.patcher.CssThemePatcher;
import org.acme.evolv.interfaces.ChatConfigRepo;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
public class VueProjectService {
    private static final Logger LOG = Logger.getLogger(VueProjectService.class);
    private final ChatComponentPatcher tsxPatcher = new ChatComponentPatcher();
    private final CssThemePatcher cssPatcher = new CssThemePatcher();

    @Inject
    @Named("jdbcRepo")
    ChatConfigRepo configRepo;

    // ---- Copy template ----
    public void copyTemplate(Path src, Path dst, boolean overwrite) throws Exception {
        final Set<String> excludeDirs = Set.of(".git", "node_modules", "dist", "build", ".cache");
        final Set<String> excludeFiles = Set.of(".gitignore", ".DS_Store");

        Files.createDirectories(dst);
        LOG.debug("Copying template from " + src + " to " + dst + ", overwrite=" + overwrite);
        try (var stream = Files.walk(src)) {
            stream.forEach(p -> {
                try {
                    Path rel = src.relativize(p);
                    if (rel.toString().isEmpty())
                        return;
                    for (Path part : rel)
                        if (excludeDirs.contains(part.getFileName().toString()))
                            return;
                    if (excludeFiles.contains(rel.getFileName().toString()))
                        return;

                    Path target = dst.resolve(rel);
                    if (Files.isDirectory(p)) {
                        Files.createDirectories(target);
                    } else {
                        Files.createDirectories(target.getParent());
                        Files.copy(p, target,
                                overwrite ? new CopyOption[] { StandardCopyOption.REPLACE_EXISTING }
                                        : new CopyOption[0]);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("copy failed at: " + p + " -> " + e.getMessage(), e);
                }
            });
        }
    }

    // ---- Patchers orchestration ----
    public void patchTsx(Path appDir, String companyId) throws Exception {
        ChatConfig cfg = requireConfig(companyId);
        tsxPatcher.patch(
                appDir,
                nullToEmpty(cfg.apiUrl()),
                nullToEmpty(cfg.welcomeText()),
                nullToEmpty(cfg.messageIconUrl()),
                nullToEmpty(cfg.headerIconUrl()));
        LOG.infof("[patchTsx] company=%s done", companyId);
    }

    public void patchCss(Path appDir, String companyId) throws Exception {
        ChatConfig cfg = requireConfig(companyId);
        var theme = new CssThemePatcher.ThemeCfg(
                nullToEmpty(cfg.themePrimary()),
                nullToEmpty(cfg.contentBg()),
                nullToEmpty(cfg.footerBg()),
                nullToEmpty(cfg.textColor()),
                nullToEmpty(cfg.bubbleUser()),
                nullToEmpty(cfg.bubbleBot()));
        cssPatcher.patch(appDir, theme);
        LOG.infof("[patchCss] company=%s done", companyId);
    }

    // ---- Relax TypeScript strict rules ----
    public void relaxTypeChecks(Path appDir) throws Exception {
        patchPackageJson(appDir);
        patchTsConfig(appDir);
    }

    private void patchPackageJson(Path appDir) throws Exception {
        Path pkg = appDir.resolve("package.json");
        if (!Files.exists(pkg))
            return;
        String t = Files.readString(pkg);
        String r = t.replaceAll("\"build\"\\s*:\\s*\"[^\"]*tsc[^\"]*&&\\s*vite build[^\"]*\"",
                "\"build\": \"vite build\"");
        if (!r.equals(t))
            Files.writeString(pkg, r);
    }

    private void patchTsConfig(Path appDir) throws Exception {
        for (String f : List.of("tsconfig.json", "tsconfig.app.json", "tsconfig.build.json")) {
            Path cfg = appDir.resolve(f);
            if (!Files.exists(cfg))
                continue;
            String t = Files.readString(cfg);
            if (!t.contains("noUnusedLocals")) {
                t = t.replaceFirst("\\{",
                        "{\n  \"compilerOptions\": {\"noUnusedLocals\": false, \"noUnusedParameters\": false},");
                Files.writeString(cfg, t);
            }
            break;
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private ChatConfig requireConfig(String companyId) {
        ChatConfig cfg = configRepo.findByCompanyId(companyId);
        if (cfg == null) {
            throw new IllegalStateException("ChatConfig not found for companyId=" + companyId);
        }
        return cfg;
    }
}

package org.acme.evolv.factory.patcher;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.logging.Logger;

/** 负责把主题色等注入到 App.css（或任意样式） */
public class CssThemePatcher {
    private static final Logger LOG = Logger.getLogger(CssThemePatcher.class);

    /** 主题配置（可按需扩展） */
    public static record ThemeCfg(
        String primary,     // --brand-primary
        String contentBg,   // --app-bg
        String footerBg,    // --footer-bg
        String textColor,   // --color-text-1
        String bubbleUser,  // --bubble-user
        String bubbleBot    // --bubble-bot
    ) {}

    /** 查找并 patch 样式文件（默认 App.css；找不到则尝试 src/styles/chat-template.css） */
    public void patch(Path appDir, ThemeCfg cfg) throws Exception {
        Path css = resolveCss(appDir);
        if (css == null) {
            LOG.warnf("[CssThemePatcher] CSS file not found under %s", appDir);
            return;
        }
        String t = Files.readString(css, StandardCharsets.UTF_8);

        // 先替换显式占位符（如果模板采用了 {theme_primary} 这种）
        Map<String,String> placeholders = Map.of(
            "\\{theme_primary\\}", nn(cfg.primary()),
            "\\{content_bg\\}",   nn(cfg.contentBg()),
            "\\{footer_bg\\}",    nn(cfg.footerBg()),
            "\\{text_color\\}",   nn(cfg.textColor()),
            "\\{bubble_user\\}",  nn(cfg.bubbleUser()),
            "\\{bubble_bot\\}",   nn(cfg.bubbleBot())
        );
        for (var e : placeholders.entrySet()) {
            t = t.replaceAll(e.getKey(), Matcher.quoteReplacement(e.getValue()));
        }

        // 然后确保 :root 里有 CSS 变量（若不存在则插入；存在则覆盖）
        t = upsertRootCssVars(t, Map.of(
            "--brand-primary", nn(cfg.primary()),
            "--app-bg",        nn(cfg.contentBg()),
            "--footer-bg",     nn(cfg.footerBg()),
            "--color-text-1",  nn(cfg.textColor()),
            "--bubble-user",   nn(cfg.bubbleUser()),
            "--bubble-bot",    nn(cfg.bubbleBot())
        ));

        Files.writeString(css, t, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
        LOG.infof("[CssThemePatcher] Patched %s", css);
    }

    private Path resolveCss(Path appDir) {
        List<Path> candidates = List.of(
            appDir.resolve("src/App.css"),
            appDir.resolve("src/styles/chat-template.css"),
            appDir.resolve("src/app/globals.css")
        );
        for (Path p : candidates) if (Files.exists(p)) return p;
        return null;
    }

    /** 在 :root {...} 内 upsert 变量；若没有 :root 则创建一个并放到文件最前面 */
    private String upsertRootCssVars(String css, Map<String,String> vars) {
        Pattern rootPat = Pattern.compile(":root\\s*\\{([\\s\\S]*?)\\}", Pattern.MULTILINE);
        Matcher m = rootPat.matcher(css);
        if (m.find()) {
            String body = m.group(1);
            for (var e : vars.entrySet()) {
                body = upsertVarLine(body, e.getKey(), e.getValue());
            }
            return css.substring(0, m.start(1)) + body + css.substring(m.end(1));
        } else {
            StringBuilder sb = new StringBuilder(":root{\n");
            vars.forEach((k,v) -> sb.append("  ").append(k).append(": ").append(v).append(";\n"));
            sb.append("}\n\n");
            return sb.insert(0, "").append(css).toString();
        }
    }

    private String upsertVarLine(String body, String key, String val) {
        Pattern p = Pattern.compile("(^|\\n)\\s*" + Pattern.quote(key) + "\\s*:\\s*[^;]*;", Pattern.MULTILINE);
        Matcher m = p.matcher(body);
        if (m.find()) {
            return m.replaceFirst(m.group(1) + "  " + key + ": " + val + ";");
        } else {
            // 追加到结尾
            if (!body.endsWith("\n")) body += "\n";
            return body + "  " + key + ": " + val + ";\n";
        }
    }

    private static String nn(String s) { return (s == null || s.isBlank()) ? "" : s; }
}

package org.acme.evolv.factory.patcher;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.regex.Pattern;
import org.jboss.logging.Logger;

public class ChatComponentPatcher {
    private static final Logger LOG = Logger.getLogger(ChatComponentPatcher.class);

    /** 替换 ChatComponent.tsx 中的 API_URL / 欢迎语 / 头像等 */
    public void patch(Path appDir, String apiUrl, String initMessage, String avatarUrl) throws Exception {
        Path p = findChatComponent(appDir);
        if (p == null) {
            LOG.warnf("[ChatComponentPatcher] ChatComponent.tsx not found under %s", appDir);
            return;
        }

        String text = Files.readString(p, StandardCharsets.UTF_8);
        String safeApi = escapeForJsString(apiUrl);
        String safeMsg = escapeForJsString(initMessage);
        String safeAvatar = escapeForJsString(avatarUrl);

        // 1) 统一 API_URL 定义
        text = replaceFirstRegex(text,
            "const\\s+API_URL\\s*=\\s*[\"'][^\"']*[\"'];?",
            "const API_URL = \"" + safeApi + "\";"
        );
        // 兼容以前硬编码 /api/ask
        text = text.replaceAll("fetch\\(\\s*[\"']\\/api\\/ask[\"']\\s*,", "fetch(API_URL,");

        // 兼容 vite env 写法
        text = text.replaceAll("\\$\\{\\s*import\\.meta\\.env\\.VITE_API_BASE_URL\\s*\\}\\s*\\/ask", safeApi);

        // 2) 欢迎语/头像占位（如果有占位符就替换；否则尝试替换常量）
        text = replaceFirstRegex(text,
            "const\\s+COMPANY_AVATAR\\s*=\\s*[\"'][^\"']*[\"'];?",
            "const COMPANY_AVATAR = \"" + safeAvatar + "\";"
        );
        text = replaceFirstRegex(text,
            "const\\s+INIT_MESSAGE\\s*=\\s*[\"`][\\s\\S]*?[\"`];?",
            "const INIT_MESSAGE = \"" + safeMsg + "\";"
        );
        // 常见占位符
        text = text.replace("{img_avatar}", "\"" + safeAvatar + "\"");
        text = text.replace("{init_message}", "\"" + safeMsg + "\"");
        text = text.replace("{api_url}", "\"" + safeApi + "\"");

        Files.writeString(p, text, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
        LOG.infof("[ChatComponentPatcher] Patched %s", p);
    }

    private Path findChatComponent(Path appDir) {
        Path p = appDir.resolve("src/ChatComponent.tsx");
        if (Files.exists(p)) return p;
        p = appDir.resolve("src/components/ChatComponent.tsx");
        if (Files.exists(p)) return p;
        return null;
    }

    private static String replaceFirstRegex(String src, String regex, String replacement) {
        return Pattern.compile(regex, Pattern.MULTILINE).matcher(src).replaceFirst(replacement);
    }

    private static String escapeForJsString(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

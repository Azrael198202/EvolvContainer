package org.acme.evolv.factory.patcher;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.regex.Pattern;
import org.jboss.logging.Logger;

public class ChatComponentPatcher {
    private static final Logger LOG = Logger.getLogger(ChatComponentPatcher.class);

    public void patch(Path appDir, String apiUrl, String header, String initMessage, String avatarUrl, String messageIcon) throws Exception {
        Path p = findChatComponent(appDir);
        if (p == null) {
            LOG.warnf("[ChatComponentPatcher] ChatComponent.tsx not found under %s", appDir);
            return;
        }

        String text = Files.readString(p, StandardCharsets.UTF_8);
        String safeApi = escapeForJsString(apiUrl);
        String safeTitle = escapeForJsString(header);
        String safeMsg = escapeForJsString(initMessage);
        String safeAvatar = escapeForJsString(avatarUrl);
        String safeMessageIcon = escapeForJsString(messageIcon);

        text = replaceFirstRegex(text,
            "const\\s+API_URL\\s*=\\s*[\"'][^\"']*[\"'];?",
            "const API_URL = \"" + safeApi + "\";"
        );
        text = text.replaceAll("fetch\\(\\s*[\"']\\/api\\/ask[\"']\\s*,", "fetch(API_URL,");

        text = text.replaceAll("\\$\\{\\s*import\\.meta\\.env\\.VITE_API_BASE_URL\\s*\\}\\s*\\/ask", safeApi);

        text = replaceFirstRegex(text,
            "const\\s+COMPANY_AVATAR\\s*=\\s*[\"'][^\"']*[\"'];?",
            "const COMPANY_AVATAR = \"" + safeAvatar + "\";"
        );
        text = replaceFirstRegex(text,
            "const\\s+INIT_MESSAGE\\s*=\\s*[\"`][\\s\\S]*?[\"`];?",
            "const INIT_MESSAGE = \"" + safeMsg + "\";"
        );

        text = text.replace("{img_avatar}", "\"" + safeAvatar + "\"");
        //text = text.replace("{{img_avatar}}", safeAvatar);
        text = text.replace("{chat_title}", safeTitle);
        text = text.replace("{init_message}", "\"" + safeMsg + "\"");
        text = text.replace("{api_url}", "\"" + safeApi + "\"");
        text = text.replace("{img_title}", safeMessageIcon);

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

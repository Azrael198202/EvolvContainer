package org.acme.evolv.factory.shell;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public class Shell {
    private static final boolean IS_WINDOWS =
            System.getProperty("os.name").toLowerCase().contains("win");

    public static String run(List<String> cmd, File workdir, Duration timeout) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (workdir != null) pb.directory(workdir);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = p.getInputStream()) { is.transferTo(baos); }

        if (!p.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)) {
            p.destroyForcibly();
            throw new RuntimeException("Command timeout: " + String.join(" ", cmd));
        }
        String out = baos.toString(StandardCharsets.UTF_8);
        if (p.exitValue() != 0) {
            throw new RuntimeException("Command failed(" + p.exitValue()+"): " + String.join(" ", cmd) + "\n" + out);
        }
        return out;
    }

    public static String runLine(String line, File workdir, Duration timeout) throws Exception {
        List<String> cmd = IS_WINDOWS
                ? List.of("cmd", "/c", line)
                : List.of("bash", "-lc", line);
        return run(cmd, workdir, timeout);
    }
}

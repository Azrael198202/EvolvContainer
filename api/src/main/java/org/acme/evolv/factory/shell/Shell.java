package org.acme.evolv.factory.shell;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class Shell {

    public static boolean isWindows() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("win");
    }

    public static boolean isUnixLike() {
        return !isWindows(); // Linux / macOS
    }

    /**
     * Execute the command directly (without forcing a shell).
     * Suitable for executables like `docker` that can be run directly.
     * @param cmd The full command with arguments (already tokenized).
     * @param timeout Timeout duration.
     * @param ignoreNonZeroExit Whether to ignore non-zero exit codes.
     */
    public static String runDirect(List<String> cmd, Duration timeout, boolean ignoreNonZeroExit)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        // For simplicity, this call blocks until completion; for robustness, consider async reading via threads/selectors.
        boolean finished = p.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!finished) {
            p.destroyForcibly();
            throw new RuntimeException("Command timeout: " + String.join(" ", cmd));
        }
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int code = p.exitValue();
        if (code != 0 && !ignoreNonZeroExit) {
            throw new RuntimeException("Command failed(" + code + "): " + String.join(" ", cmd) + "\n" + out);
        }
        return out;
    }

    /**
     * Execute via shell (use only when shell syntax such as pipes, redirection, or '|| true' is required).
     * - Windows: uses `cmd /c`
     * - *nix: uses `bash -lc`
     */
    public static String runViaShell(String command, Duration timeout, boolean ignoreNonZeroExit)
            throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        if (isWindows()) {
            cmd.add("cmd"); cmd.add("/c"); cmd.add(command);
        } else {
            cmd.add("bash"); cmd.add("-lc"); cmd.add(command);
        }
        return runDirect(cmd, timeout, ignoreNonZeroExit);
    }

    /**
     * Cross-platform Docker execution helper:
     * On Windows, you can specify the absolute path to `docker.exe` to avoid PATH issues.
     * Pass `null` to use the default command `"docker"`.
     */
    public static String runDocker(List<String> dockerArgs, Duration timeout, boolean ignoreNonZeroExit,
                                   String windowsDockerAbsolutePathIfNeeded)
            throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        if (isWindows()) {
            // Prefer absolute path; otherwise rely on the docker command in PATH.
            String dockerCmd = windowsDockerAbsolutePathIfNeeded != null
                    ? windowsDockerAbsolutePathIfNeeded
                    : "docker";
            cmd.add(dockerCmd);
        } else {
            cmd.add("docker");
        }
        cmd.addAll(dockerArgs);
        return runDirect(cmd, timeout, ignoreNonZeroExit);
    }
}

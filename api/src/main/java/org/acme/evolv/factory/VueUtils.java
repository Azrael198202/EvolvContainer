package org.acme.evolv.factory;

import org.jboss.logging.Logger;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/*
 * isWindows, npmCmd, npxCmd, dockerCmd
 * runDirect
 * runNpmCiOrInstall, runNpmRunBuild, runNpxCreateVite
 */
public class VueUtils {
    private static final Logger LOG = Logger.getLogger(VueUtils.class);

    private static final Duration LONG = Duration.ofMinutes(15);

    private static boolean isWindows() {
        LOG.debug("Checking if OS is Windows");
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("win");
    }

    public static String npmCmd() {
        return isWindows() ? "npm.cmd" : "npm";
    }

    public static String npxCmd() {
        return isWindows() ? "npx.cmd" : "npx";
    }

    public static String dockerCmd() {
        return isWindows() ? "docker.exe" : "docker";
    }


    /*
     * exec cmd in workDir, wait up to timeout.
     * return combined stdout+stderr.
     * if ignoreNonZeroExit is false, throw if exit code != 0.
     * logs are printed to console in real-time.
     */
    public static String runDirect(List<String> cmd, File workDir, Duration timeout, boolean ignoreNonZeroExit)
            throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (workDir != null) pb.directory(workDir);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        StringBuilder logBuffer = new StringBuilder();
        Thread outThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[proc] " + line);
                    logBuffer.append(line).append("\n");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        outThread.start();

        boolean finished = p.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!finished) {
            p.destroyForcibly();
            throw new RuntimeException("Command timeout: " + String.join(" ", cmd));
        }
        outThread.join();

        int code = p.exitValue();
        String out = logBuffer.toString();
        if (code != 0 && !ignoreNonZeroExit) {
            throw new RuntimeException("Command failed(" + code + "): " + String.join(" ", cmd) + "\n" + out);
        }
        return out;
    }

    // ------------------ npm 辅助函数 ------------------

    public static String runNpmCiOrInstall(File workDir, boolean useCi) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(npmCmd());
        cmd.add(useCi ? "ci" : "install");
        cmd.add("--no-audit");
        cmd.add("--no-fund");
        return runDirect(cmd, workDir, LONG, false);
    }

    public static String runNpmRunBuild(File workDir) throws Exception {
        List<String> cmd = List.of(npmCmd(), "run", "build");
        return runDirect(cmd, workDir, LONG, false);
    }

    public static String runNpxCreateVite(File workDir, String appName) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(npxCmd());
        cmd.add("--yes");
        cmd.add("create-vite@latest");
        cmd.add(appName);
        cmd.add("--");
        cmd.add("--template");
        cmd.add("vue");
        return runDirect(cmd, workDir, LONG, false);
    }
}

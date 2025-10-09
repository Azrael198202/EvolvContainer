package org.acme.evolv.factory;

import org.jboss.logging.Logger;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Docker 
 * - build images
 * - run/stop containers
 * - copy files to/from containers
 * - exec commands in containers
 * - etc.
 * Uses direct 'docker' CLI calls, no Docker SDK.
 * Assumes 'docker' is in PATH and user has permission to run it.
 * Note: on Windows, this means running in a terminal with admin rights.
 * Note: this is a simple implementation, not production-ready.
 *     No retries, no advanced error handling, no Windows support, etc.
 *   Just enough to get the job done.
 */
public class DockerService {
    private static final Logger LOG = Logger.getLogger(DockerService.class);

    private static final Duration MID  = Duration.ofMinutes(5);
    private static final Duration LONG = Duration.ofMinutes(15);

    // ---------- helpers ----------
    private static boolean isWindows() {
        LOG.debug("Checking if OS is Windows");
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("win");
    }
    private static String dockerCmd() { return isWindows() ? "docker.exe" : "docker"; }

    private static String run(List<String> cmd, File workDir, Duration timeout, boolean ignoreNonZeroExit) throws Exception {
        return VueUtils.runDirect(cmd, workDir, timeout, ignoreNonZeroExit);
    }

    private static String run(List<String> cmd, File workDir, Duration timeout,
                              boolean ignoreNonZeroExit, String streamId, LogSseHub hub) throws Exception {
        return VueUtils.runDirect(cmd, workDir, timeout, ignoreNonZeroExit,
                line -> { if (hub != null && streamId != null) hub.send(streamId, line); });
    }

    // ---------- container lifecycle ----------
    public boolean exists(String container) throws Exception {
        try {
            run(List.of(dockerCmd(), "inspect", container), null, MID, false);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
    public boolean isRunning(String container) throws Exception {
        String out = run(List.of(dockerCmd(), "inspect", "-f", "{{.State.Running}}", container), null, MID, false);
        return out.trim().equalsIgnoreCase("true");
    }
    public void ensureRunning(String container) throws Exception {
        if (!isRunning(container)) {
            run(List.of(dockerCmd(), "start", container), null, MID, false);
        }
    }

    // ---------- build/run ----------
    public String build(File dir, String image) throws Exception {
        return run(List.of(dockerCmd(), "build", "--no-cache", "--progress=plain", "-t", image, "."),
                   dir, LONG, false);
    }
    public String build(File dir, String image, String streamId, LogSseHub hub) throws Exception {
        return run(List.of(dockerCmd(), "build", "--no-cache", "--progress=plain", "-t", image, "."),
                   dir, LONG, false, streamId, hub);
    }

    public String runDetached(String container, int port, String image) throws Exception {
        return run(List.of(dockerCmd(), "run", "-d", "--name", container, "-p", port + ":80", image),
                   null, MID, false);
    }
    public String runDetached(String container, int port, String image, String streamId, LogSseHub hub) throws Exception {
        return run(List.of(dockerCmd(), "run", "-d", "--name", container, "-p", port + ":80", image),
                   null, MID, false, streamId, hub);
    }

    public String rmForce(String container) throws Exception {
        return run(List.of(dockerCmd(), "rm", "-f", container), null, MID, true);
    }
    public String rmForce(String container, String streamId, LogSseHub hub) throws Exception {
        return run(List.of(dockerCmd(), "rm", "-f", container), null, MID, true, streamId, hub);
    }

    // ---------- cp / exec ----------
    public String cpToContainer(java.nio.file.Path src, String container, String dst) throws Exception {
        return run(List.of(dockerCmd(), "cp", src.toString(), container + ":" + dst), null, LONG, false);
    }
    public String cpToContainer(java.nio.file.Path src, String container, String dst, String streamId, LogSseHub hub) throws Exception {
        return run(List.of(dockerCmd(), "cp", src.toString(), container + ":" + dst), null, LONG, false, streamId, hub);
    }

    public String exec(String container, String cmd) throws Exception {
        return run(List.of(dockerCmd(), "exec", container, "/bin/sh", "-lc", cmd), null, MID, false);
    }
    public String exec(String container, String cmd, String streamId, LogSseHub hub) throws Exception {
        return run(List.of(dockerCmd(), "exec", container, "/bin/sh", "-lc", cmd), null, MID, false, streamId, hub);
    }

    public String execSafe(String container, String cmd) throws Exception {
        List<String> shell = pickShellArgs(container);
        List<String> args = new ArrayList<>();
        args.add(dockerCmd()); args.add("exec"); args.add(container);
        args.addAll(shell);    args.add(cmd);
        return run(args, null, MID, false);
    }
    public String execSafe(String container, String cmd, String streamId, LogSseHub hub) throws Exception {
        List<String> shell = pickShellArgs(container);
        List<String> args = new ArrayList<>();
        args.add(dockerCmd()); args.add("exec"); args.add(container);
        args.addAll(shell);    args.add(cmd);
        return run(args, null, MID, false, streamId, hub);
    }

    // ---------- shell detection ----------
    private List<String> pickShellArgs(String container) throws Exception {
        String[] shells = { "/bin/sh", "/bin/ash", "/bin/bash" };
        for (String s : shells) {
            try {
                run(List.of(dockerCmd(), "exec", container, s, "-lc", "echo ok"), null, MID, false);
                return List.of(s, "-lc");
            } catch (RuntimeException ignore) { /* try next */ }
        }
        return List.of("/bin/sh", "-lc"); // fallback
    }
}

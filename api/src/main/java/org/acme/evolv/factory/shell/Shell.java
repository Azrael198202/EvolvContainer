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
        return !isWindows(); // Linux / macOS 想同处理
    }

    /**
     * 直接执行命令（不强行包 shell）。适合 docker 等本身就是可执行文件的命令。
     * @param cmd 完整命令及参数（已分词）
     * @param timeout 超时时间
     * @param ignoreNonZeroExit 是否忽略非 0 退出码
     */
    public static String runDirect(List<String> cmd, Duration timeout, boolean ignoreNonZeroExit)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        // 简洁起见：阻塞等待；若需更稳健可做线程/selector 异步读
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
     * 通过 shell 执行（仅在需要用管道、重定向、'|| true' 等 shell 语法时使用）
     * - Windows: cmd /c
     * - *nix: bash -lc
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
     * 跨平台 docker 执行帮助：Windows 下可指定 docker.exe 绝对路径，避免 PATH 问题。
     * 传入 null 则使用 "docker"。
     */
    public static String runDocker(List<String> dockerArgs, Duration timeout, boolean ignoreNonZeroExit,
                                   String windowsDockerAbsolutePathIfNeeded)
            throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        if (isWindows()) {
            // 优先绝对路径；否则依赖 PATH 中的 docker
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

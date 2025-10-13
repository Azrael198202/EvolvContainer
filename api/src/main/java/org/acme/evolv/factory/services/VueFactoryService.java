package org.acme.evolv.factory.services;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.acme.evolv.utils.LogSseHub;
import org.acme.evolv.utils.VueUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@ApplicationScoped
public class VueFactoryService {

    private static final Logger LOG = Logger.getLogger(VueFactoryService.class);

    @Inject
    LogSseHub hub;

    @Inject 
    VueProjectService vue;

    @Inject
    DockerService docker;

    @ConfigProperty(name = "factory.workspace", defaultValue = "C:\\\\vue-factory")
    String workspace;

    @ConfigProperty(name = "factory.registryPrefix", defaultValue = "local/vue-")
    String registryPrefix;

    @ConfigProperty(name = "factory.templateVuePath", defaultValue = "E:\\\\lwpw\\\\EvolvAI\\\\sdk-ui\\\\chat-app")
    String templateVuePath;


    @PostConstruct
    void init() {
        LOG.infof("workspace=%s, registryPrefix=%s", workspace, registryPrefix);
    }

    public record Result(String image, String container, String url, String logs) {
    }

    public Result createAndRun(String companyId, String name, int port, String streamId) throws Exception {
        String safe = name.replaceAll("[^a-zA-Z0-9-_]", "-").toLowerCase();

        File root = new File(workspace);
        if (!root.exists() && !root.mkdirs()) {
            throw new RuntimeException("Cannot create workspace: " + root.getAbsolutePath());
        }
        File appDir = new File(root, safe);
        if (appDir.exists()) {
            throw new RuntimeException("App already exists: " + safe);
        }

        StringBuilder log = new StringBuilder();
        var out = streamer(streamId);

        try {
            // 1) npx create-vite
            log.append("create-vite:\n")
                    .append(VueUtils.runNpxCreateVite(root, safe, out)).append("\n");

            // 2) 写 Dockerfile / nginx.conf
            java.nio.file.Files.writeString(new File(appDir, "Dockerfile").toPath(), dockerfile());
            java.nio.file.Files.writeString(new File(appDir, "nginx.conf").toPath(), nginxConf());

            // 3) npm ci / install + build
            boolean useCi = new File(appDir, "package-lock.json").exists();
            log.append("npm ").append(useCi ? "ci" : "install").append(":\n")
                    .append(VueUtils.runNpmCiOrInstall(appDir, useCi, out)).append("\n");

            log.append("npm run build:\n")
                    .append(VueUtils.runNpmRunBuild(appDir, out)).append("\n");

            // 4) docker build / run
            String image = registryPrefix + safe + ":latest";
            String container = "vue-" + safe;

            log.append("docker build:\n")
                    .append(docker.build(appDir, image, streamId, hub)).append("\n");

            String rmOut = docker.rmForce(container, streamId, hub);
            if (rmOut != null && !rmOut.isBlank()) {
                log.append("docker rm (old):\n").append(rmOut).append("\n");
            }

            log.append("docker run:\n")
                    .append(docker.runDetached(container, port, image, streamId, hub)).append("\n");

            String url = "http://localhost:" + port;
            return new Result(image, container, url, log.toString());
        } finally {
            if (streamId != null && !streamId.isBlank()) {
                hub.send(streamId, "[DONE]");
                hub.close(streamId);
            }
        }
    }

    public Result createAndRun(String companyId, String name, int port) throws Exception {
        return createAndRun(companyId, name, port, null);
    }

    public Result createFromTemplate(String companyId, String name, int port, String streamId) throws Exception {
        String safe = name.replaceAll("[^a-zA-Z0-9-_]", "-").toLowerCase();
        File root = new File(workspace);
        if (!root.exists())
            root.mkdirs();

        File appDir = new File(root, safe);
        if (!appDir.exists())
            appDir.mkdirs();

        StringBuilder log = new StringBuilder();
        var out = streamer(streamId);

        try {
            // Step 1: copy + patch
            vue.copyTemplate(Path.of(templateVuePath), appDir.toPath(), true);
            log.append("copy template -> ").append(appDir.getAbsolutePath()).append("\n");
            if (streamId != null && !streamId.isBlank())
                hub.send(streamId, "copy template done");

            vue.patchTsx(appDir.toPath(), companyId);
            vue.patchCss(appDir.toPath(), companyId);

            log.append("patch ChatComponent.tsx\n");
            if (streamId != null && !streamId.isBlank())
                hub.send(streamId, "patch ChatComponent.tsx done");

            // Step 1.5: ensure Dockerfile & nginx.conf
            Path dockerfilePath = appDir.toPath().resolve("Dockerfile");
            Path nginxConfPath = appDir.toPath().resolve("nginx.conf");
            if (!Files.exists(dockerfilePath)) {
                Files.writeString(dockerfilePath, dockerfile());
                log.append("write default Dockerfile\n");
            }
            if (!Files.exists(nginxConfPath)) {
                Files.writeString(nginxConfPath, nginxConf());
                log.append("write default nginx.conf\n");
            }

            // Step 1.75: relax ts checks
            vue.relaxTypeChecks(appDir.toPath());
            log.append("relax ts checks\n");

            // Step 2: npm ci / install + build (real-time output)
            boolean useCi = appDir.toPath().resolve("package-lock.json").toFile().exists();
            log.append("npm ").append(useCi ? "ci" : "install").append(":\n")
                    .append(VueUtils.runNpmCiOrInstall(appDir, useCi, out)).append("\n");

            log.append("npm run build:\n")
                    .append(VueUtils.runNpmRunBuild(appDir, out)).append("\n");

            // Step 3: docker build / run（if exists, just copy dist）
            String image = registryPrefix + safe + ":latest";
            String container = "vue-" + safe;

            if (docker.exists(container)) {
                docker.ensureRunning(container);
                log.append("container exists: ").append(container).append("\n");
                if (streamId != null && !streamId.isBlank())
                    hub.send(streamId, "container exists, updating static files...");

                // clear /usr/share/nginx/html + copy dist/* to it  
                docker.execSafe(container, "mkdir -p /usr/share/nginx/html", streamId, hub);
                docker.execSafe(container, "find /usr/share/nginx/html -mindepth 1 -exec rm -rf {} + || true", streamId,
                        hub);

                docker.execSafe(container, "rm -rf /tmp/distcopy && mkdir -p /tmp/distcopy", streamId, hub);
                docker.cpToContainer(appDir.toPath().resolve("dist"), container, "/tmp/distcopy", streamId, hub);
                docker.execSafe(container, "cp -a /tmp/distcopy/dist/. /usr/share/nginx/html/ && rm -rf /tmp/distcopy",
                        streamId, hub);

                log.append("updated /usr/share/nginx/html from dist\n");
                return new Result(image, container, "http://localhost:" + port, log.toString());
            } else {
                log.append("docker build:\n").append(docker.build(appDir, image, streamId, hub)).append("\n");

                String rmOut = docker.rmForce(container, streamId, hub);
                if (rmOut != null && !rmOut.isBlank())
                    log.append("docker rm (old):\n").append(rmOut).append("\n");

                log.append("docker run:\n").append(docker.runDetached(container, port, image, streamId, hub))
                        .append("\n");

                String url = "http://localhost:" + port;
                return new Result(image, container, url, log.toString());
            }
        } finally {
            if (streamId != null && !streamId.isBlank()) {
                hub.send(streamId, "[DONE]");
                hub.close(streamId);
            }
        }
    }

    private String dockerfile() {
        return """
                FROM nginx:1.27-alpine
                COPY nginx.conf /etc/nginx/nginx.conf
                COPY dist /usr/share/nginx/html
                EXPOSE 80
                CMD ["nginx","-g","daemon off;"]
                    """;
    }

    private String nginxConf() {
        return """
                worker_processes auto;
                events { worker_connections 1024; }
                http {
                    include mime.types;
                    default_type application/octet-stream; sendfile on;
                    server {
                        listen 80;
                        server_name _;
                        root /usr/share/nginx/html;
                        index index.html;
                        location / {
                            try_files $uri $uri/ /index.html;
                        }
                    }
                } """;
    }

    private java.util.function.Consumer<String> streamer(String streamId) {
        return (line) -> {
            if (hub != null && streamId != null && !streamId.isBlank()) {
                hub.send(streamId, line);
            }
        };
    }
}

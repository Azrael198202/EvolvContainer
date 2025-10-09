package org.acme.evolv.factory;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@ApplicationScoped
public class VueFactoryService {

    private static final Logger LOG = Logger.getLogger(VueFactoryService.class);

    @ConfigProperty(name = "factory.workspace", defaultValue = "C:\\\\vue-factory")
    String workspace;

    @ConfigProperty(name = "factory.registryPrefix", defaultValue = "local/vue-")
    String registryPrefix;

    @ConfigProperty(name = "factory.templateVuePath", defaultValue = "E:\\\\lwpw\\\\EvolvAI\\\\sdk-ui\\\\chat-app")
    String templateVuePath;

    private final DockerService docker = new DockerService();
    private final VueProjectService vue = new VueProjectService();

    @PostConstruct
    void init() {
        LOG.infof("workspace=%s, registryPrefix=%s", workspace, registryPrefix);
    }

    public record Result(String image, String container, String url, String logs) {
    }

    public Result createAndRun(String name, int port) throws Exception {
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

        // 2) npx create-vite
        log.append("create-vite:\n")
                .append(VueUtils.runNpxCreateVite(root, safe)).append("\n");

        java.nio.file.Files.writeString(new File(appDir, "Dockerfile").toPath(), dockerfile());
        java.nio.file.Files.writeString(new File(appDir, "nginx.conf").toPath(), nginxConf());

        // 4) npm ci / install + build
        boolean useCi = new File(appDir, "package-lock.json").exists();
        log.append("npm ").append(useCi ? "ci" : "install").append(":\n")
                .append(VueUtils.runNpmCiOrInstall(appDir, useCi)).append("\n");

        log.append("npm run build:\n")
                .append(VueUtils.runNpmRunBuild(appDir)).append("\n");

        // 5) docker build / run
        String image = registryPrefix + safe + ":latest";
        String container = "vue-" + safe;

        log.append("docker build:\n")
                .append(docker.build(appDir, image)).append("\n");

        String rmOut = docker.rmForce(container);
        if (rmOut != null && !rmOut.isBlank()) {
            log.append("docker rm (old):\n").append(rmOut).append("\n");
        }

        log.append("docker run:\n")
                .append(docker.runDetached(container, port, image)).append("\n");

        String url = "http://localhost:" + port;
        return new Result(image, container, url, log.toString());
    }

    public Result createFromTemplate(String name, int port) throws Exception {
        String safe = name.replaceAll("[^a-zA-Z0-9-_]", "-").toLowerCase();
        File root = new File(workspace);
        if (!root.exists())
            root.mkdirs();

        File appDir = new File(root, safe);
        if (!appDir.exists())
            appDir.mkdirs();

        StringBuilder log = new StringBuilder();

        // Step 1: copy + patch
        vue.copyTemplate(Path.of(templateVuePath), appDir.toPath(), true);
        vue.patchChatComponent(appDir.toPath(), "http://192.168.1.199:8000/api/v1/chat/gpt-ask");

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
        vue.relaxTypeChecks(appDir.toPath());

        // Step 2: npm build
        boolean useCi = appDir.toPath().resolve("package-lock.json").toFile().exists();
        VueUtils.runNpmCiOrInstall(appDir, useCi);
        VueUtils.runNpmRunBuild(appDir);

        // Step 3: docker build / run
        String image = registryPrefix + safe + ":latest";
        String container = "vue-" + safe;

        if (docker.exists(container)) {
            docker.ensureRunning(container);
            log.append("container exists: ").append(container).append("\n");
            docker.execSafe(container, "mkdir -p /usr/share/nginx/html");
            docker.cpToContainer(appDir.toPath().resolve("dist"), container, "/usr/share/nginx/html/");
        } else {
            docker.build(appDir, image);
            docker.rmForce(container);
            docker.runDetached(container, port, image);
        }

        return new Result(image, container, "http://localhost:" + port, log.toString());
    }

    private String dockerfile() {
        return """
                FROM node:20-alpine AS build
                WORKDIR /app
                COPY package*.json ./
                RUN npm ci COPY . .
                RUN npm run build

                FROM nginx:1.27-alpine
                COPY nginx.conf /etc/nginx/nginx.conf
                COPY --from=build /app/dist /usr/share/nginx/html
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
}

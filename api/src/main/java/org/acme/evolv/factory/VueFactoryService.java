package org.acme.evolv.factory;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.acme.evolv.factory.shell.Shell;

import java.io.File;
import java.nio.file.Files;
import java.time.Duration;


@ApplicationScoped
public class VueFactoryService {

    private static final Logger LOG = Logger.getLogger(VueFactoryService.class);

    @ConfigProperty(name = "factory.workspace", defaultValue = "C:\\\\vue-factory")
    String workspace;

    @ConfigProperty(name = "factory.registryPrefix", defaultValue = "local/vue-")
    String registryPrefix;

    private static final Duration LONG = Duration.ofMinutes(15);
    private static final Duration MID  = Duration.ofMinutes(5);

    public record Result(String image, String container, String url, String logs){}

    @PostConstruct
    void init(){
        LOG.infof("workspace=%s, registryPrefix=%s", workspace, registryPrefix);
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

        // 1) npx create-vite
        // Windows: use cmd.exe /c to avoid "The filename or extension is too long" error
        String s1 = Shell.runLine(
                "npx --yes create-vite@latest " + safe + " -- --template vue",
                root, LONG);
        log.append("create-vite:\n").append(s1).append("\n");

        // 2) write Dockerfile & nginx.conf
        Files.writeString(new File(appDir, "Dockerfile").toPath(), dockerfile());
        Files.writeString(new File(appDir, "nginx.conf").toPath(), nginxConf());

        // 3) npm ci & npm run build (inside appDir)
        String s3a = Shell.runLine("npm ci", appDir, LONG);
        log.append("npm ci:\n").append(s3a).append("\n");

        String s3b = Shell.runLine("npm run build", appDir, LONG);
        log.append("npm run build:\n").append(s3b).append("\n");

        // 4) docker build
        String image = registryPrefix + safe + ":latest";
        String s4 = Shell.runLine("docker build -t " + image + " .", appDir, LONG);
        log.append("docker build:\n").append(s4).append("\n");

        // 5) docker rm -f <container>
        String container = "vue-" + safe;
        try {
            Shell.runLine("docker rm -f " + container, null, MID);
            log.append("docker rm: removed existing container\n");
        } catch (Exception ignored) {
            log.append("docker rm: none\n");
        }

        // 6) docker run -p <port>:80
        String s5 = Shell.runLine("docker run -d --name " + container + " -p " + port + ":80 " + image,
                null, MID);
        log.append("docker run:\n").append(s5).append("\n");

        String url = "http://localhost:" + port;
        return new Result(image, container, url, log.toString());
    }

    private String dockerfile() {
        return """
            FROM node:20-alpine AS build
            WORKDIR /app
            COPY package*.json ./
            RUN npm ci
            COPY . .
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
              include       mime.types;
              default_type  application/octet-stream;
              sendfile on;
              server {
                listen 80;
                server_name _;
                root /usr/share/nginx/html;
                index index.html;
                location / {
                  try_files $uri $uri/ /index.html;
                }
              }
            }
            """;
    }

    public static int pickPort(String name){
        return 9000 + Math.abs(name.hashCode() % 8000);
    }
}

# calculator

A simple Java calculator (add, subtract, multiply, divide) with an end-to-end CI/CD pipeline:
**GitHub -> Jenkins -> Maven build/test -> Docker image -> Docker Hub -> Docker Compose deploy (app + Nginx).**

Course: CSE 816 Software Production Engineering, mini-project. Student: Nihit Reddy Syamala (IMT2023584).

## Design note (instructor direction)

The instructor asked for a **simple Java app, not Spring Boot**. So:

- Plain Java 17+, Maven only for build and JUnit 5. **No framework, no runtime dependencies.**
- Spring-specific items (Spring MVC, MockMvc, `/actuator/health`) are **not used and not claimed**.
- Because a console program exits immediately, Docker Compose would have nothing to health-check or proxy. So the app has two modes:
  1. **CLI mode** - `java -jar calculator.jar add 2 3` prints the result and exits.
  2. **Server mode** - `java -jar calculator.jar serve` starts a tiny HTTP server using the JDK's built-in `com.sun.net.httpserver` (not a framework). It serves `/health` and a small `/api/v1/<op>?a=&b=` interface. This is a custom endpoint, **not Spring Actuator**.
- If your instructor confirms that only the CLI is allowed, the CLI is complete on its own; server mode is the addition that makes Compose health checks and Nginx meaningful.

Numeric type: `double` (64-bit floating point). Limitation: decimals are binary approximations (`0.1 + 0.2` = `0.30000000000000004`); results too large to represent are reported as errors. `NaN`/`Infinity` input is rejected.

## Prerequisites

JDK 17+ and Maven 3.9+ (to build/test locally), Docker + Docker Compose v2 (to run containers).

## Build, test, run

```bash
mvn clean package            # builds target/calculator.jar and runs tests
mvn test                     # tests only; XML reports in target/surefire-reports/

# CLI mode
java -jar target/calculator.jar add 2 3          # 5
java -jar target/calculator.jar divide 10 4      # 2.5
java -jar target/calculator.jar divide 1 0       # Error: Division by zero is not allowed (exit code 1)
java -jar target/calculator.jar add x 1          # Error: Invalid number for a: 'x'
java -jar target/calculator.jar                  # usage (exit code 2)

# Server mode
java -jar target/calculator.jar serve 8080       # or: PORT=8080 java -jar ...
```

### HTTP examples (server mode)

```bash
curl http://localhost:8081/health                          # {"status":"UP"}
curl "http://localhost:8081/api/v1/add?a=2&b=3"            # {"operation":"add","a":2,"b":3,"result":5}
curl "http://localhost:8081/api/v1/subtract?a=10&b=4"      # ... "result":6
curl "http://localhost:8081/api/v1/multiply?a=6&b=7"       # ... "result":42
curl "http://localhost:8081/api/v1/divide?a=10&b=4"        # ... "result":2.5
curl -i "http://localhost:8081/api/v1/divide?a=1&b=0"      # HTTP 400 {"error":"Division by zero is not allowed"}
curl -i "http://localhost:8081/api/v1/add?a=abc&b=1"       # HTTP 400 {"error":"Invalid number for a: 'abc'"}
```

Same calls through the Nginx proxy: replace port `8081` with `8082`.

## Docker

```bash
docker build -t nihit3003/calculator-devops:latest .
docker run -d -p 8081:8080 --name calc nihit3003/calculator-devops:latest   # server mode
curl http://localhost:8081/health
docker run --rm nihit3003/calculator-devops:latest add 2 3                  # CLI mode (overrides CMD)
```

The image is multi-stage (Maven+JDK build stage, `eclipse-temurin:21-jre-alpine` runtime), copies only the JAR, runs as a non-root user, exposes 8080 and has a `HEALTHCHECK`.

## Docker Compose

```bash
docker compose up -d        # app + nginx proxy
docker compose ps           # both should show "healthy"
docker ps
docker compose down
```

| What | URL |
|---|---|
| Direct app | http://localhost:8081 |
| Via Nginx proxy | http://localhost:8082 |

Ports 8081/8082 are used so they don't clash with Jenkins (8080). Override with `APP_PORT` / `PROXY_PORT`.
Image reference everywhere: `nihit3003/calculator-devops:<tag>` (Jenkins uses the build number as `IMAGE_TAG`; manual runs default to `latest`).

**Health checks:** the app container runs `wget -qO- http://localhost:8080/health`. Nginx waits for the app to be healthy (`depends_on: service_healthy`) but its own check hits `/nginx-health`, which is answered by Nginx itself, so there is no circular dependency. Limitation: `/health` reports only that the JVM server is responding.

## Git workflow

```bash
git checkout -b feature/<name>
# ...edit, test...
git add -A && git commit -m "feat: clear message"
git push -u origin feature/<name>
# open a Pull Request on GitHub (base: main), review, "Merge pull request"
git checkout main && git pull
```

## Jenkins setup (local Jenkins)

**Agent tools needed** (not assumed present): JDK 17+, Maven, Docker CLI + Compose v2. If Jenkins itself runs in a container, mount the Docker socket and install the tools in that image. The `jenkins` user must be allowed to use Docker (`sudo usermod -aG docker jenkins`, then restart Jenkins).

**Plugins:** Pipeline, Git, GitHub, Credentials Binding, JUnit, Mail (the `mail` step; Email Extension is optional).

**Credentials** (Manage Jenkins > Credentials > Global): kind *Username with password*, ID `dockerhub-credentials`, username `nihit3003`, password = a Docker Hub **access token** (Docker Hub > Account Settings > Personal access tokens, Read & Write). Never paste it into code or chat.

**Job:** New Item > *Pipeline* > *Pipeline script from SCM* > Git > your repo URL > branch `*/main` > Script Path `Jenkinsfile`.

**Trigger** (choose one):
- *Webhook:* enable "GitHub hook trigger for GITScm polling". GitHub repo > Settings > Webhooks > `http://<public-jenkins>/github-webhook/`, content type `application/json`. A local Jenkins is not reachable from GitHub, so use a tunnel such as ngrok, or:
- *Poll SCM (works locally):* the Jenkinsfile already polls every ~2 minutes (`H/2 * * * *`).

**Email:** Manage Jenkins > System > *E-mail Notification* (or Extended E-mail): SMTP server `smtp.gmail.com`, port 465 (SSL) or 587 (TLS), user = your Gmail, password = a Gmail **App Password** (needs 2-step verification). Set "System Admin e-mail address" to your Gmail. Use *Test configuration by sending test e-mail*.

**Docker Hub:** create the public repository `calculator-devops` under `nihit3003` (or let the first push create it, then set it public).

## Placeholders to review

| Item | Where | Current value |
|---|---|---|
| Docker Hub user | `Jenkinsfile`, `docker-compose.yml`, README | `nihit3003` |
| Credential ID | `Jenkinsfile` | `dockerhub-credentials` |
| Notification email | `Jenkinsfile` | `nihit300305@gmail.com` |
| GitHub repo URL | git remote | `https://github.com/Nihit3003/calculator.git` |
| Host ports | compose / Jenkinsfile | 8081 / 8082 |

## Screenshot / evidence checklist

- [ ] Green Jenkins pipeline run (Stage View, all 6 stages)
- [ ] Test results page (JUnit) in Jenkins
- [ ] Email notification received (success; ideally a failure one too)
- [ ] `docker ps` showing `calculator-app` and `calculator-proxy` (healthy)
- [ ] Sample responses (curl output) direct and via proxy, including the 400 divide-by-zero
- [ ] Docker Hub repo page with the tags
- [ ] GitHub: commit history, merged pull request, webhook delivery (if used)

## How to explain this in the viva/demo

- **Architecture:** developer pushes to GitHub -> Jenkins (webhook/poll) -> Maven build + tests -> Docker image -> Docker Hub -> `docker compose up -d` runs app + Nginx on one user-defined network.
- **Stages:** Checkout (get code) / Build (`mvn package`) / Test (`mvn test`, JUnit report; failure stops the pipeline so nothing is pushed or deployed) / Docker Build (tag = build number + latest) / Push (Docker Hub, credentials from Jenkins store) / Deploy (compose up, wait for healthy, request via proxy).
- **Credentials:** only referenced by ID (`withCredentials`), password sent through stdin, masked in logs; nothing secret is in Git.
- **Health checks:** `/health` on the JDK server -> Docker `healthcheck` -> Compose `depends_on: service_healthy`; Jenkins also polls the container health and calls the API through the proxy.
- **Idempotent redeploys:** fixed project/container names; `up -d` only recreates containers whose image changed; the unique build-number tag prevents stale images.
- **Common failures:** `docker: permission denied` (jenkins not in docker group); `mvn: not found` (tool missing on agent); port already in use (change `APP_PORT`/`PROXY_PORT`); webhook not firing for local Jenkins (use ngrok or Poll SCM); push `denied` (wrong token scope/username); no email (SMTP/App Password not configured).



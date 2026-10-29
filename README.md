# Calculator

A small calculator written in plain Java. It supports addition, subtraction, multiplication and division, and can be used from the command line or over a lightweight HTTP interface. The project ships with a complete CI/CD pipeline: every push to `main` is built, tested, packaged as a Docker image, published to Docker Hub and deployed behind an Nginx reverse proxy.

## Features

- Four operations: `add`, `subtract`, `multiply`, `divide`
- Command-line mode for quick calculations
- Server mode with a JSON HTTP interface and a `/health` endpoint, built on the JDK's own `com.sun.net.httpserver`
- Clear error handling: missing or invalid numbers, unknown operations, division by zero and overflow
- No frameworks and no runtime dependencies; JUnit 5 is used for tests only
- Multi-stage Docker image that runs as a non-root user and includes a container health check
- Docker Compose stack (app + Nginx) with health checks and a restart policy
- Jenkins pipeline: build, test, Docker build, push, deploy and email notifications

## Tech stack

| Area | Tools |
|---|---|
| Language / build | Java 17+, Maven |
| Tests | JUnit 5 |
| Containers | Docker, Docker Compose, Nginx |
| CI/CD | Jenkins (declarative pipeline), Docker Hub |

## Project structure

```
.
├── src/main/java/com/nihit/calculator/
│   ├── Calculator.java            # arithmetic
│   ├── CalculatorService.java     # input validation and operation dispatch
│   ├── CalculationException.java  # user-facing errors
│   ├── CalculatorHttpServer.java  # HTTP mode (/health and /api/v1/<operation>)
│   └── Main.java                  # entry point (CLI and server mode)
├── src/test/java/...              # unit and HTTP tests
├── nginx/nginx.conf               # reverse proxy configuration
├── Dockerfile                     # multi-stage image build
├── docker-compose.yml             # app + Nginx stack
├── Jenkinsfile                    # CI/CD pipeline
└── pom.xml
```

## Getting started

### Requirements

- JDK 17 or newer
- Maven 3.6 or newer
- Docker and Docker Compose v2 (only for the container workflows)

### Build and test

```bash
mvn clean package      # compiles, runs the tests and creates target/calculator.jar
mvn test               # tests only; reports are written to target/surefire-reports/
```

### Command-line usage

```bash
java -jar target/calculator.jar add 2 3          # 5
java -jar target/calculator.jar subtract 10 4    # 6
java -jar target/calculator.jar multiply 6 7     # 42
java -jar target/calculator.jar divide 10 4      # 2.5
```

Errors are written to stderr with a non-zero exit code:

```bash
java -jar target/calculator.jar divide 1 0       # Error: Division by zero is not allowed   (exit 1)
java -jar target/calculator.jar add x 1          # Error: Invalid number for a: 'x'         (exit 1)
java -jar target/calculator.jar                  # usage message                            (exit 2)
```

### Server mode

```bash
java -jar target/calculator.jar serve 8080       # or set the PORT environment variable
```

| Endpoint | Description |
|---|---|
| `GET /health` | Returns `{"status":"UP"}` |
| `GET /api/v1/{add\|subtract\|multiply\|divide}?a=<number>&b=<number>` | Returns the result as JSON |

```bash
curl http://localhost:8080/health
# {"status":"UP"}

curl "http://localhost:8080/api/v1/add?a=2&b=3"
# {"operation":"add","a":2,"b":3,"result":5}

curl -i "http://localhost:8080/api/v1/divide?a=1&b=0"
# HTTP/1.1 400 Bad Request
# {"error":"Division by zero is not allowed"}
```

Invalid input returns HTTP 400, unknown paths return 404, and non-GET requests return 405.

### Notes on numbers

Values are 64-bit floating point (`double`), so some decimal results are binary approximations (for example `0.1 + 0.2` gives `0.30000000000000004`). `NaN` and `Infinity` inputs are rejected, and results too large to represent are reported as errors.

## Docker

```bash
docker build -t nihit3003/calculator-devops:latest .

# server mode (default)
docker run -d --name calculator -p 8081:8080 nihit3003/calculator-devops:latest
curl http://localhost:8081/health

# CLI mode (arguments replace the default command)
docker run --rm nihit3003/calculator-devops:latest add 2 3
```

The image is built in two stages (Maven + JDK, then a slim JRE runtime), contains only the packaged JAR, runs as a non-root user and defines a `HEALTHCHECK` against `/health`.

## Docker Compose

The Compose file runs the calculator behind an Nginx reverse proxy on a private network.

```bash
docker compose up -d       # start the stack
docker compose ps          # both services should report "healthy"
docker compose down        # stop and remove it
```


Nginx starts only after the app reports healthy. Configuration is read from environment variables, with these defaults:

| Variable | Default | Purpose |
|---|---|---|
| `IMAGE_NAME` | `nihit3003/calculator-devops` | Image repository |
| `IMAGE_TAG` | `latest` | Image tag (the pipeline sets the build number) |
| `APP_PORT` | `8081` | Host port for the app |
| `PROXY_PORT` | `8082` | Host port for Nginx |

Ports 8081 and 8082 are used so they do not clash with a local Jenkins on 8080.

## CI/CD pipeline

The `Jenkinsfile` defines a declarative pipeline that runs on every push to `main`.

| Stage | What it does |
|---|---|
| Checkout | Fetches the latest commit from GitHub |
| Build | `mvn clean package -DskipTests` |
| Test | `mvn test`; JUnit reports are published, and a failure stops the pipeline |
| Docker Build | Builds the image, tagged with the build number and `latest` |
| Push | Logs in to Docker Hub and pushes both tags |
| Deploy | `docker compose up -d`, waits for both containers to be healthy, then calls the API through the proxy |
| Post | Sends a success or failure email and prunes dangling images |

The job accepts two parameters, `PUSH_IMAGE` and `DEPLOY`, both enabled by default. Deployments are repeatable: the Compose project and container names are fixed, and the image tag changes with every build, so re-running updates the same stack.

### Jenkins setup

**On the Jenkins machine:** JDK 17+, Maven, Docker and Docker Compose v2. The `jenkins` user must be able to use Docker (`sudo usermod -aG docker jenkins`, then restart Jenkins).

**Plugins:** Pipeline, Git, GitHub, Credentials Binding, JUnit and Mailer.

**Credentials:** add a *Username with password* credential with the ID `dockerhub-credentials`. Use your Docker Hub username and a Docker Hub access token with read/write permission as the password.

**Job:** create a *Pipeline* job using *Pipeline script from SCM*, point it at this repository, set the branch to `*/main` and the script path to `Jenkinsfile`.

**Triggers:** the pipeline polls the repository about every two minutes. If Jenkins is reachable from the internet, you can instead add a GitHub webhook to `http://<your-jenkins>/github-webhook/` for immediate builds.

**Email notifications:** configure SMTP under *Manage Jenkins > System* (for Gmail: `smtp.gmail.com`, port 465 with SSL, and an app password). Set the recipient with `NOTIFY_EMAIL` in the `Jenkinsfile`.

### Pipeline configuration

These values are set in the `environment` block of the `Jenkinsfile`:

| Setting | Value |
|---|---|
| `DOCKERHUB_USER` | `nihit3003` |
| `IMAGE_NAME` | `nihit3003/calculator-devops` |
| `DOCKERHUB_CREDS_ID` | `dockerhub-credentials` |
| `NOTIFY_EMAIL` | notification recipient |
| `APP_PORT` / `PROXY_PORT` | `8081` / `8082` |

## Contributing

1. Create a branch: `git checkout -b feature/<name>`
2. Make your changes and run `mvn test`
3. Commit with a clear message and push the branch
4. Open a pull request into `main`

## Troubleshooting

| Problem | Likely cause and fix |
|---|---|
| `Source option 5 is no longer supported` | An old Maven compiler plugin is being used. The `pom.xml` pins a current one; make sure you have the latest `main`. |
| `release version 17 not supported` | `javac` is older than 17 or only a JRE is installed. Install a JDK 17+ and check `javac -version`. |
| `permission denied` on the Docker socket in Jenkins | Add `jenkins` to the `docker` group and restart Jenkins. |
| Compose reports a port already in use | Change `APP_PORT` or `PROXY_PORT`, or stop the process using the port. |
| Pipeline push to Docker Hub is denied | Check the credential ID is `dockerhub-credentials` and the token has write access. |
| No email notifications | Verify the SMTP settings and use the test button in Jenkins. |

// Declarative pipeline: GitHub push -> build -> test -> docker build -> push -> deploy.
// Requires on the Jenkins agent: JDK 17+, Maven, Docker + Docker Compose v2, curl (optional).
// See README.md "Jenkins setup" for plugins, credentials and trigger configuration.

pipeline {
    agent any   // ENV-SPECIFIC: use a label (e.g. agent { label 'docker' }) if you have several agents

    parameters {
        booleanParam(name: 'PUSH_IMAGE', defaultValue: true,  description: 'Push the image to Docker Hub')
        booleanParam(name: 'DEPLOY',     defaultValue: true,  description: 'Deploy with docker compose after push')
    }

    triggers {
        // Preferred: GitHub webhook (needs the GitHub plugin + "GitHub hook trigger for GITScm polling" in the job).
        githubPush()
        // Fallback if Jenkins is not reachable from the internet (local Jenkins!): poll every ~2 min.
        pollSCM('H/2 * * * *')
    }

    options {
        timestamps()
        timeout(time: 20, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()   // avoids two deploys racing each other
    }

    environment {
        DOCKERHUB_USER = 'nihit3003'                      // ENV-SPECIFIC: Docker Hub username
        IMAGE_NAME     = "${DOCKERHUB_USER}/calculator-devops"
        IMAGE_TAG      = "${env.BUILD_NUMBER}"            // unique tag per build
        DOCKERHUB_CREDS_ID = 'dockerhub-credentials'      // Jenkins "Username with password" credential ID
        NOTIFY_EMAIL   = 'nihit300305@gmail.com'          // ENV-SPECIFIC: recipient of build emails
        APP_PORT       = '8081'
        PROXY_PORT     = '8082'
        COMPOSE_PROJECT_NAME = 'calculator'               // fixed project name -> redeploys update the same stack
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git log -1 --oneline'
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B clean package -DskipTests'   // tests run in the next stage
            }
        }

        stage('Test') {
            steps {
                sh 'mvn -B test'                         // non-zero exit on failure -> pipeline stops here
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                    docker build -t ${IMAGE_NAME}:${IMAGE_TAG} -t ${IMAGE_NAME}:latest .
                '''
            }
        }

        stage('Push') {
            when { expression { params.PUSH_IMAGE } }
            steps {
                withCredentials([usernamePassword(credentialsId: env.DOCKERHUB_CREDS_ID,
                                                  usernameVariable: 'DH_USER',
                                                  passwordVariable: 'DH_PASS')]) {
                    // Password is piped via stdin and masked by Jenkins; never echoed or hard-coded.
                    sh '''
                        echo "$DH_PASS" | docker login -u "$DH_USER" --password-stdin
                        docker push ${IMAGE_NAME}:${IMAGE_TAG}
                        docker push ${IMAGE_NAME}:latest
                        docker logout
                    '''
                }
            }
        }

        stage('Deploy') {
            when { expression { params.DEPLOY } }
            steps {
                // Idempotent: same project name + container names; "up -d" recreates only what changed.
                sh '''
                    docker compose up -d --remove-orphans

                    echo "Waiting for containers to become healthy..."
                    for i in $(seq 1 30); do
                        APP=$(docker inspect --format '{{.State.Health.Status}}' calculator-app 2>/dev/null || echo starting)
                        PRX=$(docker inspect --format '{{.State.Health.Status}}' calculator-proxy 2>/dev/null || echo starting)
                        echo "  app=$APP proxy=$PRX"
                        [ "$APP" = "healthy" ] && [ "$PRX" = "healthy" ] && break
                        sleep 3
                    done
                    [ "$APP" = "healthy" ] && [ "$PRX" = "healthy" ] || { docker compose logs --tail=50; exit 1; }

                    # End-to-end check THROUGH the proxy (runs inside the proxy container; no curl needed on agent)
                    docker exec calculator-proxy wget -qO- http://127.0.0.1/health
                    docker exec calculator-proxy wget -qO- "http://127.0.0.1/api/v1/add?a=2&b=3"
                    docker compose ps
                '''
            }
        }
    }

    post {
        success {
            mail to: env.NOTIFY_EMAIL,   // needs SMTP configured in Manage Jenkins > System
                 subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                 body: "Build succeeded.\nImage: ${env.IMAGE_NAME}:${env.IMAGE_TAG}\nDetails: ${env.BUILD_URL}"
        }
        failure {
            mail to: env.NOTIFY_EMAIL,
                 subject: "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                 body: "Build failed.\nCheck the console output: ${env.BUILD_URL}console"
        }
        always {
            // Cleanup: remove dangling layers only; the running deployment is untouched.
            sh 'docker image prune -f || true'
        }
    }
}

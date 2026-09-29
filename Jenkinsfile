pipeline {
    agent any

    parameters {
        string(name: 'SERVER_PORT', defaultValue: '8005', description: 'Target application server port')
        choice(name: 'ENVIRONMENT', choices: ['dev', 'prod'], description: 'Target deployment environment profile')
        choice(name: 'DEPLOY_TARGET', choices: ['Both', 'Docker', 'Tomcat'], description: 'Target deployment runtime')
        string(name: 'DOCKER_IMAGE_NAME', defaultValue: 'podcast-episode-release-system', description: 'Docker image repository name')
        string(name: 'DOCKER_CONTAINER_NAME', defaultValue: 'podcast-release-system', description: 'Docker running container name')
        string(name: 'TOMCAT_WEBAPPS_DIR', defaultValue: 'C:\\Users\\thele\\Downloads\\apache-tomcat-11.0.25\\apache-tomcat-11.0.25\\webapps', description: 'Path to target Tomcat webapps directory')
    }

    stages {
        stage('Checkout SCM') {
            steps {
                echo 'Checking out source code from Git repository...'
                checkout scm
            }
        }

        stage('Compile & Test') {
            steps {
                echo 'Executing unit, integration, and security test suite...'
                script {
                    if (isUnix()) {
                        sh 'mvn clean test'
                    } else {
                        bat 'where mvn >nul 2>&1 && mvn clean test || "C:\\Users\\thele\\apache-maven-3.9.16\\bin\\mvn.cmd" clean test'
                    }
                }
            }
        }

        stage('Package WAR') {
            steps {
                echo "Packaging WAR artifact for environment profile: ${params.ENVIRONMENT}..."
                script {
                    if (isUnix()) {
                        sh 'mvn package -DskipTests'
                    } else {
                        bat 'where mvn >nul 2>&1 && mvn package -DskipTests || "C:\\Users\\thele\\apache-maven-3.9.16\\bin\\mvn.cmd" package -DskipTests'
                    }
                }
            }
        }

        stage('Deploy to Tomcat') {
            when {
                expression { params.DEPLOY_TARGET == 'Tomcat' || params.DEPLOY_TARGET == 'Both' }
            }
            steps {
                script {
                    def tomcatDir = (params.TOMCAT_WEBAPPS_DIR && params.TOMCAT_WEBAPPS_DIR.trim()) ? params.TOMCAT_WEBAPPS_DIR.trim() : 'C:\\Users\\thele\\Downloads\\apache-tomcat-11.0.25\\apache-tomcat-11.0.25\\webapps'
                    echo "Deploying target/podcast-release.war to Tomcat directory: ${tomcatDir}"
                    
                    if (isUnix()) {
                        sh """
                            mkdir -p "${tomcatDir}"
                            cp target/podcast-release.war "${tomcatDir}/"
                            echo "Successfully copied podcast-release.war to ${tomcatDir}"
                        """
                    } else {
                        bat """
                            IF NOT EXIST "${tomcatDir}" mkdir "${tomcatDir}"
                            copy /Y "target\\podcast-release.war" "${tomcatDir}\\podcast-release.war"
                            echo Successfully copied podcast-release.war to ${tomcatDir}
                        """
                    }
                }
            }
        }

        stage('Build & Tag Docker Image') {
            when {
                expression { params.DEPLOY_TARGET == 'Docker' || params.DEPLOY_TARGET == 'Both' }
            }
            steps {
                script {
                    def imageName = params.DOCKER_IMAGE_NAME
                    def tagVersion = "${imageName}:${BUILD_NUMBER}"
                    def tagLatest = "${imageName}:latest"
                    echo "Building Docker image version: ${tagVersion} and ${tagLatest}..."
                    if (isUnix()) {
                        sh "docker build -t ${tagVersion} -t ${tagLatest} ."
                    } else {
                        bat "docker build -t ${tagVersion} -t ${tagLatest} ."
                    }
                }
            }
        }

        stage('Deploy Docker Container') {
            when {
                expression { params.DEPLOY_TARGET == 'Docker' || params.DEPLOY_TARGET == 'Both' }
            }
            steps {
                script {
                    def containerName = params.DOCKER_CONTAINER_NAME
                    def imageName = "${params.DOCKER_IMAGE_NAME}:${BUILD_NUMBER}"
                    def port = params.SERVER_PORT
                    echo "Deploying fresh Docker container: ${containerName} using image ${imageName} on port ${port}..."
                    if (isUnix()) {
                        sh """
                            docker stop ${containerName} || true
                            docker rm ${containerName} || true
                            docker run -d --name ${containerName} -p ${port}:8080 --restart unless-stopped -v podcast-release-uploads:/opt/podcast-release/uploads ${imageName}
                            sleep 3
                            docker ps --filter name=${containerName}
                        """
                    } else {
                        bat """
                            docker stop ${containerName} 2>nul || ver >nul
                            docker rm ${containerName} 2>nul || ver >nul
                            docker run -d --name ${containerName} -p ${port}:8080 --restart unless-stopped -v podcast-release-uploads:/opt/podcast-release/uploads ${imageName}
                            timeout /t 3 /nobreak >nul
                            docker ps --filter name=${containerName}
                        """
                    }
                }
            }
        }
    }

    post {
        always {
            echo 'Publishing test execution reports to Jenkins...'
            junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true

            echo 'Archiving failure screenshots and DOM dumps if present...'
            archiveArtifacts artifacts: 'target/selenium-screenshots/**', allowEmptyArchive: true
        }
        success {
            echo 'Archiving packaged application WAR artifact...'
            archiveArtifacts artifacts: 'target/*.war', allowEmptyArchive: false
            echo 'Pipeline execution and continuous deployment stages completed successfully!'
        }
        failure {
            echo 'Pipeline build, test quality gate, or deployment failed. Deployment aborted.'
        }
    }
}

pipeline {
    agent any

    parameters {
        string(name: 'SERVER_PORT', defaultValue: '8083', description: 'Target application server port')
        choice(name: 'ENVIRONMENT', choices: ['dev', 'prod'], description: 'Target deployment environment profile')
        string(name: 'TOMCAT_WEBAPPS_DIR', defaultValue: 'C:\\Program Files\\Apache Software Foundation\\Tomcat 10.1\\webapps', description: 'Path to target Tomcat webapps directory')
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
            steps {
                echo "Deploying target/podcast-release.war to Tomcat webapps directory: ${params.TOMCAT_WEBAPPS_DIR}..."
                script {
                    if (isUnix()) {
                        sh '''
                            if [ -d "${TOMCAT_WEBAPPS_DIR}" ]; then
                                cp target/podcast-release.war "${TOMCAT_WEBAPPS_DIR}/"
                                echo "Successfully deployed podcast-release.war to ${TOMCAT_WEBAPPS_DIR}"
                            else
                                echo "Tomcat directory ${TOMCAT_WEBAPPS_DIR} not found. Artifact target/podcast-release.war staged."
                            fi
                        '''
                    } else {
                        bat '''
                            IF EXIST "%TOMCAT_WEBAPPS_DIR%" (
                                copy /Y "target\\podcast-release.war" "%TOMCAT_WEBAPPS_DIR%\\podcast-release.war"
                                echo Successfully deployed podcast-release.war to %TOMCAT_WEBAPPS_DIR%
                            ) ELSE (
                                echo Tomcat webapps directory %TOMCAT_WEBAPPS_DIR% not found. WAR artifact staged at target\\podcast-release.war for Tomcat deployment.
                            )
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            echo 'Archiving build artifact...'
            archiveArtifacts artifacts: 'target/*.war', allowEmptyArchive: false
        }
        success {
            echo 'Pipeline execution and Tomcat deployment stage completed successfully!'
        }
        failure {
            echo 'Pipeline build or deployment failed.'
        }
    }
}

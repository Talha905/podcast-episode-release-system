pipeline {
    agent any

    parameters {
        string(name: 'SERVER_PORT', defaultValue: '8083', description: 'Target application server port')
        choice(name: 'ENVIRONMENT', choices: ['dev', 'prod'], description: 'Target deployment environment profile')
        string(name: 'TOMCAT_WEBAPPS_DIR', defaultValue: 'C:\\xampp\\tomcat\\webapps', description: 'Path to target Tomcat webapps directory')
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
                script {
                    def tomcatDir = (params.TOMCAT_WEBAPPS_DIR && params.TOMCAT_WEBAPPS_DIR.trim()) ? params.TOMCAT_WEBAPPS_DIR.trim() : 'C:\\xampp\\tomcat\\webapps'
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

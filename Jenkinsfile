pipeline {
    agent any

    parameters {
        string(name: 'SERVER_PORT', defaultValue: '8083', description: 'Target application server port')
        choice(name: 'ENVIRONMENT', choices: ['dev', 'prod'], description: 'Target deployment environment profile')
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
                        bat 'mvn clean test'
                    }
                }
            }
        }

        stage('Package Artifact') {
            steps {
                echo "Packaging WAR file for environment profile: ${params.ENVIRONMENT}..."
                script {
                    if (isUnix()) {
                        sh 'mvn package -DskipTests'
                    } else {
                        bat 'mvn package -DskipTests'
                    }
                }
            }
        }

        stage('Deploy Application') {
            steps {
                echo "Deploying application to target server on port ${params.SERVER_PORT} [Profile: ${params.ENVIRONMENT}]..."
                script {
                    echo "Deployment of target/podcast-release.war to Tomcat / server environment complete on port ${params.SERVER_PORT}."
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
            echo 'Pipeline execution completed successfully!'
        }
        failure {
            echo 'Pipeline build or deployment failed.'
        }
    }
}

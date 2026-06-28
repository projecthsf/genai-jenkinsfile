// Optional: Import shared libraries at the very top
@Library('my-shared-library@master') _

pipeline {
    // 1. Agent Directive (Defines where the pipeline execution occurs)
    agent {
        node {
            label 'node-linux-heavy'
            customWorkspace '/var/lib/jenkins/custom-workspace'
        }
    }

    // 2. Options Directive (Pipeline-specific configurations)
    options {
        timeout(time: 1, unit: 'HOURS') // Fails build if it hangs too long
        retry(2)                        // Retries the entire pipeline on failure
        timestamps()                    // Adds execution times to console logs
        disableConcurrentBuilds()       // Prevents simultaneous executions of this job
        buildDiscarder(logRotator(numToKeepStr: '10')) // Retains logs for only last 10 builds
    }

    // 3. Parameters Directive (Runtime variables prompted to users)
    parameters {
        string(name: 'DEPLOY_ENV', defaultValue: 'staging', description: 'Target Environment')
        booleanParam(name: 'RUN_TESTS', defaultValue: true, description: 'Check to execute tests')
        choice(name: 'LOG_LEVEL', choices: ['INFO', 'DEBUG', 'WARN'], description: 'App verbosity')
        password(name: 'API_TOKEN', defaultValue: '', description: 'Secure token for API calls')
    }

    // 4. Environment Directive (Global environment variables)
    environment {
        GLOBAL_SERVICE_NAME = 'payment-gateway'
        // Retrieving credentials securely using credentials helpers
        DB_USER = credentials('database-username-id')
    }

    // 5. Triggers Directive (Automated pipeline execution prompts)
    triggers {
        cron('H H(0-2) * * 1-5') // Cron expression for weekday nightly builds
        pollSCM('H/15 * * * *')  // Polls source control for changes every 15 minutes
    }

    // 6. Tools Directive (Pre-configured tools from global settings)
    tools {
        maven 'Maven_3.9'
        jdk 'Java_17'
    }

    // 7. Stages Block (The core execution sequence)
    stages {

        stage('Initialize & Setup') {
            // Stage-specific environment variable override
            environment {
                STAGE_VAR = 'local-init'
            }
            steps {
                echo "Starting pipeline for ${env.GLOBAL_SERVICE_NAME} on ${params.DEPLOY_ENV}"
                echo "Current Build Number: ${env.BUILD_NUMBER}"
            }
        }

        stage('Parallel Test Executions') {
            // Evaluates whether this stage should run
            when {
                environment name: 'RUN_TESTS', value: 'true'
            }

            // 8. Parallel Directive (Runs multiple independent steps concurrently)
            parallel {
                stage('Unit Tests') {
                    steps {
                        echo "Running Backend Unit Tests..."
                        sh 'mvn test'
                    }
                }
                stage('Static Code Analysis') {
                    steps {
                        echo "Analyzing code quality..."
                        // Using explicit credentials block within steps
                        withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_KEY')]) {
                            echo "Scanning code with token: ${SONAR_KEY}"
                        }
                    }
                }
            }
        }

        stage('Conditional Build') {
            // Complex evaluations combining expressions
            when {
                allOf {
                    branch 'main'
                    expression { return params.LOG_LEVEL == 'INFO' }
                }
            }
            steps {
                echo "Building production artifact..."
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Scripted Fallback Stage') {
            steps {
                // 9. Script Block (Allows raw Apache Groovy features within Declarative syntax)
                script {
                    def modules = ['auth', 'core', 'ui']
                    for (int i = 0; i < modules.size(); i++) {
                        echo "Processing native Groovy operation for module: ${modules[i]}"
                    }

                    // Simple programmatic try-catch condition
                    try {
                        sh 'false' // Deliberately failing command
                    } catch (Exception err) {
                        echo "Caught exception safely inside scripted block: ${err.message}"
                    }
                }
            }
        }
    }

    // 10. Post Section (Executes blocks depending on final pipeline states)
    post {
        always {
            echo "This block executes regardless of the pipeline outcome."
            cleanWs() // Cleans the workspace directory to prevent disk storage bloat
        }
        success {
            echo "Pipeline completed successfully!"
        }
        failure {
            echo "Pipeline failed. Sending alert notifications..."
        }
        unstable {
            echo "Pipeline completed with test or check violations (Unstable state)."
        }
        changed {
            echo "The status of the pipeline changed compared to the previous run."
        }
    }
}

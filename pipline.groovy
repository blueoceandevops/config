pipeline {
    agent { label 'jdk8' }
    stages {

        stage('Git Fetch') { // for display purposes
            // Get some code from a GitHub repository
            steps {
                git branch: 'master', url: 'git@bicsgit.bc:id961900/java-spring-boot.git'
            }
        }
        stage('Build With Unit Testing') {
            steps {
                // Run the maven build
                script {
                    // Get the Maven tool.
                    // ** NOTE: This 'M3' Maven tool must be configured
                    // **       in the global configuration.

                    def mvnHome = tool 'Maven 3.3.9'
                    if (isUnix()) {
                        sh "'${mvnHome}/bin/mvn' -Dmaven.test.failure.ignore clean package"
                        def pom = readMavenPom file: 'pom.xml'
                        print pom.version
                        junit '**/target/surefire-reports/TEST-*.xml'
                        archive 'target/*.jar'
                    } else {
                        bat(/"${mvnHome}\bin\mvn" -Dmaven.test.failure.ignore clean package/)
                        def pom = readMavenPom file: 'pom.xml'
                        print pom.version
                        junit '**/target/surefire-reports/TEST-*.xml'
                        archive 'target/*.jar'
                    }
                }

            }
        }
        stage('Integration Tests') {
            // Run the maven build
            steps {
                script {
                    def mvnHome = tool 'Maven 3.3.9'
                    if (isUnix()) {
                        sh "'${mvnHome}/bin/mvn'  verify -Dunit-tests.skip=true"
                    } else {
                        bat(/"${mvnHome}\bin\mvn" verify -Dunit-tests.skip=true/)
                    }

                }
            }
        }
        stage('Sonar Check') {
            // Run the maven build
            steps {
                script {
                    def mvnHome = tool 'Maven 3.3.9'
                    sh "'${mvnHome}/bin/mvn'  verify sonar:sonar -Dsonar.host.url=http://bicsjava.bc/sonar/ -Dmaven.test.failure.ignore=true"

                }
            }
        }
        stage('ITT Deploy Approval') {
            steps {
                timeout(time: 1, unit: 'MINUTES') {
                    //input message:'Approve deployment?', submitter: 'it-ops'
                    input message: 'Approve deployment?'
                }
            }
        }
        stage('ITT deployment') {
            when {
                // Only say hello if a "greeting" is requested
                expression { currentBuild.result == null || currentBuild.result == 'SUCCESS' }
            }
            steps {
                timeout(time: 2, unit: 'MINUTES') {
                    build job: 'SpringBootDeplotToITT'
                    echo 'the application is deployed !'
                }
            }

        }
    }
    post {
        success {
            mail to: 'mahmoud.romih@bics.com',
                    subject: "Successful Pipeline: ${currentBuild.fullDisplayName}",
                    body: "the pipline is successful for ${env.BUILD_URL}"
        }
        unstable {
            mail to: 'mahmoud.romih@bics.com',
                    subject: "Untsable Pipeline: ${currentBuild.fullDisplayName}",
                    body: "Something is unstable with ${env.BUILD_URL}"
        }
        failure {
            mail to: 'mahmoud.romih@bics.com',
                    subject: "Failed Pipeline: ${currentBuild.fullDisplayName}",
                    body: "Something is wrong with ${env.BUILD_URL}"
        }
        changed {
            echo 'Things were different before...'
        }
    }

}





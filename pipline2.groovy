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
        stage('Sonar Check') {
            // Run the maven build
            steps {
                script {
                    def mvnHome = tool 'Maven 3.3.9'
                    sh "'${mvnHome}/bin/mvn'  verify sonar:sonar -Dsonar.host.url=http://bicsjava.bc/sonar/ -Dmaven.test.failure.ignore=true"

                }
            }
        }
        stage('ITT deployment') {
            when {
                // Only say hello if a "greeting" is requested
                expression { currentBuild.result == null || currentBuild.result == 'SUCCESS' }
            }
            steps {
                script {
                    def base_url="http://bicsjava.bc/jenkins/job/spring-boot-pipeline/lastSuccessfulBuild/artifact/target/javalogaggregation%23%230.0.1-SNAPSHOT-LOCAL.jar"
                    def exec = """ 
                                    ssh -o StrictHostKeyChecking=no -l id961900  el4718.bc uname -a  
                                    cd /opt/java/bin \
                                    ./app stop && ./get config && ./app deploy $base_url && ./app start
                                 """

                    sshagent(['442826db-1ac3-46bd-a351-502f65ae8c38']) {
                        sh exec
                    }
                    echo 'the application is deployed !'
                }

            }
        }

    }
}
post {
    success {
        echo 'I succeeded!'
    }
    unstable {
        echo 'I am unstable :/'
    }
    failure {
        echo 'I failed :('
    }
    changed {
        echo 'Things were different before...'
    }
}



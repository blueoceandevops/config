node {
    def mvnHome
    stage('Git Fetch') { // for display purposes
        // Get some code from a GitHub repository
        git branch: 'master', credentialsId: '6a1c4ce3-78ae-43e4-9b33-d7d64a774dda', url: 'git@bicsgit.bc:id961900/java-spring-boot.git'
        // Get the Maven tool.
        // ** NOTE: This 'M3' Maven tool must be configured
        // **       in the global configuration.
        mvnHome = tool 'Maven 3.3.9'
    }
    stage('Build With Unit Testing') {
        // Run the maven build
        if (isUnix()) {
            sh "'${mvnHome}/bin/mvn' -Dmaven.test.failure.ignore clean package"
        } else {
            bat(/"${mvnHome}\bin\mvn" -Dmaven.test.failure.ignore clean package/)
            def pom = readMavenPom file:'pom.xml'
            print pom.version
            junit '**/target/surefire-reports/TEST-*.xml'
            archive 'target/*.jar'
        }
    }
    stage('Integration Tests') {
        // Run the maven build
        if (isUnix()) {
            sh "'${mvnHome}/bin/mvn' verify -Dunit-tests.skip=true"
        } else {
            bat(/"${mvnHome}\bin\mvn" verify -Dunit-tests.skip=true/)
        }
    }
    stage('ITT Deploy Approval') {
        timeout(time:5, unit:'MINUTES') {
            //input message:'Approve deployment?', submitter: 'it-ops'
            input message:'Approve deployment?'
        }
    }
    stage('ITT deployment') {
        if ( currentBuild.result == null || currentBuild.result == 'SUCCESS') {
            echo 'the application is deployed !'
        }
        else {
            echo 'the application is not deployed !'
        }

    }
    stage('Post cleanup and communication') {
        if ( currentBuild.result == null || currentBuild.result == 'SUCCESS') {
            //deleteDir() /* clean up our workspace */
            echo 'One way or another, I have finished'
        }
        else {
            echo 'I failed :('
        }

    }

}
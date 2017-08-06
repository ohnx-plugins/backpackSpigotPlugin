pipeline {
  agent any
  stages {
    stage('Build') {
      steps {
        sh 'mvn install'
      }
    }
    stage('Archive Artifacts') {
      steps {
        archiveArtifacts 'target/backpack-1.1.0.jar'
      }
    }
  }
}
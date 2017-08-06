pipeline {
  agent any
  options {
    buildDiscarder(logRotator(numToKeepStr: '3'))
  }
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

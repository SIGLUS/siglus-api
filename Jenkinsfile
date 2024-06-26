pipeline {
    agent { label 'docker'}
    options {
        buildDiscarder(logRotator(numToKeepStr: '50'))
        timestamps ()
    }
    environment {
        IMAGE_REPO = "siglusdevops/siglusapi"
        IMAGE_TAG = sh(script: '''tag=${BRANCH_NAME}-$(git rev-parse HEAD); echo ${tag}''', returnStdout: true).trim()
    }
    stages {
        stage('Build') {
            steps {
                sh '''
                    docker run --rm -v `pwd`:/app -w /app --network=host amazoncorretto:8u342-alpine3.16 sh -c \
                    "apk add npm freetype ttf-dejavu fontconfig && ./gradlew clean build"
                '''
            }
        }
        stage('Push Image') {
            steps {
                sh '''
                    IMAGE_NAME=${IMAGE_REPO}:${IMAGE_TAG}
                    docker build -t ${IMAGE_NAME} .
                    docker push ${IMAGE_NAME}
                    if [ "$GIT_BRANCH" = "release" ]; then
                      echo "push latest tag for release branch"
                      docker build -t ${IMAGE_REPO}:latest .
                      docker push ${IMAGE_REPO}:latest
                      docker rmi ${IMAGE_REPO}:latest
                    fi
                    docker rmi ${IMAGE_NAME}
                '''
            }
        }
        stage('Deploy To Dev') {
            when {
                branch 'master'
            }
            steps {
                deploy ("dev", env.IMAGE_TAG)
            }
        }
        stage('Deploy To QA') {
            when {
                branch 'showcase'
            }
            steps {
                deploy ("qa", env.IMAGE_TAG)
            }
        }
    }
}

def deploy(app_env, image_tag) {
    withEnv(["IMAGE_TAG=$image_tag","APP_ENV=$app_env"]) {
        sh '''
            echo "trigger downstream pipeline with: ENV=${APP_ENV},IMAGE_TAG=${IMAGE_TAG}"
        '''
    }
    build job: 'build_siglus_api',
          wait: true,
          parameters: [
              [$class: 'StringParameterValue', name: 'ENV', value: "$app_env"],
              [$class: 'StringParameterValue', name: 'IMAGE_TAG', value: "$image_tag"]]
}

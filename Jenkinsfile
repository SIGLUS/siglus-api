pipeline {
    agent { label 'docker'}
    options {
        buildDiscarder(logRotator(numToKeepStr: '50'))
        timestamps ()
    }
    environment {
        IMAGE_REPO = "siglusdevops/siglusapi"
        SERVICE_NAME = "siglusapi"
    }
    stages {
        stage('Build') {
            steps {
                sh '''
                    docker run --rm -v `pwd`:/app -w /app --network=host adoptopenjdk/openjdk8:alpine sh -c \
                    "apk add npm && ./gradlew clean build"
                '''
            }
        }
        stage('Push Image') {
            steps {
                sh '''
                    IMAGE_TAG=${BRANCH_NAME}-$(git rev-parse HEAD)
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
        stage('Deploy To dev') {
            when {
                branch 'master'
            }
            steps {
                deploy "dev"
            }
        }
        stage('Deploy To QA') {
            when {
                branch 'showcase'
            }
            steps {
                deploy "qa"
            }
        }
        stage('Deploy To UAT') {
            when {
                branch 'release'
            }
            steps {
                deploy "uat"
            }
        }
    }
}


def deploy(app_env, image_tag) {
    build job: 'deploy_siglus_api_manually',
          wait: true,
          parameters: [string(name: 'IMAGE_TAG', value: String.valueOf(image_tag)),string(name: 'ENV', value: String.valueOf(app_env))]
}

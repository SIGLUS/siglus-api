pipeline {
    agent any
    options {
        buildDiscarder(logRotator(numToKeepStr: '50'))
    }
    stages {
        stage('Build') {
            steps {
                fetch_setting_env()
                ansiColor('xterm') {
                    println "gradle: build()"
                    sh '''
                      pwd && ls -l
                      sudo npm install
                      ./gradlew clean build
                    '''
                }
                checkstyle pattern: '**/build/reports/checkstyle/*.xml'
                pmd pattern: '**/build/reports/pmd/*.xml'
                junit '**/build/test-results/*/*.xml'
            }
        }
        stage('SonarQube Analysis') {
            when {
                branch 'dev'
            }
            steps {
                withCredentials([string(credentialsId: 'sonarqube_token', variable: 'SONARQUBE_TOKEN')]) {
                    sh './gradlew sonarqube -Dsonar.projectKey=siglus-api -Dsonar.host.url=http://13.234.176.65:9000 -Dsonar.login=$SONARQUBE_TOKEN'
                }
            }
        }
        stage('Push Image') {
            steps {
                withCredentials([usernamePassword(credentialsId: "cad2f741-7b1e-4ddd-b5ca-2959d40f62c2", usernameVariable: "USER", passwordVariable: "PASS")]) {
                    sh '''
                        set +x
                        docker login -u $USER -p $PASS
                        IMAGE_TAG=${BRANCH_NAME}-$(git rev-parse HEAD)
                        IMAGE_REPO=siglusdevops/siglusapi
                        IMAGE_NAME=${IMAGE_REPO}:${IMAGE_TAG}
                        docker build -t ${IMAGE_NAME} .
                        docker push ${IMAGE_NAME}
                        if [ "$GIT_BRANCH" = "release-1.2" ]; then
                          echo "push latest tag for release branch"
                          docker build -t ${IMAGE_REPO}:latest .
                          docker push ${IMAGE_REPO}:latest
                          docker rmi ${IMAGE_REPO}:latest
                        fi
                        docker rmi ${IMAGE_NAME}
                    '''
                }
            }
        }
        stage('Deploy To Dev') {
            when {
                branch 'dev'
            }
            steps {
                deploy "dev"
            }
        }
        stage('Deploy To QA') {
            when {
                branch 'master'
            }
            steps {
                deploy "qa"
            }
        }
        stage('Deploy To Integ') {
            when {
                branch 'release-1.2'
            }
            steps {
                deploy "integ"
            }
        }
        stage('Deploy To UAT') {
            when {
                branch 'release-1.2'
            }
            steps {
                script {
                    try {
                        timeout (time: 30, unit: "MINUTES") {
                            input message: "Do you want to proceed for UAT deployment?"
                        }
                        deploy "uat"
                    }
                    catch (err) {
                        def user = err.getCauses()[0].getUser()
                        if ('SYSTEM' == user.toString()) { // timeout
                            currentBuild.result = "SUCCESS"
                        }
                    }
                }
            }
        }
    }
}


def fetch_setting_env() {
    withCredentials([file(credentialsId: 'setting_env', variable: 'SETTING_ENV')]) {
        sh '''
            rm -f .env
            cp $SETTING_ENV .env
        '''
    }
}

def deploy(app_env) {
    withCredentials([file(credentialsId: "setting_env_${app_env}", variable: 'SETTING_ENV')]) {
        withEnv(["APP_ENV=${app_env}", "CONSUL_HOST=${app_env}.siglus.us.internal:8500", "DOCKER_HOST=tcp://${app_env}.siglus.us.internal:2376"]) {
            sh '''
                rm -f docker-compose*
                rm -f .env
                rm -f settings.env
                wget https://raw.githubusercontent.com/SIGLUS/siglus-ref-distro/master/docker-compose.yml

                IMAGE_TAG=${BRANCH_NAME}-$(git rev-parse HEAD)
                SERVICE_NAME=siglusapi

                cp $SETTING_ENV settings.env
                sed -i "s#<APP_ENV>#${APP_ENV}#g" settings.env
                echo "OL_SIGLUSAPI_VERSION=${IMAGE_TAG}" > .env

                echo "deregister ${SERVICE_NAME} on ${APP_ENV} consul"
                curl -s http://${CONSUL_HOST}/v1/health/service/${SERVICE_NAME} | \
                jq -r '.[] | "curl -XPUT http://${CONSUL_HOST}/v1/agent/service/deregister/" + .Service.ID' > clear.sh
                chmod a+x clear.sh && ./clear.sh

                echo "deploy ${SERVICE_NAME} on ${APP_ENV}"
                docker-compose -H ${DOCKER_HOST} -f docker-compose.yml -p siglus-ref-distro up --no-deps --force-recreate -d ${SERVICE_NAME}
            '''
        }
    }
}

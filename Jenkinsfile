pipeline {
    agent any
    options {
        buildDiscarder(logRotator(numToKeepStr: '50'))
        timestamps ()
    }
    parameters {
        string(name: 'DEPLOY_INTEG', defaultValue: 'YES')
        string(name: 'DEPLOY_UAT', defaultValue: 'YES')
        string(name: 'DEPLOY_PROD', defaultValue: 'YES')
    }
    environment {
        IMAGE_REPO = "siglusdevops/siglusapi"
        SERVICE_NAME = "siglusapi"
    }
    stages {
        stage('Build') {
            steps {
                println "gradle: build"
                sh '''
                    pwd && ls -l
                    npm install
                    ./gradlew clean build
                '''
                println "sonarqube: analysis"
                withCredentials([string(credentialsId: 'sonar-token', variable: 'SONARQUBE_TOKEN')]) {
                    sh '''
                        if [ "$GIT_BRANCH" = "master" ]; then
                            ./gradlew sonarqube -x test -Dsonar.projectKey=siglus-api -Dsonar.host.url=http://localhost:9000 -Dsonar.login=$SONARQUBE_TOKEN
                        fi
                    '''
                }
                println "docker: push image"
                withCredentials([usernamePassword(credentialsId: "docker-hub", usernameVariable: "USER", passwordVariable: "PASS")]) {
                    sh '''
                        set +x
                        IMAGE_TAG=${BRANCH_NAME}-$(git rev-parse HEAD)
                        IMAGE_NAME=${IMAGE_REPO}:${IMAGE_TAG}
                        docker login -u $USER -p $PASS
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
        }
        stage('Deploy To QA') {
            when {
                branch 'master'
            }
            steps {
                deploy "qa"
            }
        }
        stage('Approval of deploy to Integ') {
            when {
                branch 'master'
            }
            steps {
                script {
                    try {
                        timeout (time: 5, unit: "MINUTES") {
                            input message: "Do you want to proceed for Integ deployment?"
                        }
                    }
                    catch (error) {
                        if ("${error}".startsWith('org.jenkinsci.plugins.workflow.steps.FlowInterruptedException')) {
                            currentBuild.result = "SUCCESS" // Build was aborted
                        }
                        env.DEPLOY_INTEG = 'NO'
                    }
                }
            }
        }
        stage('Deploy To Integ') {
            when {
                allOf{
                    branch 'master'
                    environment name: 'DEPLOY_INTEG', value: 'YES'
                }
            }
            steps {
                deploy "integ"
            }
        }
        stage('Approval of deploy to UAT') {
            when {
                branch 'release'
            }
            steps {
                script {
                    try {
                        timeout (time: 5, unit: "MINUTES") {
                            input message: "Do you want to proceed for UAT deployment?"
                        }
                    }
                    catch (error) {
                        if ("${error}".startsWith('org.jenkinsci.plugins.workflow.steps.FlowInterruptedException')) {
                            currentBuild.result = "SUCCESS" // Build was aborted
                        }
                        env.DEPLOY_UAT = 'NO'
                    }
                }
            }
        }
        stage('Deploy To UAT') {
            when {
                allOf{
                    branch 'release'
                    environment name: 'DEPLOY_UAT', value: 'YES'
                }
            }
            steps {
                deploy "uat"
            }
        }
        stage('Approval of deploy to Production') {
            when {
                branch 'release'
            }
            steps {
                script {
                    try {
                        timeout (time: 5, unit: "MINUTES") {
                            input message: "Do you want to proceed for Production deployment?"
                        }
                    }
                    catch (error) {
                        if ("${error}".startsWith('org.jenkinsci.plugins.workflow.steps.FlowInterruptedException')) {
                            currentBuild.result = "SUCCESS" // Build was aborted
                        }
                        env.DEPLOY_PROD = 'NO'
                    }
                }
            }
        }
        stage('Deploy To Production') {
            when {
                allOf{
                    branch 'release'
                    environment name: 'DEPLOY_PROD', value: 'YES'
                }
            }
            steps {
                deploy "prod"
            }
        }
    }
}


def deploy(app_env) {
    withCredentials([file(credentialsId: "settings.${app_env}.env", variable: 'SETTING_ENV')]) {
        withEnv(["APP_ENV=${app_env}", "CONSUL_HOST=${app_env}.siglus.us.internal:8500"]) {
            sh '''
                IMAGE_TAG=${BRANCH_NAME}-$(git rev-parse HEAD)
                rm -f docker-compose.${APP_ENV}.yml .env settings.${APP_ENV}.env
                echo "OL_SIGLUSAPI_VERSION=${IMAGE_TAG}" > .env
                cp $SETTING_ENV settings.${APP_ENV}.env
                cp ../siglus-ref-distro_master/docker-compose.${APP_ENV}.yml ./

                echo "deregister ${SERVICE_NAME} on ${APP_ENV} consul"
                curl -s http://${CONSUL_HOST}/v1/health/service/${SERVICE_NAME} | jq -r '.[] | "curl -XPUT http://${CONSUL_HOST}/v1/agent/service/deregister/" + .Service.ID' > clear.sh
                chmod a+x clear.sh && ./clear.sh

                echo "deploy ${SERVICE_NAME} on ${APP_ENV}"
                if [ "${APP_ENV}" = "prod" ]; then
                    eval $(docker-machine env manager --shell=bash)
                    docker-machine ls
                    docker service update --force --image ${IMAGE_REPO}:${IMAGE_TAG} siglus_${SERVICE_NAME}
                else
                    eval $(docker-machine env ${APP_ENV} --shell=bash)
                    docker-machine ls
                    docker-compose -f docker-compose.${APP_ENV}.yml -p siglus-ref-distro up --no-deps --force-recreate -d ${SERVICE_NAME}
                fi
            '''
        }
    }
}

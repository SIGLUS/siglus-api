pipeline {
    agent any
    options {
        buildDiscarder(logRotator(numToKeepStr: '50'))
        timestamps ()
    }
    parameters {
        string(name: 'DEPLOY_UAT', defaultValue: 'NO')
        string(name: 'DEPLOY_PROD', defaultValue: 'NO')
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
                            ./gradlew sonarqube -Dsonar.projectKey=siglus-api -Dsonar.host.url=http://10.0.0.91:9000 -Dsonar.login=$SONARQUBE_TOKEN
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
        stage('Deploy To Integ') {
            when {
                branch 'release'
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
                        env.DEPLOY_UAT = 'YES'
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
                    expression{env.BRANCH_NAME = 'release'}
                    expression{env.DEPLOY_UAT = 'YES'}
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
                        env.DEPLOY_PROD = 'YES'
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
                    expression{env.BRANCH_NAME = 'release'}
                    expression{env.DEPLOY_PROD = 'YES'}
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
        withEnv(["APP_ENV=${app_env}", "CONSUL_HOST=${app_env}.siglus.us.internal:8500", "DOCKER_HOST=tcp://${app_env}.siglus.us.internal:2376"]) {
            sh '''
                IMAGE_TAG=${BRANCH_NAME}-$(git rev-parse HEAD)
                rm -f docker-compose.${APP_ENV}.yml .env settings.${APP_ENV}.env
                wget https://raw.githubusercontent.com/SIGLUS/siglus-ref-distro/master/docker-compose.${APP_ENV}.yml
                echo "OL_SIGLUSAPI_VERSION=${IMAGE_TAG}" > .env
                cp $SETTING_ENV settings.${APP_ENV}.env

                echo "deregister ${SERVICE_NAME} on ${APP_ENV} consul"
                curl -s http://${CONSUL_HOST}/v1/health/service/${SERVICE_NAME} | jq -r '.[] | "curl -XPUT http://${CONSUL_HOST}/v1/agent/service/deregister/" + .Service.ID' > clear.sh
                chmod a+x clear.sh && ./clear.sh

                echo "deploy ${SERVICE_NAME} on ${APP_ENV}"
                if [ "${APP_ENV}" = "prod" ]; then
                    docker-machine ls
                    eval $(docker-machine env manager)
                    docker service update --image ${IMAGE_REPO}:${IMAGE_TAG} siglus_${SERVICE_NAME}
                else
                    docker-compose -H ${DOCKER_HOST} -f docker-compose.${APP_ENV}.yml -p siglus-ref-distro up --no-deps --force-recreate -d ${SERVICE_NAME}
                fi
            '''
        }
    }
}

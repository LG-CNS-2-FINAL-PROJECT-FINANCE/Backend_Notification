#!/usr/bin/env groovy
def APP_NAME
def APP_VERSION
def DOCKER_IMAGE_NAME
def PROD_IMAGE_NAME
def HELM_VALUES

pipeline {
    agent any

    environment {
        USER_EMAIL = 'ssassaium@gmail.com'
        USER_ID = 'kaebalsaebal'
        REGISTRY_HOST = credentials('DEV_REGISTRY')
        PROD_REGISTRY = credentials('PROD_REGISTRY')
        SERVICE_NAME = '(서비스 이름: product, market 등...)'
    }

    tools {
        gradle 'Gradle 8.14.2' // 젠킨스 Tools의 Gradle 이름
        jdk 'OpenJDK 17' // 젠킨스 Tools의 JDK 이름
    }

    stages {
		    // master/main 브랜취시 aws ecr 연결
		    stage('Login into AWS When Master Branch') {
            when {
                expression { env.BRANCH_NAME == 'master' || env.BRANCH_NAME == 'main' }
            }
            steps {
                script {

                    REGISTRY_HOST = PROD_REGISTRY

                    withCredentials([[
                        $class: 'AmazonWebServicesCredentialsBinding',
                        credentialsId: 'aws-credential',
                        accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                        secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
                    ]]) {
                        sh """
                        aws ecr get-login-password --region ap-northeast-2 \
                        | podman login --username AWS --password-stdin ${REGISTRY_HOST}
                        """
                    }
                }
            }
        }

        stage('Set Version') {
            steps {
                script {
                    APP_NAME = sh (
                            script: "gradle -q getAppName",
                            returnStdout: true
                    ).trim()
                    APP_VERSION = sh (
                            script: "gradle -q getAppVersion",
                            returnStdout: true
                    ).trim()

                    DOCKER_IMAGE_NAME = "${REGISTRY_HOST}/${APP_NAME}:${APP_VERSION}"

                    sh "echo IMAGE_NAME is ${APP_NAME}"
                    sh "echo IMAGE_VERSION is ${APP_VERSION}"
                    sh "echo DOCKER_IMAGE_NAME is ${DOCKER_IMAGE_NAME}"
                }
            }
        }

        stage('Checkout Branch') {
            steps {
                // Git에서 해당 브랜취의 코드를 가져옵니다.
                checkout scm
            }
        }

        stage('Build Spring Boot App') {
            steps {
                // gradlew 권한부여
                sh 'chmod +x gradlew'
                // Gradlew로 빌드
                sh './gradlew clean build'
            }
        }

        stage('Image Build and Push to Registry') {
            steps {
                script {
                    // 이미지 빌드
                    sh "echo Image building..."
                    sh "podman build -t ${DOCKER_IMAGE_NAME} ."
                    // 레지스트리 푸쉬 - dev와 master 분리
                    if (env.BRANCH_NAME == 'dev'){
                        sh "echo Image pushing to local registry..."
                        sh "podman push ${DOCKER_IMAGE_NAME}"
                    }
                    // master/main 브랜취일시 ecr로 푸쉬
                    else if (env.BRANCH_NAME == 'master' || env.BRANCH_NAME == 'main'){
                        PROD_IMAGE_NAME = "${REGISTRY_HOST}/${APP_NAME}:${APP_VERSION}"
                        sh "echo Image pushing to prod registry..."
                        sh "podman tag ${DOCKER_IMAGE_NAME} ${PROD_IMAGE_NAME}"
                        sh "podman push ${PROD_IMAGE_NAME}"
                    }
                    // 로컬 이미지 제거
                    sh "podman rmi -f ${DOCKER_IMAGE_NAME} || true"
                }
            }
        }

        stage('Update Helm Values') {
            steps{
                script{
                    withCredentials([usernamePassword(
                        credentialsId:'github-credential',
                        usernameVariable: 'GIT_USERNAME',
                        passwordVariable: 'GIT_PASSWORD'
                    )]) {
                        def imageRepo = "${REGISTRY_HOST}/${APP_NAME}"
                        def imageTag = "${APP_VERSION}"
                        def MANIFEST_REPO = "https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/LG-CNS-2-FINAL-PROJECT-FINANCE/Backend_Manifests.git"

												// master/main 브랜취용 매니페스트 분리
                        if (env.BRANCH_NAME == 'master' || env.BRANCH_NAME == 'main'){
                            HELM_VALUES = 'values-prod'
                        } else if (env.BRANCH_NAME == 'dev') {
                            HELM_VALUES = 'values-dev'
                        }

                        sh """
                             # Git 사용자 정보 설정(커밋 사용자 명시땜에)
                            git config --global user.email "${USER_EMAIL}"
                            git config --global user.name "${USER_ID}"

                            # 매니페스트 레포 클론
                            git clone ${MANIFEST_REPO}
                            cd Backend_Manifests

                            # yq를 사용하여 개발 환경의 values 파일 업데이트
                            yq -i '.image.repository = "${imageRepo}"' helm_chart/${SERVICE_NAME}/${HELM_VALUES}.yaml
                            yq -i '.image.tag = "${imageTag}"' helm_chart/${SERVICE_NAME}/${HELM_VALUES}.yaml

                            # 변경 사항 커밋 및 푸시
                            if ! git diff --quiet; then
                              git add helm_chart/${SERVICE_NAME}/${HELM_VALUES}.yaml
                              git commit -m "Update image tag for dev to ${DOCKER_IMAGE_NAME} [skip ci]"
                              git push origin master
                            else
                              echo "No changes to commit."
                            fi
                        """
                    }
                }
            }
        }

        stage('Clean Workspace') {
            steps {
                deleteDir() // workspace 전체 정리
            }
        }
    }

    // 빌드 완료 후
    post {
        // 성공이든, 실패든 항상 수행
        always {
            echo "Cleaning up workspace..."
            deleteDir() // workspace 전체 정리
            echo "Cleaning up podman..."
            sh "podman image prune -af || true" // podman 찌꺼기가 쌓여, 정리
            sh "podman container prune -f || true" // podman 찌꺼기가 쌓여, 정리
        }
    }
}
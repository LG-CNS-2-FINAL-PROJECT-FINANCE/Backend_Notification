#!/usr/bin/env groovy
def APP_NAME
def APP_VERSION
def DOCKER_IMAGE_NAME

pipeline {
    agent any

    environment {
        REGISTRY_HOST = "192.168.56.200:5000"
        USER_EMAIL = 'ssassaium@gmail.com'
        USER_ID = 'kaebalsaebal'
        SERVICE_NAME = 'notification'

    }

    tools {
        gradle 'Gradle 8.14.2' // 젠킨스 Tools의 Gradle 이름
        jdk 'OpenJDK 17' // 젠킨스 Tools의 JDK 이름
        //dockerTool 'Docker' // 젠킨스 Tools의 Docker 이름
    }

    stages {
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

        stage('Checkout Dev Branch') {
            steps {
                // Git에서 dev 브랜치의 코드를 가져옵니다.
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
                // 이미지 빌드
                sh "echo Image building..."
                sh "podman build -t ${DOCKER_IMAGE_NAME} ."
                // 레지스트리 푸쉬
                sh "echo Image pushing to local registry..."
                sh "podman push ${DOCKER_IMAGE_NAME}"
                // 로컬 이미지 제거
                sh "podman rmi -f ${DOCKER_IMAGE_NAME} || true"
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

                        sh """
                             # Git 사용자 정보 설정(커밋 사용자 명시땜에)
                            git config --global user.email "${USER_EMAIL}"
                            git config --global user.name "${USER_ID}"

                            # 매니페스트 레포 클론
                            git clone ${MANIFEST_REPO}
                            cd Backend_Manifests

                            # yq를 사용하여 개발 환경의 values 파일 업데이트
                            yq -i '.image.repository = "${imageRepo}"' helm_chart/${SERVICE_NAME}/values-dev.yaml
                            yq -i '.image.tag = "${imageTag}"' helm_chart/${SERVICE_NAME}/values-dev.yaml

                            # 변경 사항 커밋 및 푸시
                            if ! git diff --quiet; then
                              git add helm_chart/${SERVICE_NAME}/values-dev.yaml
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
        }
    }
}
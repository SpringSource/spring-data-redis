pipeline {
	agent none

	triggers {
		pollSCM 'H/10 * * * *'
		upstream(upstreamProjects: "spring-data-keyvalue/main", threshold: hudson.model.Result.SUCCESS)
	}

	options {
		disableConcurrentBuilds()
		buildDiscarder(logRotator(numToKeepStr: '14'))
	}

	stages {
		stage("Docker images") {
			parallel {
				stage('Publish OpenJDK 8 + Redis 6.2 docker image') {
					when {
						anyOf {
							changeset "ci/openjdk8-redis-6.2/**"
							changeset "Makefile"
						}
					}
					agent { label 'data' }
					options { timeout(time: 20, unit: 'MINUTES') }

					steps {
						script {
							def image = docker.build("springci/spring-data-openjdk8-with-redis-6.2", "-f ci/openjdk8-redis-6.2/Dockerfile .")
							docker.withRegistry('', 'hub.docker.com-springbuildmaster') {
								image.push()
							}
						}
					}
				}
				stage('Publish OpenJDK 11 + Redis 6.2 docker image') {
					when {
						anyOf {
							changeset "ci/openjdk11-redis-6.2/**"
							changeset "Makefile"
						}
					}
					agent { label 'data' }
					options { timeout(time: 20, unit: 'MINUTES') }

					steps {
						script {
							def image = docker.build("springci/spring-data-openjdk11-with-redis-6.2", "-f ci/openjdk11-redis-6.2/Dockerfile .")
							docker.withRegistry('', 'hub.docker.com-springbuildmaster') {
								image.push()
							}
						}
					}
				}
				stage('Publish OpenJDK 16 + Redis 6.2 docker image') {
					when {
						anyOf {
							changeset "ci/openjdk16-redis-6.2/**"
							changeset "Makefile"
						}
					}
					agent { label 'data' }
					options { timeout(time: 20, unit: 'MINUTES') }

					steps {
						script {
							def image = docker.build("springci/spring-data-openjdk16-with-redis-6.2", "-f ci/openjdk16-redis-6.2/Dockerfile .")
							docker.withRegistry('', 'hub.docker.com-springbuildmaster') {
								image.push()
							}
						}
					}
				}
			}
		}

		stage("test: baseline (jdk8)") {
			when {
				anyOf {
					branch 'main'
					not { triggeredBy 'UpstreamCause' }
				}
			}
			agent {
				label 'data'
			}
			options { timeout(time: 30, unit: 'MINUTES') }
			environment {
				ARTIFACTORY = credentials('02bd1690-b54f-4c9f-819d-a77cb7a9822c')
			}
			steps {
				script {
					docker.withRegistry('', 'hub.docker.com-springbuildmaster') {
						docker.image('springci/spring-data-openjdk8-with-redis-6.2:latest').inside('-v $HOME:/tmp/jenkins-home') {
							sh 'PROFILE=none LONG_TESTS=true ci/test.sh'
						}
					}
				}
			}
		}

		stage("Test other configurations") {
			when {
				allOf {
					branch 'main'
					not { triggeredBy 'UpstreamCause' }
				}
			}
			parallel {
				stage("test: baseline (jdk11)") {
					agent {
						label 'data'
					}
					options { timeout(time: 30, unit: 'MINUTES') }
					environment {
						ARTIFACTORY = credentials('02bd1690-b54f-4c9f-819d-a77cb7a9822c')
					}
					steps {
						script {
							docker.withRegistry('', 'hub.docker.com-springbuildmaster') {
								docker.image('springci/spring-data-openjdk11-with-redis-6.2:latest').inside('-v $HOME:/tmp/jenkins-home') {
									sh 'PROFILE=java11 ci/test.sh'
								}
							}
						}
					}
				}
				stage("test: baseline (jdk16)") {
					agent {
						label 'data'
					}
					options { timeout(time: 30, unit: 'MINUTES') }
					environment {
						ARTIFACTORY = credentials('02bd1690-b54f-4c9f-819d-a77cb7a9822c')
					}
					steps {
						script {
							docker.withRegistry('', 'hub.docker.com-springbuildmaster') {
								docker.image('springci/spring-data-openjdk16-with-redis-6.2:latest').inside('-v $HOME:/tmp/jenkins-home') {
									sh 'PROFILE=java11 ci/test.sh'
								}
							}
						}
					}
				}
			}
		}

		stage('Release to artifactory') {
			when {
				anyOf {
					branch 'main'
					not { triggeredBy 'UpstreamCause' }
				}
			}
			agent {
				label 'data'
			}
			options { timeout(time: 20, unit: 'MINUTES') }

			environment {
				ARTIFACTORY = credentials('02bd1690-b54f-4c9f-819d-a77cb7a9822c')
			}

			steps {
				script {
					docker.withRegistry('', 'hub.docker.com-springbuildmaster') {
						docker.image('adoptopenjdk/openjdk8:latest').inside('-v $HOME:/tmp/jenkins-home') {
							sh 'MAVEN_OPTS="-Duser.name=jenkins -Duser.home=/tmp/jenkins-home" ./mvnw -s settings.xml -Pci,artifactory ' +
									'-Dartifactory.server=https://repo.spring.io ' +
									"-Dartifactory.username=${ARTIFACTORY_USR} " +
									"-Dartifactory.password=${ARTIFACTORY_PSW} " +
									"-Dartifactory.staging-repository=libs-snapshot-local " +
									"-Dartifactory.build-name=spring-data-redis " +
									"-Dartifactory.build-number=${BUILD_NUMBER} " +
									'-Dmaven.test.skip=true clean deploy -U -B'
						}
					}
				}
			}
		}

		stage('Publish documentation') {
			when {
				branch 'main'
			}
			agent {
				label 'data'
			}
			options { timeout(time: 20, unit: 'MINUTES') }

			environment {
				ARTIFACTORY = credentials('02bd1690-b54f-4c9f-819d-a77cb7a9822c')
			}

			steps {
				script {
					docker.withRegistry('', 'hub.docker.com-springbuildmaster') {
						docker.image('adoptopenjdk/openjdk8:latest').inside('-v $HOME:/tmp/jenkins-home') {
							sh 'MAVEN_OPTS="-Duser.name=jenkins -Duser.home=/tmp/jenkins-home" ./mvnw -s settings.xml -Pci,distribute ' +
									'-Dartifactory.server=https://repo.spring.io ' +
									"-Dartifactory.username=${ARTIFACTORY_USR} " +
									"-Dartifactory.password=${ARTIFACTORY_PSW} " +
									"-Dartifactory.distribution-repository=temp-private-local " +
									'-Dmaven.test.skip=true clean deploy -U -B'
						}
					}
				}
			}
		}
	}

	post {
		changed {
			script {
				slackSend(
						color: (currentBuild.currentResult == 'SUCCESS') ? 'good' : 'danger',
						channel: '#spring-data-dev',
						message: "${currentBuild.fullDisplayName} - `${currentBuild.currentResult}`\n${env.BUILD_URL}")
				emailext(
						subject: "[${currentBuild.fullDisplayName}] ${currentBuild.currentResult}",
						mimeType: 'text/html',
						recipientProviders: [[$class: 'CulpritsRecipientProvider'], [$class: 'RequesterRecipientProvider']],
						body: "<a href=\"${env.BUILD_URL}\">${currentBuild.fullDisplayName} is reported as ${currentBuild.currentResult}</a>")
			}
		}
	}
}

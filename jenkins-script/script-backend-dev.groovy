pipeline {
    agent any
    environment {
        GITHUB_TOKEN=credentials('GITHUB_TOKEN')
        REGISTRY_DEV=credentials('REGISTRY_DEV')
    }
    tools {
        gradle('gradle8.10.2')
    }
    stages {
        stage('github clone') {
            steps {
                sh '''
                if [ -d "bha-backend/.git" ]; then
                    cd bha-backend
                    git reset --hard
                    git clean -fd
                    git checkout dev
                    git pull origin dev
                else
                    rm -rf bha-backend
                    git clone -b dev https://$GITHUB_TOKEN@github.com/hojoon90/BHA-Backend.git
                    mv BHA-Backend bha-backend
                fi
                '''
            }
        }
        stage('build'){
            steps {
                sh '''
                    echo 'copy props'
                    #mkdir -p /home/blahblah/Development/jenkins/workspace/bha-repository/src/main/resources
                    cp /var/jenkins_home/workspace/properties/dev/application.yml /var/jenkins_home/workspace/bha-dev/backend-deploy/bha-backend/bha-api/src/main/resources/application.yml
                    cp /var/jenkins_home/workspace/properties/dev/infra-config.yml /var/jenkins_home/workspace/bha-dev/backend-deploy/bha-backend/bha-infra/src/main/resources/infra-config.yml
                    cp /var/jenkins_home/workspace/properties/dev/jpa-config.yml /var/jenkins_home/workspace/bha-dev/backend-deploy/bha-backend/bha-core/src/main/resources/jpa-config.yml

                    echo 'build'
                    gradle -Denv=dev --build-file bha-backend/build.gradle bha-api:clean
                    gradle -Denv=dev --build-file bha-backend/build.gradle bha-api:bootjar
                    
                    echo 'delete props'
                    rm /var/jenkins_home/workspace/bha-dev/backend-deploy/bha-backend/bha-api/src/main/resources/application.yml
                    rm /var/jenkins_home/workspace/bha-dev/backend-deploy/bha-backend/bha-core/src/main/resources/jpa-config.yml
                    rm /var/jenkins_home/workspace/bha-dev/backend-deploy/bha-backend/bha-infra/src/main/resources/infra-config.yml
                '''
            }
        }
        stage('deploy'){
            steps {
                sh '''
                    echo 'make dir'
                    ssh -T blahblah@192.168.my.ip -p 22 "mkdir -p /home/blahblah/Development/apps/dev/bha-backend"
                    ssh -T blahblah@192.168.my.ip -p 22 "mkdir -p /home/blahblah/Development/apps/dev/bha-backup"
                    
                    echo 'backup'
                    ssh -T blahblah@192.168.my.ip -p 22 "mv /home/blahblah/Development/apps/dev/bha-backend/bha-api*.jar /home/blahblah/Development/apps/dev/bha-backup/bha-api_$(date +"%Y%m%d_%H%M%S").jar || :"
                    
                    echo 'upload jar to dev'
                    scp -P 22 bha-backend/bha-api/build/libs/bha-api*.jar blahblah@192.168.my.ip:/home/blahblah/Development/apps/dev/bha-backend/bha-api.jar
                    scp -P 22 bha-backend/bha-api/docker/Dockerfile blahblah@192.168.my.ip:/home/blahblah/Development/apps/dev/bha-backend/Dockerfile
                '''
            }
        }
        stage('docker-push'){
            steps{
                sh '''
                    echo 'docker-push'
                
                    #ssh -T blahblah@192.168.my.ip "docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $CI_REGISTRY"
                    ssh -T blahblah@192.168.my.ip -p 22 "docker build -t $REGISTRY_DEV/bha-api:$BUILD_ID -f /home/blahblah/Development/apps/dev/bha-backend/Dockerfile /home/blahblah/Development/apps/dev/bha-backend"
                    ssh -T blahblah@192.168.my.ip -p 22 "docker push $REGISTRY_DEV/bha-api:$BUILD_ID"
                '''
            }
        }
        stage('rollout-api'){
            steps{
                sh '''
                        echo 'rollout api'
                        var=$(ssh -T blahblah@192.168.my.ip -p 22 "docker ps -q -f name=bha-api")
                        if [ -n "$var" ]; then
                            echo "stop container"
                            ssh -T blahblah@192.168.my.ip -p 22 "docker stop bha-api"
                        else
                            echo "No running container found."
                        fi
                        ssh -T blahblah@192.168.my.ip -p 22 "docker rm bha-api"
                        ssh -T blahblah@192.168.my.ip -p 22 "docker run -d \
                                --name bha-api \
                                -p 8080:8080 \
                                --network=bha-network \
                                -v /home/blahblah/Development/apps/dev/files:/apps/bha/files \
                                $REGISTRY_DEV/bha-api:$BUILD_ID"
                    '''
            }
        }
    }
}

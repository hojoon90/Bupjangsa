pipeline {
    agent any
    environment {
        GITHUB_TOKEN=credentials('GITHUB_TOKEN')
        REGISTRY_DEV=credentials('REGISTRY_DEV')
    }
    stages {
        stage('github clone') {
            steps {
                sh '''
                if [ -d "bha-frontend/.git" ]; then
                    cd bha-frontend
                    git reset --hard
                    git clean -fd
                    git checkout dev
                    git pull origin dev
                else
                    rm -rf bha-frontend
                    git clone -b dev https://$GITHUB_TOKEN@github.com/hojoon90/BHA-Frontend.git
                    mv BHA-Frontend bha-frontend
                fi
                '''
            }
        }
        stage('build'){
            steps {
                sh '''
                    echo 'copy props'
                    cp /var/jenkins_home/workspace/properties/dev/.env.development /var/jenkins_home/workspace/bha-dev/frontend-deploy/bha-frontend/.env
                '''
            }
        }
        stage('deploy'){
            steps {
                sh '''
                    echo 'backup'
                    ssh -T blahblah@192.168.my.ip -p 22 "mv /home/blahblah/Development/apps/dev/bha-frontend /home/blahblah/Development/apps/dev/bha-backup-front/bha-frontend.web_$(date +"%Y%m%d_%H%M%S") || :"
                    ssh -T blahblah@192.168.my.ip -p 22 "mkdir -p /home/blahblah/Development/apps/dev/bha-frontend"
                    scp -P 22 -r bha-frontend/* bha-frontend/.env blahblah@192.168.my.ip:/home/blahblah/Development/apps/dev/bha-frontend
                    ssh -T blahblah@192.168.my.ip -p 22 "rm -rf /home/blahblah/Development/apps/dev/dev/bha-frontend/.git"
                    scp -P 22 bha-frontend/docker/Dockerfile_dev blahblah@192.168.my.ip:/home/blahblah/Development/apps/dev/bha-frontend/Dockerfile

                '''
            }
        }
        stage('docker-push'){
            steps{
                sh '''
                    echo 'docker push'
                
                    ssh -T blahblah@192.168.my.ip -p 22 "docker build -t $REGISTRY_DEV/bha-web:$BUILD_ID -f /home/blahblah/Development/apps/dev/bha-frontend/Dockerfile /home/blahblah/Development/apps/dev/bha-frontend"
                    ssh -T blahblah@192.168.my.ip -p 22 "docker push $REGISTRY_DEV/bha-web:$BUILD_ID"
                '''
            }
        }
        stage('rollout-web'){
            steps{
                sh '''
                        echo 'rollout web'
                        var=$(ssh -T blahblah@192.168.my.ip -p 22 "docker ps -q -f name=bha-web")
                        if [ -n "$var" ]; then
                            echo "stop container"
                            ssh -T blahblah@192.168.my.ip -p 22 "docker stop bha-web"
                        else
                            echo "No running container found."
                        fi
                        ssh -T blahblah@192.168.my.ip -p 22 "docker rm bha-web"
                        ssh -T blahblah@192.168.my.ip -p 22 "docker run -d --name bha-web -p 3000:3000 --network=bha-network $REGISTRY_DEV/bha-web:$BUILD_ID"
                    '''
            }
        }
    }
}

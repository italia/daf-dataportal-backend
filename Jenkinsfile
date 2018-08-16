pipeline{
    agent any
     stages {
        stage('Build') {
         steps {
             script{
             if(env.BRANCH_NAME=='testci2'|| env.BRANCH_NAME=='security-enhancements'){
                sh '''
                sbt " -DSTAGING=true; reload; clean; compile;  docker:publish"
                '''
                }
            }
         }
        }
        stage('Staging'){
            steps{
            script{
                if(env.BRANCH_NAME=='testci2'|| env.BRANCH_NAME== 'security-enhancements'){
                    kubectl delete -f  daf_datipubblici_test.yaml
                    sh '''
                    cd kubernetes
                    ./config-map-test.sh                    
                    kubectl apply -f  daf_datipubblici_test.yaml
                    '''
            }
            }
        }
     }
     }
}

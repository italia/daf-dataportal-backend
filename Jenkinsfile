pipeline{
    agent any
     stages {
        stage('Build') {
         steps {
             script{
             if(env.BRANCH_NAME=='testci2'|| env.BRANCH_NAME=='security-enhancements'){
                sh '''
                STAGING=true;
                sbt " -DSTAGING=$STAGING; reload; clean; compile;  docker:publish"
                '''
                }
            }
         }
        }
        stage('Staging'){
            steps{
            script{
                if(env.BRANCH_NAME=='testci2'|| env.BRANCH_NAME== 'security-enhancements'){
                    sh '''
                    kubectl delete configmap datipubblici-conf
                    kubectl create configmap datipubblici-conf --from-file=conf/test/prodBase.conf
                    cd kubernetes
                    kubectl delete -f  daf_datipubblici_test.yaml
                    kubectl create -f  daf_datipubblici_test.yaml
                    '''
                }
            }
            }
        }
     }
}

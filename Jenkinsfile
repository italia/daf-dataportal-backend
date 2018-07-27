pipeline{
    agent any
     stages {
        stage('Build') {
         steps {
             script{
             if(env.BRANCH_NAME=='testci2'){
                sh '''
                STAGING=true;
                sbt " -DSTAGING=$STAGING; reload ; compile;  docker:publish"
                '''
                }
            }
         }
        }
        stage('Staging'){
            steps{
            script{
                if(env.BRANCH_NAME=='testci2'){
                    sh '''
                    ls
                    kubectl create --from-file=conf/test/prodBase.conf
                    '''
                }
            }
            }
        }
     }
}

pipeline{
    agent any
     stages {
        /*stage('Build') {
         steps {
             script{
             if(env.BRANCH_NAME=='testci'){
                sh '''
                STAGING=true;
                sbt " -DSTAGING=$STAGING; reload ; compile;  docker:publish"
                '''
                }
            }
         }
        }*/
        stage('Staging'){
            script{
                if(env.BRANCH_NAME=='testci'){
                    sh '''
                    ls
                    kubectl create -f conf/test/prodBase.conf
                    '''
                }
            }
        }
     }
}

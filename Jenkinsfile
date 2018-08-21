pipeline{
    agent any
     stages {
        stage('Build') {
         steps {
             script{
             if(env.BRANCH_NAME=='testci2'|| env.BRANCH_NAME=='security-enhancements'){
                slackSend (message: "BUILD START: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' CHECK THE RESULT ON: https://cd.daf.teamdigitale.it/blue/organizations/jenkins/CI-Dataportal_Backend/activity")
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
                    //kubectl delete -f  daf_datipubblici_test.yaml
                    sh '''
                    cd kubernetes
                    ./config-map-test.sh                    
                    kubectl apply -f  daf_datipubblici_test.yaml --force
                    '''
                    slackSend (color: '#00FF00', message: "SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' https://cd.daf.teamdigitale.it/blue/organizations/jenkins/CI-Dataportal_Backend/activity")
            }
            }
        }
     }
     }
     post { 
        failure { 
            slackSend (color: '#ff0000', message: "FAIL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' https://cd.daf.teamdigitale.it/blue/organizations/jenkins/CI-Dataportal_Backend/activity")
        }
    }
}

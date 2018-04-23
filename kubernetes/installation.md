## Installation

### Prerequisites

Security Manager must be properly installed and configured before proceed with this installation

### Procedure

The installation depends on the environment where is is run.
For this reason, when executing the following steps, replace \<environment\> with `test` or `prod` accordingly.

1. git clone https://github.com/italia/daf-dataportal-backend.git
2. `sbt docker:publish` to compile and push the docker image on Nexus
3. cd `kubernetes` 
4. `./config-map-<environment>.sh` to create config map
5. `./kubectl create -f daf_datipubblici.yaml` to deploy the containers in kubernetes

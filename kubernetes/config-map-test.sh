#!/usr/bin/env bash
kubectl --kubeconfig=${JENKINS_HOME}/.kube/config.teamdigitale-staging delete configmap datipubblici-conf || true
kubectl --kubeconfig=${JENKINS_HOME}/.kube/config.teamdigitale-staging create configmap datipubblici-conf --from-file=../conf/test/prodBase.conf

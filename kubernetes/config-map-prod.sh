#!/usr/bin/env bash
kubectl delete configmap datipubblici-conf
kubectl create configmap datipubblici-conf --from-file=../conf/prodBase.conf
#!/usr/bin/env bash
kubectl delete configmap datipubblici-conf || true
kubectl create configmap datipubblici-conf --from-file=../conf/test/prodBase.conf

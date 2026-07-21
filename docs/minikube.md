# Minikube Runtime Guide

This guide runs Iron Metrics locally on Kubernetes with Minikube. It assumes the WSL2 Ubuntu workflow already used by the project.

## Requirements

- Docker Engine running inside WSL.
- Minikube installed inside WSL.
- kubectl installed inside WSL.

Check the tools:

```bash
docker --version
minikube version
kubectl version --client
```

## Start Minikube

```bash
minikube start --driver=docker
```

Point the current shell to Minikube's Docker daemon so the local image is built directly inside the cluster:

```bash
eval $(minikube docker-env)
```

Run that command again whenever you open a new WSL terminal for this workflow.

## Build the API Image

From the project root:

```bash
docker build -t iron-metrics:local .
```

The Kubernetes deployment uses:

```text
image: iron-metrics:local
imagePullPolicy: IfNotPresent
```

That keeps Minikube from trying to pull the local image from a registry.

## Apply Kubernetes Manifests

```bash
kubectl apply -k k8s
```

Wait for PostgreSQL and the API:

```bash
kubectl -n iron-metrics rollout status deployment/iron-metrics-postgres
kubectl -n iron-metrics rollout status deployment/iron-metrics-api
```

Check pod status:

```bash
kubectl -n iron-metrics get pods
```

## Access the API

Forward the service locally:

```bash
kubectl -n iron-metrics port-forward service/iron-metrics-api 8080:8080
```

Useful endpoints:

```text
http://localhost:8080/api/v1/actuator/health/liveness
http://localhost:8080/api/v1/actuator/health/readiness
http://localhost:8080/api/v1/swagger-ui.html
```

## Configuration

Runtime configuration lives in:

```text
k8s/configmap.yaml
k8s/secret.yaml
```

The checked-in secret is only for local Minikube development. Replace `IRON_METRICS_DB_PASSWORD` and `IRON_METRICS_JWT_SECRET` before using any shared or production-like cluster.

## Cleanup

Remove Iron Metrics resources:

```bash
kubectl delete -k k8s
```

Stop Minikube:

```bash
minikube stop
```

Delete the local Minikube cluster and its volumes:

```bash
minikube delete
```

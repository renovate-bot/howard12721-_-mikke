#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ARGOCD_NAMESPACE="${ARGOCD_NAMESPACE:-argocd}"
KEY_FILE="${SOPS_AGE_KEY_FILE:-${HOME}/.config/sops/age/keys.txt}"

if [ ! -f "${KEY_FILE}" ] && [ -f "${ROOT_DIR}/.secrets/sops/age/mikke.agekey" ]; then
  KEY_FILE="${ROOT_DIR}/.secrets/sops/age/mikke.agekey"
fi

if ! command -v kubectl >/dev/null 2>&1; then
  echo "kubectl is required" >&2
  exit 1
fi

if [ ! -f "${KEY_FILE}" ]; then
  echo "SOPS age key file not found: ${KEY_FILE}" >&2
  exit 1
fi

kubectl -n "${ARGOCD_NAMESPACE}" create secret generic argocd-sops-age \
  --from-file=keys.txt="${KEY_FILE}" \
  --dry-run=client \
  -o yaml | kubectl apply -f -

kubectl apply -f "${ROOT_DIR}/infra/k8s/argocd/ksops-plugin-configmap.yaml"

kubectl -n "${ARGOCD_NAMESPACE}" patch deployment argocd-repo-server \
  --type strategic \
  --patch-file "${ROOT_DIR}/infra/k8s/argocd/repo-server-ksops-patch.yaml"

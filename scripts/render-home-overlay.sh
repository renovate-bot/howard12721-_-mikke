#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BIN_DIR="${ROOT_DIR}/.bin"
KEY_FILE="${SOPS_AGE_KEY_FILE:-${ROOT_DIR}/.secrets/sops/age/mikke.agekey}"

mkdir -p "${BIN_DIR}"

install_ksops() {
  curl -fsSL https://raw.githubusercontent.com/viaduct-ai/kustomize-sops/master/scripts/install-ksops-archive.sh | bash -s -- "${BIN_DIR}" >/dev/null
  chmod +x "${BIN_DIR}/ksops"
}

install_kustomize() {
  local tmp_dir
  tmp_dir="$(mktemp -d)"
  (
    cd "${tmp_dir}"
    curl -fsSL https://raw.githubusercontent.com/kubernetes-sigs/kustomize/master/hack/install_kustomize.sh | bash >/dev/null
    mv kustomize "${BIN_DIR}/kustomize"
  )
  rm -rf "${tmp_dir}"
  chmod +x "${BIN_DIR}/kustomize"
}

if ! "${BIN_DIR}/ksops" version >/dev/null 2>&1; then
  install_ksops
fi

if ! "${BIN_DIR}/kustomize" version >/dev/null 2>&1; then
  install_kustomize
fi

if [ ! -f "${KEY_FILE}" ]; then
  echo "SOPS age key file not found: ${KEY_FILE}" >&2
  exit 1
fi

PATH="${BIN_DIR}:${PATH}" \
SOPS_AGE_KEY_FILE="${KEY_FILE}" \
  kustomize build --enable-alpha-plugins --enable-exec "${ROOT_DIR}/infra/k8s/overlays/home"

#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BIN_DIR="${ROOT_DIR}/.bin"
KEY_FILE="${SOPS_AGE_KEY_FILE:-${ROOT_DIR}/.secrets/sops/age/mikke.agekey}"
KSOPS_VERSION="4.5.1"
KUSTOMIZE_VERSION="5.8.1"

mkdir -p "${BIN_DIR}"

detect_platform() {
  local os arch

  os="$(uname -s)"
  arch="$(uname -m)"

  case "${os}" in
    Linux)
      PLATFORM_OS="linux"
      KSOPS_OS="Linux"
      ;;
    Darwin)
      PLATFORM_OS="darwin"
      KSOPS_OS="Darwin"
      ;;
    *)
      echo "Unsupported operating system: ${os}" >&2
      exit 1
      ;;
  esac

  case "${arch}" in
    x86_64|amd64)
      PLATFORM_ARCH="amd64"
      KSOPS_ARCH="x86_64"
      ;;
    arm64|aarch64)
      PLATFORM_ARCH="arm64"
      KSOPS_ARCH="arm64"
      ;;
    *)
      echo "Unsupported architecture: ${arch}" >&2
      exit 1
      ;;
  esac
}

sha256_file() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{ print $1 }'
  else
    shasum -a 256 "$1" | awk '{ print $1 }'
  fi
}

verify_sha256() {
  local file_path expected actual

  file_path="$1"
  expected="$2"
  actual="$(sha256_file "${file_path}")"

  if [ "${actual}" != "${expected}" ]; then
    echo "Checksum verification failed for ${file_path}" >&2
    echo "expected: ${expected}" >&2
    echo "actual:   ${actual}" >&2
    exit 1
  fi
}

download_and_extract_binary() {
  local url expected_sha binary_name destination
  local tmp_dir archive_path extracted_path

  url="$1"
  expected_sha="$2"
  binary_name="$3"
  destination="$4"
  tmp_dir="$(mktemp -d)"
  archive_path="${tmp_dir}/archive.tar.gz"
  extracted_path="${tmp_dir}/${binary_name}"

  curl -fsSL -o "${archive_path}" "${url}"
  verify_sha256 "${archive_path}" "${expected_sha}"
  tar -xzf "${archive_path}" -C "${tmp_dir}"
  mv "${extracted_path}" "${destination}"
  chmod +x "${destination}"
  rm -rf "${tmp_dir}"
}

mark_installed_version() {
  printf '%s\n' "$2" > "${BIN_DIR}/$1.version"
}

needs_install() {
  local binary_name expected_version
  local binary_path version_file

  binary_name="$1"
  expected_version="$2"
  binary_path="${BIN_DIR}/${binary_name}"
  version_file="${BIN_DIR}/${binary_name}.version"

  [ ! -x "${binary_path}" ] || [ ! -f "${version_file}" ] || [ "$(cat "${version_file}")" != "${expected_version}" ]
}

install_ksops() {
  local archive_name expected_sha

  case "${KSOPS_OS}_${KSOPS_ARCH}" in
    Darwin_x86_64)
      archive_name="ksops_${KSOPS_VERSION}_Darwin_x86_64.tar.gz"
      expected_sha="16359177ac71c66b50e79b46ec76b2406974ecc65333cc64e5ec3dae69306d5a"
      ;;
    Darwin_arm64)
      archive_name="ksops_${KSOPS_VERSION}_Darwin_arm64.tar.gz"
      expected_sha="8fd97fe1dd4acd96cfd3fc286915d564e445aeeec99c39f4902f2da58d64199e"
      ;;
    Linux_amd64|Linux_x86_64)
      archive_name="ksops_${KSOPS_VERSION}_Linux_x86_64.tar.gz"
      expected_sha="9641e03a301bf2fc4b98d0e837a0351e1ca443ecd7a68b25e6af2149fbb2ef30"
      ;;
    Linux_arm64)
      archive_name="ksops_${KSOPS_VERSION}_Linux_arm64.tar.gz"
      expected_sha="c7bb562cc2d580a1decc6c77a35e4d35575fb01ca003425726004ecd13dc1cb5"
      ;;
    *)
      echo "Unsupported platform for ksops: ${KSOPS_OS}_${KSOPS_ARCH}" >&2
      exit 1
      ;;
  esac

  download_and_extract_binary \
    "https://github.com/viaduct-ai/kustomize-sops/releases/download/v${KSOPS_VERSION}/${archive_name}" \
    "${expected_sha}" \
    "ksops" \
    "${BIN_DIR}/ksops"
  mark_installed_version "ksops" "${KSOPS_VERSION}"
}

install_kustomize() {
  local archive_name expected_sha

  archive_name="kustomize_v${KUSTOMIZE_VERSION}_${PLATFORM_OS}_${PLATFORM_ARCH}.tar.gz"

  case "${PLATFORM_OS}_${PLATFORM_ARCH}" in
    darwin_amd64)
      expected_sha="ee7cf0c1e3592aa7bb66ba82b359933a95e7f2e0b36e5f53ed0a4535b017f2f8"
      ;;
    darwin_arm64)
      expected_sha="8886f8a78474e608cc81234f729fda188a9767da23e28925802f00ece2bab288"
      ;;
    linux_amd64)
      expected_sha="029a7f0f4e1932c52a0476cf02a0fd855c0bb85694b82c338fc648dcb53a819d"
      ;;
    linux_arm64)
      expected_sha="0953ea3e476f66d6ddfcd911d750f5167b9365aa9491b2326398e289fef2c142"
      ;;
    *)
      echo "Unsupported platform for kustomize: ${PLATFORM_OS}_${PLATFORM_ARCH}" >&2
      exit 1
      ;;
  esac

  download_and_extract_binary \
    "https://github.com/kubernetes-sigs/kustomize/releases/download/kustomize/v${KUSTOMIZE_VERSION}/${archive_name}" \
    "${expected_sha}" \
    "kustomize" \
    "${BIN_DIR}/kustomize"
  mark_installed_version "kustomize" "${KUSTOMIZE_VERSION}"
}

detect_platform

if needs_install "ksops" "${KSOPS_VERSION}"; then
  install_ksops
fi

if needs_install "kustomize" "${KUSTOMIZE_VERSION}"; then
  install_kustomize
fi

if [ ! -f "${KEY_FILE}" ]; then
  echo "SOPS age key file not found: ${KEY_FILE}" >&2
  exit 1
fi

PATH="${BIN_DIR}:${PATH}" \
SOPS_AGE_KEY_FILE="${KEY_FILE}" \
  kustomize build --enable-alpha-plugins --enable-exec "${ROOT_DIR}/infra/k8s/overlays/home"

#!/usr/bin/env bash
# edutrack-ms-commons — installer / updater
#
# Idempotent, stateless: clona el repo en un directorio temporal, hace
# `./mvnw clean install` y deja el artefacto en el repositorio Maven local
# (~/.m2/repository). Sirve igual como instalador y como updater — siempre
# trae el ref más reciente y reinstala desde cero.
#
# One-liner:
#   curl -fsSL https://raw.githubusercontent.com/goslynn/edutrack-ms-commons/master/install.sh | bash
#
# Variables de entorno opcionales:
#   COMMONS_REPO_URL   URL del repo (default: https://github.com/goslynn/edutrack-ms-commons.git)
#   COMMONS_REF        Branch/tag/SHA a instalar (default: master)
#   COMMONS_RUN_TESTS  "1" para ejecutar los tests durante install (default: 0)
#   COMMONS_KEEP_TMP   "1" para no borrar el clon temporal al terminar (default: 0)

set -Eeuo pipefail

REPO_URL="${COMMONS_REPO_URL:-https://github.com/goslynn/edutrack-ms-commons.git}"
REF="${COMMONS_REF:-master}"
RUN_TESTS="${COMMONS_RUN_TESTS:-0}"
KEEP_TMP="${COMMONS_KEEP_TMP:-0}"

# ── salida con colores si la consola lo soporta ────────────────────────────
if [[ -t 1 ]] && command -v tput >/dev/null 2>&1 && [[ "$(tput colors 2>/dev/null || echo 0)" -ge 8 ]]; then
    C_RESET="$(tput sgr0)"; C_BOLD="$(tput bold)"
    C_BLUE="$(tput setaf 4)"; C_GREEN="$(tput setaf 2)"
    C_YELLOW="$(tput setaf 3)"; C_RED="$(tput setaf 1)"; C_DIM="$(tput dim)"
else
    C_RESET=""; C_BOLD=""; C_BLUE=""; C_GREEN=""; C_YELLOW=""; C_RED=""; C_DIM=""
fi

log()   { printf "%s[commons]%s %s\n" "${C_BOLD}${C_BLUE}" "${C_RESET}" "$*" >&2; }
ok()    { printf "%s[commons]%s %s\n" "${C_BOLD}${C_GREEN}" "${C_RESET}" "$*" >&2; }
warn()  { printf "%s[commons]%s %s\n" "${C_BOLD}${C_YELLOW}" "${C_RESET}" "$*" >&2; }
err()   { printf "%s[commons]%s %s\n" "${C_BOLD}${C_RED}" "${C_RESET}" "$*" >&2; }
step()  { printf "\n%s━━ %s ━━%s\n" "${C_BOLD}${C_BLUE}" "$*" "${C_RESET}" >&2; }

# ── manejo de errores y limpieza ───────────────────────────────────────────
TMP_DIR=""
on_exit() {
    local rc=$?
    if [[ -n "${TMP_DIR}" && -d "${TMP_DIR}" ]]; then
        if [[ "${KEEP_TMP}" == "1" ]]; then
            warn "Conservando clon temporal en ${TMP_DIR} (COMMONS_KEEP_TMP=1)"
        else
            rm -rf -- "${TMP_DIR}"
        fi
    fi
    if (( rc != 0 )); then
        err "Instalación abortada con código ${rc}."
    fi
    exit "${rc}"
}
trap on_exit EXIT
trap 'err "Error en línea ${LINENO} (último comando: ${BASH_COMMAND})"' ERR

# ── precondiciones ─────────────────────────────────────────────────────────
require() {
    local cmd="$1"
    command -v "${cmd}" >/dev/null 2>&1 || { err "Falta dependencia: '${cmd}' no está en PATH"; exit 127; }
}

step "Verificando precondiciones"
require git
require java
JAVA_VERSION="$(java -version 2>&1 | head -n1)"
log "Java detectado: ${C_DIM}${JAVA_VERSION}${C_RESET}"

# Java 21+ requerido (maven.compiler.release=21)
JAVA_MAJOR="$(java -version 2>&1 | awk -F[\".] '/version/ {print $2; exit}')"
if [[ -n "${JAVA_MAJOR}" && "${JAVA_MAJOR}" -lt 21 ]]; then
    err "Se requiere JDK 21+, detectado JDK ${JAVA_MAJOR}."
    exit 1
fi

log "Repositorio:  ${C_BOLD}${REPO_URL}${C_RESET}"
log "Ref:          ${C_BOLD}${REF}${C_RESET}"
log "Tests:        $([[ "${RUN_TESTS}" == "1" ]] && echo on || echo off)"

# ── clone shallow en tmp ───────────────────────────────────────────────────
step "Clonando repositorio"
TMP_DIR="$(mktemp -d -t edutrack-ms-commons.XXXXXX)"
log "Directorio temporal: ${C_DIM}${TMP_DIR}${C_RESET}"

# --depth 1 + --branch acepta branches y tags; SHAs concretos requieren fetch
if ! git clone --depth 1 --branch "${REF}" --single-branch "${REPO_URL}" "${TMP_DIR}" 2>&1 | sed "s/^/${C_DIM}  git │${C_RESET} /"; then
    warn "Clone shallow por ref directa falló — probablemente '${REF}' es un SHA. Reintentando con clone completo."
    rm -rf -- "${TMP_DIR}"; TMP_DIR="$(mktemp -d -t edutrack-ms-commons.XXXXXX)"
    git clone "${REPO_URL}" "${TMP_DIR}" 2>&1 | sed "s/^/${C_DIM}  git │${C_RESET} /"
    git -C "${TMP_DIR}" checkout "${REF}" 2>&1 | sed "s/^/${C_DIM}  git │${C_RESET} /"
fi

HEAD_SHA="$(git -C "${TMP_DIR}" rev-parse HEAD)"
HEAD_SUBJECT="$(git -C "${TMP_DIR}" log -1 --pretty=%s)"
ok "Checkout en ${C_BOLD}${HEAD_SHA:0:12}${C_RESET} — ${C_DIM}${HEAD_SUBJECT}${C_RESET}"

# ── leer coordenadas Maven directamente del pom ────────────────────────────
extract_pom_field() {
    # extrae el primer <tag>valor</tag> del bloque <project>, evitando <parent>
    local tag="$1" pom="$2"
    # quitamos el bloque <parent>...</parent> para no confundirnos con su versión
    awk -v tag="${tag}" '
        /<parent>/ {skip=1}
        /<\/parent>/ {skip=0; next}
        !skip && match($0, "<"tag">[^<]*</"tag">") {
            s = substr($0, RSTART+length(tag)+2, RLENGTH-2*length(tag)-5)
            print s
            exit
        }
    ' "${pom}"
}

POM="${TMP_DIR}/pom.xml"
[[ -f "${POM}" ]] || { err "pom.xml no encontrado en el clon."; exit 1; }
GROUP_ID="$(extract_pom_field groupId "${POM}")"
ARTIFACT_ID="$(extract_pom_field artifactId "${POM}")"
VERSION="$(extract_pom_field version "${POM}")"
log "Coordenadas:  ${C_BOLD}${GROUP_ID}:${ARTIFACT_ID}:${VERSION}${C_RESET}"

# ── mvn clean install (siempre clean → sirve como updater) ─────────────────
step "Ejecutando ./mvnw clean install"
MVNW="${TMP_DIR}/mvnw"
chmod +x "${MVNW}" 2>/dev/null || true

MVN_ARGS=( -B -ntp clean install )
if [[ "${RUN_TESTS}" != "1" ]]; then
    MVN_ARGS+=( -DskipTests )
fi

# Pipeamos a sed para prefijar cada línea — pero preservamos el exit code de mvnw via PIPESTATUS
set +e
( cd "${TMP_DIR}" && "${MVNW}" "${MVN_ARGS[@]}" ) 2>&1 | sed "s/^/${C_DIM}  mvn │${C_RESET} /"
MVN_RC=${PIPESTATUS[0]}
set -e

if (( MVN_RC != 0 )); then
    err "Maven falló con código ${MVN_RC}."
    exit "${MVN_RC}"
fi

# ── verificar instalación en el repo local ─────────────────────────────────
M2_REPO="${M2_HOME:-${HOME}/.m2}/repository"
GROUP_PATH="${GROUP_ID//.//}"
INSTALLED_JAR="${M2_REPO}/${GROUP_PATH}/${ARTIFACT_ID}/${VERSION}/${ARTIFACT_ID}-${VERSION}.jar"

if [[ ! -f "${INSTALLED_JAR}" ]]; then
    err "El jar no aparece en el repo local Maven: ${INSTALLED_JAR}"
    exit 1
fi
JAR_SIZE="$(du -h "${INSTALLED_JAR}" | awk '{print $1}')"
ok "Instalado: ${C_BOLD}${INSTALLED_JAR}${C_RESET} (${JAR_SIZE})"

# ── snippet final ──────────────────────────────────────────────────────────
step "Listo"
ok "edutrack-ms-commons ${C_BOLD}${VERSION}${C_RESET} instalado desde ${HEAD_SHA:0:12}."

cat >&1 <<EOF

${C_BOLD}Pega esto en el <dependencies> de tu pom.xml:${C_RESET}

    <dependency>
        <groupId>${GROUP_ID}</groupId>
        <artifactId>${ARTIFACT_ID}</artifactId>
        <version>${VERSION}</version>
    </dependency>

EOF

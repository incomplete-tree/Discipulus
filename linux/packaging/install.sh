#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
app_id="dev.harrydekat.discipulus"
data_home="${XDG_DATA_HOME:-${HOME}/.local/share}"
bin_home="${XDG_BIN_HOME:-${HOME}/.local/bin}"
app_home="${data_home}/discipulus"

if [[ ! -x "${script_dir}/discipulus/discipulus" ]]; then
  printf 'Missing Linux bundle executable: %s\n' "${script_dir}/discipulus/discipulus" >&2
  exit 1
fi

mkdir -p "${app_home}" "${bin_home}" \
  "${data_home}/applications" \
  "${data_home}/metainfo" \
  "${data_home}/icons/hicolor/scalable/apps"

cp -a "${script_dir}/discipulus/." "${app_home}/"
ln -sfn "${app_home}/discipulus" "${bin_home}/discipulus"

cp "${script_dir}/${app_id}.appdata.xml" \
  "${data_home}/metainfo/${app_id}.appdata.xml"
cp "${script_dir}/icon.svg" \
  "${data_home}/icons/hicolor/scalable/apps/${app_id}.svg"

# Keep the desktop file relocatable for a user-local installation. KDE Plasma
# resolves the command through ~/.local/bin, so task-manager actions work too.
sed "s#^Exec=discipulus#Exec=${bin_home}/discipulus#g; \
  s#^Icon=${app_id}#Icon=${data_home}/icons/hicolor/scalable/apps/${app_id}.svg#" \
  "${script_dir}/${app_id}.desktop" \
  > "${data_home}/applications/${app_id}.desktop"

if command -v update-desktop-database >/dev/null 2>&1; then
  update-desktop-database "${data_home}/applications" >/dev/null 2>&1 || true
fi
if command -v kbuildsycoca6 >/dev/null 2>&1; then
  kbuildsycoca6 --noincremental >/dev/null 2>&1 || true
fi

cat <<EOF
Discipulus installed for this user.
If ${bin_home} is not on PATH, add it to your shell profile.
EOF

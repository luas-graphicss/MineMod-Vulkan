#!/bin/sh
set -eu

snapshot_archive="http://snapshot.debian.org/archive"
snapshot_time="20260824T000000Z"
package_list="$(dirname "$0")/packages.txt"

cat > /etc/apt/sources.list <<EOF
deb ${snapshot_archive}/debian/${snapshot_time} bullseye main
deb ${snapshot_archive}/debian-security/${snapshot_time} bullseye-security main
deb ${snapshot_archive}/debian/${snapshot_time} bullseye-updates main
EOF

export DEBIAN_FRONTEND=noninteractive
apt-get --option Acquire::Check-Valid-Until=false --option Acquire::Retries=5 update
xargs --arg-file "${package_list}" apt-get --option Acquire::Retries=5 install --yes --no-install-recommends
apt-get clean

#!/usr/bin/env bash
set -euo pipefail

project_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
version=${SHAREGUARD_VERSION:-0.0.1}
jar_path=${1:-"$project_root/target/shareguard-0.0.1-SNAPSHOT.jar"}
output_dir=${2:-"$project_root/dist"}

if [[ ! -f "$jar_path" ]]; then
    echo "ShareGuard JAR not found: $jar_path" >&2
    echo "Build it first with ./mvnw clean package" >&2
    exit 1
fi

package_root=$(mktemp -d)
trap 'rm -rf "$package_root"' EXIT

install -d \
    "$package_root/DEBIAN" \
    "$package_root/etc/shareguard" \
    "$package_root/usr/lib/shareguard" \
    "$package_root/usr/lib/systemd/system" \
    "$package_root/var/lib/shareguard"

# Do not let the caller's umask leak group-write permissions into the package.
chmod 0755 \
    "$package_root" \
    "$package_root/etc" \
    "$package_root/etc/shareguard" \
    "$package_root/usr" \
    "$package_root/usr/lib" \
    "$package_root/usr/lib/shareguard" \
    "$package_root/usr/lib/systemd" \
    "$package_root/usr/lib/systemd/system" \
    "$package_root/var" \
    "$package_root/var/lib" \
    "$package_root/var/lib/shareguard"

sed "s/@VERSION@/$version/g" "$project_root/packaging/deb/DEBIAN/control" > "$package_root/DEBIAN/control"
install -m 0644 "$project_root/packaging/deb/DEBIAN/conffiles" "$package_root/DEBIAN/conffiles"
install -m 0755 "$project_root/packaging/deb/DEBIAN/postinst" "$package_root/DEBIAN/postinst"
install -m 0755 "$project_root/packaging/deb/DEBIAN/prerm" "$package_root/DEBIAN/prerm"
install -m 0755 "$project_root/packaging/deb/DEBIAN/postrm" "$package_root/DEBIAN/postrm"
install -m 0600 "$project_root/packaging/deb/etc/shareguard/shareguard.env" "$package_root/etc/shareguard/shareguard.env"
install -m 0644 "$jar_path" "$package_root/usr/lib/shareguard/shareguard.jar"
install -m 0755 "$project_root/packaging/helper/shareguard_helper.py" "$package_root/usr/lib/shareguard/shareguard-helper"
install -m 0644 "$project_root/packaging/deb/systemd/shareguard-helper.service" "$package_root/usr/lib/systemd/system/shareguard-helper.service"
install -m 0644 "$project_root/packaging/deb/systemd/shareguard.service" "$package_root/usr/lib/systemd/system/shareguard.service"

mkdir -p "$output_dir"
dpkg-deb --root-owner-group --build "$package_root" "$output_dir/shareguard_${version}_all.deb"

from __future__ import annotations

import shutil
from collections.abc import Mapping
from pathlib import Path

from provider_bundle.digests import sha256_of
from provider_bundle.manifest import BundlePin

MANIFEST_FILE_NAME = "provider.properties"
LICENSE_FILE_NAME = "mesa-license.rst"
_DIRECTORY_MODE = 0o755
_FILE_MODE = 0o644


def render_properties(mesa_version: str, pin: BundlePin, digests: Mapping[str, str]) -> str:
    lines = [f"mesa={mesa_version}", f"entry={pin.entry}"]
    if pin.dependencies:
        lines.append("dependencies=" + " ".join(pin.dependencies))
    lines.extend(f"sha256.{library}={digests[library]}" for library in pin.libraries)
    lines.extend(f"driver.{name}={value}" for name, value in sorted(pin.driver_settings.items()))
    return "\n".join(lines) + "\n"


def stage_bundle(
    mesa_version: str, pin: BundlePin, install_prefix: Path, license_file: Path, bundle: Path
) -> dict[str, str]:
    if bundle.exists():
        shutil.rmtree(bundle)
    bundle.mkdir(parents=True)
    bundle.chmod(_DIRECTORY_MODE)

    for library in pin.libraries:
        _stage(install_prefix / pin.artifacts[library], bundle / library)
    _stage(license_file, bundle / LICENSE_FILE_NAME)

    digests = {library: sha256_of(bundle / library) for library in pin.libraries}
    described = bundle / MANIFEST_FILE_NAME
    described.write_bytes(render_properties(mesa_version, pin, digests).encode("ascii"))
    described.chmod(_FILE_MODE)
    return digests


def bundle_defects(mesa_version: str, pin: BundlePin, bundle: Path) -> list[str]:
    defects = []
    for library in pin.libraries:
        staged = bundle / library
        if staged.is_symlink() or not staged.is_file():
            defects.append(f"{library} is missing or is not a regular file")
            continue
        found = sha256_of(staged)
        if found != pin.digests[library]:
            defects.append(f"{library} has SHA-256 {found}, the manifest pins {pin.digests[library]}")

    expected = render_properties(mesa_version, pin, pin.digests).encode("ascii")
    described = bundle / MANIFEST_FILE_NAME
    if not described.is_file() or described.stat().st_size != len(expected) or described.read_bytes() != expected:
        defects.append(f"{MANIFEST_FILE_NAME} does not describe the pinned bundle")
    return defects


def _stage(source: Path, staged: Path) -> None:
    shutil.copyfile(source, staged)
    staged.chmod(_FILE_MODE)

from __future__ import annotations

import stat
from pathlib import Path

from provider_bundle.bundle import (
    LICENSE_FILE_NAME,
    MANIFEST_FILE_NAME,
    bundle_defects,
    render_properties,
    stage_bundle,
)
from provider_bundle.manifest import BundlePin
from sample_manifest import LIBRARY_CONTENTS

MESA_VERSION = "24.2.8"


def _mode_of(file: Path) -> int:
    return stat.S_IMODE(file.stat().st_mode)


def test_render_properties_follows_the_layout_the_client_reads(pin: BundlePin) -> None:
    assert render_properties(MESA_VERSION, pin, pin.digests) == (
        "mesa=24.2.8\n"
        "entry=libEGL.so.1\n"
        "dependencies=libglapi.so.0\n"
        f"sha256.libglapi.so.0={pin.digests['libglapi.so.0']}\n"
        f"sha256.libEGL.so.1={pin.digests['libEGL.so.1']}\n"
        "driver.GALLIUM_DRIVER=zink\n"
        "driver.MESA_LOADER_DRIVER_OVERRIDE=zink\n"
    )


def test_stage_bundle_copies_the_libraries_under_their_bundle_names(
    pin: BundlePin, install_prefix: Path, license_file: Path, tmp_path: Path
) -> None:
    bundle = tmp_path / "bundle"

    digests = stage_bundle(MESA_VERSION, pin, install_prefix, license_file, bundle)

    assert digests == dict(pin.digests)
    assert sorted(staged.name for staged in bundle.iterdir()) == sorted(
        [*LIBRARY_CONTENTS, LICENSE_FILE_NAME, MANIFEST_FILE_NAME]
    )
    for name, content in LIBRARY_CONTENTS.items():
        assert (bundle / name).read_bytes() == content
        assert _mode_of(bundle / name) == 0o644
    assert (bundle / MANIFEST_FILE_NAME).read_text(encoding="ascii") == render_properties(MESA_VERSION, pin, digests)


def test_stage_bundle_replaces_what_the_directory_held(
    pin: BundlePin, install_prefix: Path, license_file: Path, tmp_path: Path
) -> None:
    bundle = tmp_path / "bundle"
    bundle.mkdir()
    (bundle / "libstale.so").write_bytes(b"left by an earlier build")

    stage_bundle(MESA_VERSION, pin, install_prefix, license_file, bundle)

    assert not (bundle / "libstale.so").exists()


def test_bundle_defects_accepts_the_bundle_the_manifest_pins(
    pin: BundlePin, install_prefix: Path, license_file: Path, tmp_path: Path
) -> None:
    bundle = tmp_path / "bundle"
    stage_bundle(MESA_VERSION, pin, install_prefix, license_file, bundle)

    assert bundle_defects(MESA_VERSION, pin, bundle) == []


def test_bundle_defects_names_a_library_whose_digest_differs(
    pin: BundlePin, install_prefix: Path, license_file: Path, tmp_path: Path
) -> None:
    bundle = tmp_path / "bundle"
    stage_bundle(MESA_VERSION, pin, install_prefix, license_file, bundle)
    (bundle / "libEGL.so.1").write_bytes(b"another build")

    defects = bundle_defects(MESA_VERSION, pin, bundle)

    assert len(defects) == 1
    assert defects[0].startswith("libEGL.so.1 has SHA-256 ")
    assert defects[0].endswith(f"the manifest pins {pin.digests['libEGL.so.1']}")


def test_bundle_defects_refuses_a_symbolic_link_in_place_of_a_library(
    pin: BundlePin, install_prefix: Path, license_file: Path, tmp_path: Path
) -> None:
    bundle = tmp_path / "bundle"
    stage_bundle(MESA_VERSION, pin, install_prefix, license_file, bundle)
    (bundle / "libglapi.so.0").unlink()
    (bundle / "libglapi.so.0").symlink_to(install_prefix / "lib" / "libglapi.so.0.0.0")

    assert bundle_defects(MESA_VERSION, pin, bundle) == ["libglapi.so.0 is missing or is not a regular file"]


def test_bundle_defects_notices_a_description_of_another_bundle(
    pin: BundlePin, install_prefix: Path, license_file: Path, tmp_path: Path
) -> None:
    bundle = tmp_path / "bundle"
    stage_bundle(MESA_VERSION, pin, install_prefix, license_file, bundle)
    described = bundle / MANIFEST_FILE_NAME
    described.write_text(described.read_text(encoding="ascii") + "driver.ZINK_DEBUG=validation\n", encoding="ascii")

    assert bundle_defects(MESA_VERSION, pin, bundle) == [f"{MANIFEST_FILE_NAME} does not describe the pinned bundle"]


def test_bundle_defects_reports_every_library_of_an_empty_directory(pin: BundlePin, tmp_path: Path) -> None:
    assert bundle_defects(MESA_VERSION, pin, tmp_path) == [
        "libglapi.so.0 is missing or is not a regular file",
        "libEGL.so.1 is missing or is not a regular file",
        f"{MANIFEST_FILE_NAME} does not describe the pinned bundle",
    ]

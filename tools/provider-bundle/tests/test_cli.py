from __future__ import annotations

import logging
from pathlib import Path

import pytest

from provider_bundle.bundle import stage_bundle
from provider_bundle.cli import main
from provider_bundle.manifest import ProviderManifest
from sample_manifest import PLATFORM, Document, ManifestWriter


@pytest.fixture
def manifest_file(document: Document, write_manifest: ManifestWriter) -> Path:
    return write_manifest(document)


@pytest.fixture
def bundle(manifest: ProviderManifest, install_prefix: Path, license_file: Path, tmp_path: Path) -> Path:
    staged = tmp_path / "bundle"
    stage_bundle(manifest.mesa_version, manifest.bundle(PLATFORM), install_prefix, license_file, staged)
    return staged


def _verify(manifest_file: Path, platform: str, bundle: Path) -> int:
    return main(["--manifest", str(manifest_file), "verify", "--platform", platform, "--bundle", str(bundle)])


def test_verify_accepts_the_bundle_the_manifest_pins(
    manifest_file: Path, bundle: Path, caplog: pytest.LogCaptureFixture
) -> None:
    caplog.set_level(logging.INFO)

    assert _verify(manifest_file, PLATFORM, bundle) == 0
    assert f"holds the pinned Mesa 24.2.8 bundle for {PLATFORM}" in caplog.text


def test_verify_fails_and_names_the_library_that_differs(
    manifest_file: Path, bundle: Path, caplog: pytest.LogCaptureFixture
) -> None:
    (bundle / "libEGL.so.1").write_bytes(b"another build")

    assert _verify(manifest_file, PLATFORM, bundle) == 1
    assert f"{PLATFORM}: libEGL.so.1 has SHA-256 " in caplog.text


def test_verify_fails_for_a_platform_the_manifest_does_not_pin(
    manifest_file: Path, bundle: Path, caplog: pytest.LogCaptureFixture
) -> None:
    assert _verify(manifest_file, "macos-arm64", bundle) == 1
    assert "the manifest pins no bundle for macos-arm64" in caplog.text


def test_a_malformed_manifest_fails_with_its_reason(
    document: Document, write_manifest: ManifestWriter, bundle: Path, caplog: pytest.LogCaptureFixture
) -> None:
    document["meson"]["sha256"] = "unpinned"

    assert _verify(write_manifest(document), PLATFORM, bundle) == 1
    assert "manifest.meson.sha256 is missing or malformed" in caplog.text


def test_a_missing_manifest_fails_with_its_reason(tmp_path: Path, caplog: pytest.LogCaptureFixture) -> None:
    assert _verify(tmp_path / "absent.json", PLATFORM, tmp_path) == 1
    assert "absent.json" in caplog.text

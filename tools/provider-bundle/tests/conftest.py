from __future__ import annotations

import json
from pathlib import Path

import pytest

from provider_bundle.manifest import BundlePin, ProviderManifest, load_manifest
from sample_manifest import INSTALLED_AS, LIBRARY_CONTENTS, PLATFORM, Document, ManifestWriter, sample_document


@pytest.fixture
def document() -> Document:
    return sample_document()


@pytest.fixture
def write_manifest(tmp_path: Path) -> ManifestWriter:
    def write(written: Document) -> Path:
        file = tmp_path / "opengl-provider.json"
        file.write_text(json.dumps(written), encoding="utf-8")
        return file

    return write


@pytest.fixture
def manifest(document: Document, write_manifest: ManifestWriter) -> ProviderManifest:
    return load_manifest(write_manifest(document))


@pytest.fixture
def pin(manifest: ProviderManifest) -> BundlePin:
    return manifest.bundle(PLATFORM)


@pytest.fixture
def install_prefix(tmp_path: Path) -> Path:
    prefix = tmp_path / "prefix"
    for name, content in LIBRARY_CONTENTS.items():
        installed = prefix / INSTALLED_AS[name]
        installed.parent.mkdir(parents=True, exist_ok=True)
        installed.write_bytes(content)
    return prefix


@pytest.fixture
def license_file(tmp_path: Path) -> Path:
    file = tmp_path / "license.rst"
    file.write_text("MIT\n", encoding="utf-8")
    return file

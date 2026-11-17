from __future__ import annotations

from pathlib import Path

import pytest

from provider_bundle.manifest import MAX_MANIFEST_BYTES, ManifestError, ProviderManifest, load_manifest
from sample_manifest import PLATFORM, Document, ManifestWriter

REPOSITORY_MANIFEST = Path(__file__).resolve().parents[3] / "manifests" / "opengl-provider.json"
UNPINNED_DIGEST = "0" * 64


def test_load_manifest_reads_the_pin_of_the_repository() -> None:
    manifest = load_manifest(REPOSITORY_MANIFEST)

    assert manifest.mesa_version == "24.2.8"
    assert sorted(manifest.bundles) == ["linux-x86_64", "windows-x86_64"]
    assert "-Dgallium-drivers=['zink']" in manifest.meson_arguments
    for pin in manifest.bundles.values():
        assert sorted(pin.digests) == sorted(pin.libraries)
        assert UNPINNED_DIGEST not in pin.digests.values()
        assert pin.driver_settings["GALLIUM_DRIVER"] == "zink"


def test_load_manifest_turns_options_into_meson_arguments(manifest: ProviderManifest) -> None:
    assert manifest.meson_arguments == ("-Dbuildtype=release", "-Dgallium-drivers=['zink']")
    assert manifest.static_libraries["libdrm"].meson_arguments == ("-Ddefault_library=static",)
    assert manifest.bundle(PLATFORM).meson_arguments == ("-Dplatforms=[]",)


def test_load_manifest_keeps_the_entry_after_its_dependencies(manifest: ProviderManifest) -> None:
    pin = manifest.bundle(PLATFORM)

    assert pin.libraries == ("libglapi.so.0", "libEGL.so.1")
    assert pin.static_libraries == ("libdrm",)
    assert pin.cross_file is None


def test_bundle_names_the_pinned_platforms_when_one_is_unknown(manifest: ProviderManifest) -> None:
    with pytest.raises(ManifestError, match="pins no bundle for macos-arm64; it pins linux-x86_64"):
        manifest.bundle("macos-arm64")


def test_load_manifest_rejects_an_entry_it_does_not_read(document: Document, write_manifest: ManifestWriter) -> None:
    document["bundles"][PLATFORM]["postInstall"] = "strip"

    with pytest.raises(ManifestError, match="holds postInstall, which this tool does not read"):
        load_manifest(write_manifest(document))


def test_load_manifest_rejects_another_schema_version(document: Document, write_manifest: ManifestWriter) -> None:
    document["schemaVersion"] = 2

    with pytest.raises(ManifestError, match="is not written in schema version 1"):
        load_manifest(write_manifest(document))


@pytest.mark.parametrize(
    ("section", "key", "value"),
    [
        ("mesa", "version", "24.2.8; rm"),
        ("meson", "sha256", "A" * 64),
        ("meson", "sha256", "a" * 63),
        ("meson", "url", "http://archive.example.org/meson-1.5.2.tar.gz"),
        ("meson", "url", "https://archive.example.org/meson.tar.gz?mirror=1"),
        ("mesonOptions", "buildtype", "release --wrap-mode=default"),
        ("mesonOptions", "gallium-drivers", ["zink'], 'other"]),
        ("mesonOptions", "strip", True),
    ],
)
def test_load_manifest_rejects_a_malformed_value(
    document: Document, write_manifest: ManifestWriter, section: str, key: str, value: object
) -> None:
    document[section][key] = value

    with pytest.raises(ManifestError, match="is missing or malformed"):
        load_manifest(write_manifest(document))


def test_load_manifest_rejects_an_artifact_above_the_install_prefix(
    document: Document, write_manifest: ManifestWriter
) -> None:
    document["bundles"][PLATFORM]["artifacts"]["libEGL.so.1"] = "lib/../../etc/libEGL.so.1"

    with pytest.raises(ManifestError, match="artifacts must stay below its root"):
        load_manifest(write_manifest(document))


def test_load_manifest_rejects_a_library_without_a_digest(document: Document, write_manifest: ManifestWriter) -> None:
    del document["bundles"][PLATFORM]["sha256"]["libglapi.so.0"]

    with pytest.raises(ManifestError, match="sha256 must describe exactly the libraries of the bundle"):
        load_manifest(write_manifest(document))


def test_load_manifest_rejects_a_library_listed_twice(document: Document, write_manifest: ManifestWriter) -> None:
    document["bundles"][PLATFORM]["dependencies"] = ["libEGL.so.1"]

    with pytest.raises(ManifestError, match="lists a library twice"):
        load_manifest(write_manifest(document))


def test_load_manifest_rejects_a_static_library_it_does_not_pin(
    document: Document, write_manifest: ManifestWriter
) -> None:
    document["bundles"][PLATFORM]["staticLibraries"] = ["zlib"]

    with pytest.raises(ManifestError, match="names zlib, which the manifest does not pin"):
        load_manifest(write_manifest(document))


def test_load_manifest_rejects_a_file_larger_than_its_limit(tmp_path: Path) -> None:
    file = tmp_path / "opengl-provider.json"
    file.write_bytes(b" " * (MAX_MANIFEST_BYTES + 1))

    with pytest.raises(ManifestError, match="is larger than"):
        load_manifest(file)


def test_load_manifest_rejects_a_file_that_is_not_json(tmp_path: Path) -> None:
    file = tmp_path / "opengl-provider.json"
    file.write_text("mesa=24.2.8\n", encoding="utf-8")

    with pytest.raises(ManifestError, match="is not a JSON document"):
        load_manifest(file)

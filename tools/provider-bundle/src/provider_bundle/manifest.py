from __future__ import annotations

import json
import re
from collections.abc import Mapping
from dataclasses import dataclass
from pathlib import Path, PurePosixPath
from urllib.parse import urlsplit

SCHEMA_VERSION = 1
MAX_MANIFEST_BYTES = 256 * 1024

_SHA256 = re.compile(r"[0-9a-f]{64}")
_VERSION = re.compile(r"[0-9]+(\.[0-9]+)+")
_HTTPS_URL = re.compile(r"https://[A-Za-z0-9.-]+/[A-Za-z0-9_./+-]+")
_PLATFORM = re.compile(r"[a-z0-9]+-[a-z0-9_]+")
_LIBRARY_NAME = re.compile(r"[A-Za-z0-9_][A-Za-z0-9_.+-]*")
_STATIC_LIBRARY_NAME = re.compile(r"[a-z][a-z0-9_-]*")
_SETTING_NAME = re.compile(r"[A-Z][A-Z0-9_]*")
_SETTING_VALUE = re.compile(r"[A-Za-z0-9_.,:/+-]*")
_OPTION_NAME = re.compile(r"[a-z][a-z0-9_.-]*")
_OPTION_VALUE = re.compile(r"[A-Za-z0-9_.,:/+=-]*")
_RELATIVE_PATH = re.compile(r"[A-Za-z0-9_][A-Za-z0-9_.+/-]*")


class ManifestError(ValueError):
    pass


@dataclass(frozen=True)
class PinnedSource:
    url: str
    sha256: str

    @property
    def file_name(self) -> str:
        return PurePosixPath(urlsplit(self.url).path).name


@dataclass(frozen=True)
class StaticLibrary:
    source: PinnedSource
    meson_arguments: tuple[str, ...]


@dataclass(frozen=True)
class BundlePin:
    platform: str
    entry: str
    dependencies: tuple[str, ...]
    driver_settings: Mapping[str, str]
    artifacts: Mapping[str, str]
    digests: Mapping[str, str]
    static_libraries: tuple[str, ...]
    meson_arguments: tuple[str, ...]
    cross_file: str | None

    @property
    def libraries(self) -> tuple[str, ...]:
        return (*self.dependencies, self.entry)


@dataclass(frozen=True)
class ProviderManifest:
    mesa_version: str
    mesa_source: PinnedSource
    meson_source: PinnedSource
    source_date_epoch: int
    meson_arguments: tuple[str, ...]
    static_libraries: Mapping[str, StaticLibrary]
    bundles: Mapping[str, BundlePin]

    def bundle(self, platform: str) -> BundlePin:
        pin = self.bundles.get(platform)
        if pin is None:
            pinned = ", ".join(sorted(self.bundles))
            raise ManifestError(f"the manifest pins no bundle for {platform}; it pins {pinned}")
        return pin


class _Node:
    def __init__(self, entries: object, where: str) -> None:
        if not isinstance(entries, dict):
            raise ManifestError(f"{where} must be an object")
        self._entries = dict(entries)
        self._where = where

    def text(self, key: str, pattern: re.Pattern[str]) -> str:
        return _matching(self._entries.pop(key, None), pattern, f"{self._where}.{key}")

    def optional_text(self, key: str, pattern: re.Pattern[str]) -> str | None:
        if key not in self._entries:
            return None
        return self.text(key, pattern)

    def integer(self, key: str) -> int:
        value = self._entries.pop(key, None)
        if not isinstance(value, int) or isinstance(value, bool) or value < 0:
            raise ManifestError(f"{self._where}.{key} must be a non-negative integer")
        return value

    def texts(self, key: str, pattern: re.Pattern[str]) -> tuple[str, ...]:
        where = f"{self._where}.{key}"
        values = self._entries.pop(key, [])
        if not isinstance(values, list):
            raise ManifestError(f"{where} must be a list")
        return tuple(_matching(value, pattern, where) for value in values)

    def node(self, key: str) -> _Node:
        return _Node(self._entries.pop(key, None), f"{self._where}.{key}")

    def children(self, key: str, name_pattern: re.Pattern[str]) -> dict[str, _Node]:
        where = f"{self._where}.{key}"
        return {
            _matching(name, name_pattern, where): _Node(child, f"{where}.{name}")
            for name, child in self._object(key).items()
        }

    def mapping(self, key: str, name_pattern: re.Pattern[str], value_pattern: re.Pattern[str]) -> dict[str, str]:
        where = f"{self._where}.{key}"
        return {
            _matching(name, name_pattern, where): _matching(value, value_pattern, f"{where}.{name}")
            for name, value in self._object(key).items()
        }

    def meson_arguments(self, key: str) -> tuple[str, ...]:
        where = f"{self._where}.{key}"
        return tuple(
            f"-D{_matching(name, _OPTION_NAME, where)}={_meson_value(value, f'{where}.{name}')}"
            for name, value in self._object(key).items()
        )

    def finish(self) -> None:
        if self._entries:
            unread = ", ".join(sorted(self._entries))
            raise ManifestError(f"{self._where} holds {unread}, which this tool does not read")

    def _object(self, key: str) -> dict[object, object]:
        entries = self._entries.pop(key, {})
        if not isinstance(entries, dict):
            raise ManifestError(f"{self._where}.{key} must be an object")
        return entries


def _matching(value: object, pattern: re.Pattern[str], where: str) -> str:
    if not isinstance(value, str) or pattern.fullmatch(value) is None:
        raise ManifestError(f"{where} is missing or malformed")
    return value


def _meson_value(value: object, where: str) -> str:
    if isinstance(value, list):
        return "[" + ",".join(f"'{_matching(item, _OPTION_VALUE, where)}'" for item in value) + "]"
    return _matching(value, _OPTION_VALUE, where)


def _below_root(path: str, where: str) -> str:
    if ".." in PurePosixPath(path).parts:
        raise ManifestError(f"{where} must stay below its root")
    return path


def _source(node: _Node) -> PinnedSource:
    source = PinnedSource(node.text("url", _HTTPS_URL), node.text("sha256", _SHA256))
    node.finish()
    return source


def _static_library(node: _Node) -> StaticLibrary:
    library = StaticLibrary(_source(node.node("source")), node.meson_arguments("mesonOptions"))
    node.finish()
    return library


def _bundle(platform: str, node: _Node, static_libraries: Mapping[str, StaticLibrary]) -> BundlePin:
    where = f"bundles.{platform}"
    entry = node.text("entry", _LIBRARY_NAME)
    dependencies = node.texts("dependencies", _LIBRARY_NAME)
    libraries = (*dependencies, entry)
    if len(set(libraries)) != len(libraries):
        raise ManifestError(f"{where} lists a library twice")

    artifacts = node.mapping("artifacts", _LIBRARY_NAME, _RELATIVE_PATH)
    digests = node.mapping("sha256", _LIBRARY_NAME, _SHA256)
    for key, described in (("artifacts", artifacts), ("sha256", digests)):
        if sorted(described) != sorted(libraries):
            raise ManifestError(f"{where}.{key} must describe exactly the libraries of the bundle")

    linked = node.texts("staticLibraries", _STATIC_LIBRARY_NAME)
    unpinned = sorted(set(linked) - set(static_libraries))
    if unpinned:
        raise ManifestError(f"{where}.staticLibraries names {', '.join(unpinned)}, which the manifest does not pin")

    cross_file = node.optional_text("crossFile", _RELATIVE_PATH)
    pin = BundlePin(
        platform=platform,
        entry=entry,
        dependencies=dependencies,
        driver_settings=node.mapping("driverSettings", _SETTING_NAME, _SETTING_VALUE),
        artifacts={library: _below_root(path, f"{where}.artifacts") for library, path in artifacts.items()},
        digests=digests,
        static_libraries=linked,
        meson_arguments=node.meson_arguments("mesonOptions"),
        cross_file=None if cross_file is None else _below_root(cross_file, f"{where}.crossFile"),
    )
    node.finish()
    return pin


def load_manifest(file: Path) -> ProviderManifest:
    if file.stat().st_size > MAX_MANIFEST_BYTES:
        raise ManifestError(f"{file} is larger than {MAX_MANIFEST_BYTES} bytes")
    try:
        document = json.loads(file.read_text(encoding="utf-8"))
    except ValueError as failure:
        raise ManifestError(f"{file} is not a JSON document") from failure

    root = _Node(document, "manifest")
    if root.integer("schemaVersion") != SCHEMA_VERSION:
        raise ManifestError(f"{file} is not written in schema version {SCHEMA_VERSION}")

    mesa = root.node("mesa")
    mesa_version = mesa.text("version", _VERSION)
    mesa_source = _source(mesa.node("source"))
    mesa.finish()

    static_libraries = {
        name: _static_library(node) for name, node in root.children("staticLibraries", _STATIC_LIBRARY_NAME).items()
    }
    manifest = ProviderManifest(
        mesa_version=mesa_version,
        mesa_source=mesa_source,
        meson_source=_source(root.node("meson")),
        source_date_epoch=root.integer("sourceDateEpoch"),
        meson_arguments=root.meson_arguments("mesonOptions"),
        static_libraries=static_libraries,
        bundles={
            platform: _bundle(platform, node, static_libraries)
            for platform, node in root.children("bundles", _PLATFORM).items()
        },
    )
    root.finish()
    return manifest

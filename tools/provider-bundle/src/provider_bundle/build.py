from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

from provider_bundle.bundle import stage_bundle
from provider_bundle.environment import reproducible_environment
from provider_bundle.manifest import ProviderManifest
from provider_bundle.meson import MesonProject, MesonRunner
from provider_bundle.sources import extract, fetch

_MESA = "mesa"
_MESA_LICENSE = Path("docs") / "license.rst"


@dataclass(frozen=True)
class BuildTree:
    root: Path
    platform: str

    @property
    def downloads(self) -> Path:
        return self.root / "downloads"

    @property
    def sources(self) -> Path:
        return self.root / "sources"

    @property
    def home(self) -> Path:
        return self.root / "home"

    def build_of(self, project: str) -> Path:
        return self.root / self.platform / "build" / project

    def prefix_of(self, project: str) -> Path:
        return self.root / self.platform / "prefix" / project


def build_bundle(
    manifest: ProviderManifest, platform: str, tool_root: Path, work: Path, bundle: Path
) -> dict[str, str]:
    pin = manifest.bundle(platform)
    tree = BuildTree(work, platform)
    tree.home.mkdir(parents=True, exist_ok=True)
    meson_script = extract(fetch(manifest.meson_source, tree.downloads), tree.sources) / "meson.py"

    pkg_config_paths = []
    for name in pin.static_libraries:
        library = manifest.static_libraries[name]
        MesonRunner(meson_script, reproducible_environment(manifest.source_date_epoch, tree.home, [])).install(
            MesonProject(
                source=extract(fetch(library.source, tree.downloads), tree.sources),
                build=tree.build_of(name),
                prefix=tree.prefix_of(name),
                arguments=library.meson_arguments,
            )
        )
        pkg_config_paths.append(tree.prefix_of(name) / "lib" / "pkgconfig")

    mesa_source = extract(fetch(manifest.mesa_source, tree.downloads), tree.sources)
    MesonRunner(
        meson_script, reproducible_environment(manifest.source_date_epoch, tree.home, pkg_config_paths)
    ).install(
        MesonProject(
            source=mesa_source,
            build=tree.build_of(_MESA),
            prefix=tree.prefix_of(_MESA),
            arguments=(*manifest.meson_arguments, *pin.meson_arguments),
            cross_file=None if pin.cross_file is None else tool_root / pin.cross_file,
        )
    )
    return stage_bundle(manifest.mesa_version, pin, tree.prefix_of(_MESA), mesa_source / _MESA_LICENSE, bundle)

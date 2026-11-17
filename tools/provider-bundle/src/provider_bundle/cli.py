from __future__ import annotations

import argparse
import logging
import subprocess
from collections.abc import Sequence
from pathlib import Path

from provider_bundle.build import build_bundle
from provider_bundle.bundle import bundle_defects
from provider_bundle.manifest import ManifestError, ProviderManifest, load_manifest
from provider_bundle.sources import SourceError

_TOOL_ROOT = Path(__file__).resolve().parents[2]
_DEFAULT_MANIFEST = _TOOL_ROOT.parents[1] / "manifests" / "opengl-provider.json"
_DEFAULT_WORK = Path("/build")

logger = logging.getLogger(__name__)


def main(arguments: Sequence[str] | None = None) -> int:
    options = _parser().parse_args(arguments)
    logging.basicConfig(level=logging.INFO, format="%(message)s")
    try:
        manifest = load_manifest(options.manifest)
        if options.command == "build":
            return _build(manifest, options)
        return _verify(manifest, options)
    except (ManifestError, SourceError, subprocess.CalledProcessError, OSError) as failure:
        logger.error("%s", failure)
        return 1


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="provider_bundle", description="Builds and verifies the pinned Mesa/Zink bundle of the OpenGL provider."
    )
    parser.add_argument("--manifest", type=Path, default=_DEFAULT_MANIFEST, help="the pin of the provider")
    commands = parser.add_subparsers(dest="command", required=True)

    build = commands.add_parser("build", help="build the pinned Mesa and stage its bundle")
    build.add_argument("--platform", required=True, help="a bundle of the manifest, such as linux-x86_64")
    build.add_argument("--work-dir", type=Path, default=_DEFAULT_WORK, help="where sources are unpacked and built")
    build.add_argument("--bundle", type=Path, required=True, help="the directory that receives the bundle")

    verify = commands.add_parser("verify", help="compare a staged bundle with the digests the manifest pins")
    verify.add_argument("--platform", required=True, help="a bundle of the manifest, such as linux-x86_64")
    verify.add_argument("--bundle", type=Path, required=True, help="the directory that holds the bundle")
    return parser


def _build(manifest: ProviderManifest, options: argparse.Namespace) -> int:
    digests = build_bundle(manifest, options.platform, _TOOL_ROOT, options.work_dir, options.bundle)
    for library, digest in digests.items():
        logger.info("sha256 %s %s", digest, library)
    return 0


def _verify(manifest: ProviderManifest, options: argparse.Namespace) -> int:
    defects = bundle_defects(manifest.mesa_version, manifest.bundle(options.platform), options.bundle)
    for defect in defects:
        logger.error("%s: %s", options.platform, defect)
    if defects:
        return 1
    logger.info("%s holds the pinned Mesa %s bundle for %s", options.bundle, manifest.mesa_version, options.platform)
    return 0

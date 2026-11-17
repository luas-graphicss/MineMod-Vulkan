from __future__ import annotations

import logging
import shutil
import tarfile
import urllib.request
from pathlib import Path, PurePosixPath

from provider_bundle.digests import sha256_of
from provider_bundle.manifest import PinnedSource

MAX_SOURCE_BYTES = 256 * 1024 * 1024
MAX_ARCHIVE_MEMBERS = 100_000
_CHUNK_BYTES = 1024 * 1024
_TIMEOUT_SECONDS = 120

logger = logging.getLogger(__name__)


class SourceError(RuntimeError):
    pass


def fetch(source: PinnedSource, downloads: Path) -> Path:
    archive = downloads / source.file_name
    if archive.is_file() and sha256_of(archive) == source.sha256:
        return archive

    downloads.mkdir(parents=True, exist_ok=True)
    partial = archive.with_name(archive.name + ".partial")
    logger.info("downloading %s", source.url)
    try:
        _download(source.url, partial)
        found = sha256_of(partial)
        if found != source.sha256:
            raise SourceError(f"{source.url} has SHA-256 {found}, the manifest pins {source.sha256}")
        partial.replace(archive)
    finally:
        partial.unlink(missing_ok=True)
    return archive


def extract(archive: Path, destination: Path) -> Path:
    with tarfile.open(archive) as packed:
        members = packed.getmembers()
        extracted = destination / _single_root(archive, members)
        if extracted.exists():
            shutil.rmtree(extracted)
        destination.mkdir(parents=True, exist_ok=True)
        if hasattr(tarfile, "data_filter"):
            packed.extractall(destination, members=members, filter="data")
        else:
            packed.extractall(destination, members=members)
    return extracted


def _download(url: str, destination: Path) -> None:
    written = 0
    with urllib.request.urlopen(url, timeout=_TIMEOUT_SECONDS) as response, destination.open("wb") as output:
        while chunk := response.read(_CHUNK_BYTES):
            written += len(chunk)
            if written > MAX_SOURCE_BYTES:
                raise SourceError(f"{url} is larger than {MAX_SOURCE_BYTES} bytes")
            output.write(chunk)


def _single_root(archive: Path, members: list[tarfile.TarInfo]) -> str:
    if len(members) > MAX_ARCHIVE_MEMBERS:
        raise SourceError(f"{archive.name} holds more than {MAX_ARCHIVE_MEMBERS} members")

    roots = set()
    for member in members:
        path = PurePosixPath(member.name)
        if path.is_absolute() or not path.parts or ".." in path.parts:
            raise SourceError(f"{archive.name} holds {member.name}, which leaves the archive")
        if member.issym():
            if not _stays_inside(path, PurePosixPath(member.linkname)):
                raise SourceError(f"{archive.name} links {member.name} to {member.linkname}, outside the archive")
        elif not (member.isfile() or member.isdir()):
            raise SourceError(f"{archive.name} holds {member.name}, which is no file, directory or symbolic link")
        roots.add(path.parts[0])

    if len(roots) != 1:
        raise SourceError(f"{archive.name} must hold exactly one top-level directory")
    return roots.pop()


def _stays_inside(link: PurePosixPath, target: PurePosixPath) -> bool:
    if target.is_absolute():
        return False
    depth = len(link.parent.parts)
    for part in target.parts:
        depth += -1 if part == ".." else 1
        if depth < 1:
            return False
    return True

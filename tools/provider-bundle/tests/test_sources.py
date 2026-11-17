from __future__ import annotations

import hashlib
import io
import tarfile
import urllib.request
from collections.abc import Sequence
from pathlib import Path

import pytest

from provider_bundle import sources
from provider_bundle.manifest import PinnedSource
from provider_bundle.sources import SourceError, extract, fetch

Member = tuple[tarfile.TarInfo, bytes]

ARCHIVE_NAME = "source-1.0.tar.gz"
ARCHIVE_URL = f"https://archive.example.org/{ARCHIVE_NAME}"
ARCHIVE_BYTES = b"an archive the manifest pins"


def _file(name: str, content: bytes = b"content") -> Member:
    member = tarfile.TarInfo(name)
    member.size = len(content)
    return member, content


def _special(name: str, kind: bytes, target: str = "") -> Member:
    member = tarfile.TarInfo(name)
    member.type = kind
    member.linkname = target
    return member, b""


def _pack(directory: Path, members: Sequence[Member]) -> Path:
    archive = directory / ARCHIVE_NAME
    with tarfile.open(archive, "w:gz") as packed:
        for member, content in members:
            packed.addfile(member, io.BytesIO(content))
    return archive


def _pinned(content: bytes) -> PinnedSource:
    return PinnedSource(ARCHIVE_URL, hashlib.sha256(content).hexdigest())


def _serve(monkeypatch: pytest.MonkeyPatch, content: bytes) -> list[str]:
    requested: list[str] = []

    def answer(url: str, timeout: float) -> io.BytesIO:
        requested.append(url)
        return io.BytesIO(content)

    monkeypatch.setattr(urllib.request, "urlopen", answer)
    return requested


def test_extract_unpacks_the_single_root_of_an_archive(tmp_path: Path) -> None:
    archive = _pack(
        tmp_path,
        [
            _file("source-1.0/meson.build", b"project('source')"),
            _special("source-1.0/latest", tarfile.SYMTYPE, "meson.build"),
        ],
    )

    extracted = extract(archive, tmp_path / "sources")

    assert extracted == tmp_path / "sources" / "source-1.0"
    assert (extracted / "meson.build").read_bytes() == b"project('source')"
    assert (extracted / "latest").read_bytes() == b"project('source')"


def test_extract_replaces_a_tree_an_earlier_build_changed(tmp_path: Path) -> None:
    archive = _pack(tmp_path, [_file("source-1.0/meson.build")])
    stale = tmp_path / "sources" / "source-1.0" / "generated.h"
    stale.parent.mkdir(parents=True)
    stale.write_bytes(b"left by an earlier build")

    extract(archive, tmp_path / "sources")

    assert not stale.exists()


@pytest.mark.parametrize(
    ("members", "reason"),
    [
        ([_file("/etc/passwd")], "which leaves the archive"),
        ([_file("source-1.0/../escaped")], "which leaves the archive"),
        ([_file("source-1.0/kept"), _file("other-1.0/added")], "exactly one top-level directory"),
        ([_special("source-1.0/link", tarfile.SYMTYPE, "../../outside")], "outside the archive"),
        ([_special("source-1.0/link", tarfile.SYMTYPE, "/etc/passwd")], "outside the archive"),
        ([_special("source-1.0/link", tarfile.LNKTYPE, "source-1.0/kept")], "no file, directory or symbolic link"),
        ([_special("source-1.0/pipe", tarfile.FIFOTYPE)], "no file, directory or symbolic link"),
    ],
)
def test_extract_refuses_an_archive_that_reaches_outside_its_root(
    tmp_path: Path, members: Sequence[Member], reason: str
) -> None:
    archive = _pack(tmp_path, members)

    with pytest.raises(SourceError, match=reason):
        extract(archive, tmp_path / "sources")

    assert not (tmp_path / "sources").exists()


def test_extract_refuses_more_members_than_its_limit(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setattr(sources, "MAX_ARCHIVE_MEMBERS", 1)
    archive = _pack(tmp_path, [_file("source-1.0/first"), _file("source-1.0/second")])

    with pytest.raises(SourceError, match="holds more than 1 members"):
        extract(archive, tmp_path / "sources")


def test_fetch_reuses_an_archive_that_matches_its_pin(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    requested = _serve(monkeypatch, b"what the network answers now")
    (tmp_path / ARCHIVE_NAME).write_bytes(ARCHIVE_BYTES)

    archive = fetch(_pinned(ARCHIVE_BYTES), tmp_path)

    assert archive.read_bytes() == ARCHIVE_BYTES
    assert requested == []


def test_fetch_stores_a_download_that_matches_its_pin(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    requested = _serve(monkeypatch, ARCHIVE_BYTES)

    archive = fetch(_pinned(ARCHIVE_BYTES), tmp_path / "downloads")

    assert archive == tmp_path / "downloads" / ARCHIVE_NAME
    assert archive.read_bytes() == ARCHIVE_BYTES
    assert requested == [ARCHIVE_URL]


def test_fetch_replaces_an_archive_that_no_longer_matches_its_pin(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    _serve(monkeypatch, ARCHIVE_BYTES)
    (tmp_path / ARCHIVE_NAME).write_bytes(b"truncated by an interrupted build")

    archive = fetch(_pinned(ARCHIVE_BYTES), tmp_path)

    assert archive.read_bytes() == ARCHIVE_BYTES


def test_fetch_refuses_a_download_whose_digest_differs(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    _serve(monkeypatch, b"what a compromised mirror answers")

    with pytest.raises(SourceError, match="the manifest pins"):
        fetch(_pinned(ARCHIVE_BYTES), tmp_path)

    assert list(tmp_path.iterdir()) == []


def test_fetch_refuses_a_download_larger_than_its_limit(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setattr(sources, "MAX_SOURCE_BYTES", len(ARCHIVE_BYTES) - 1)
    _serve(monkeypatch, ARCHIVE_BYTES)

    with pytest.raises(SourceError, match="is larger than"):
        fetch(_pinned(ARCHIVE_BYTES), tmp_path)

    assert list(tmp_path.iterdir()) == []

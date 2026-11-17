from __future__ import annotations

import hashlib
from collections.abc import Callable
from pathlib import Path
from typing import Any

Document = dict[str, Any]
ManifestWriter = Callable[[Document], Path]

PLATFORM = "linux-x86_64"
LIBRARY_CONTENTS = {"libglapi.so.0": b"dispatch tables", "libEGL.so.1": b"entry points"}
INSTALLED_AS = {"libglapi.so.0": "lib/libglapi.so.0.0.0", "libEGL.so.1": "lib/libEGL.so.1.0.0"}


def _repeated(digit: str) -> str:
    return digit * 64


def sample_document() -> Document:
    return {
        "schemaVersion": 1,
        "mesa": {
            "version": "24.2.8",
            "source": {"url": "https://archive.example.org/mesa-24.2.8.tar.xz", "sha256": _repeated("1")},
        },
        "meson": {"url": "https://archive.example.org/meson-1.5.2.tar.gz", "sha256": _repeated("2")},
        "sourceDateEpoch": 1732752000,
        "staticLibraries": {
            "libdrm": {
                "source": {"url": "https://archive.example.org/libdrm-2.4.123.tar.xz", "sha256": _repeated("3")},
                "mesonOptions": {"default_library": "static"},
            }
        },
        "mesonOptions": {"buildtype": "release", "gallium-drivers": ["zink"]},
        "bundles": {
            PLATFORM: {
                "entry": "libEGL.so.1",
                "dependencies": ["libglapi.so.0"],
                "driverSettings": {"MESA_LOADER_DRIVER_OVERRIDE": "zink", "GALLIUM_DRIVER": "zink"},
                "staticLibraries": ["libdrm"],
                "mesonOptions": {"platforms": []},
                "artifacts": dict(INSTALLED_AS),
                "sha256": {name: hashlib.sha256(content).hexdigest() for name, content in LIBRARY_CONTENTS.items()},
            }
        },
    }

from __future__ import annotations

from collections.abc import Sequence
from pathlib import Path

_TOOLCHAIN_PATH = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"


def reproducible_environment(source_date_epoch: int, home: Path, pkg_config_paths: Sequence[Path]) -> dict[str, str]:
    environment = {
        "PATH": _TOOLCHAIN_PATH,
        "HOME": str(home),
        "LC_ALL": "C.UTF-8",
        "TZ": "UTC",
        "PYTHONHASHSEED": "0",
        "PYTHONDONTWRITEBYTECODE": "1",
        "SOURCE_DATE_EPOCH": str(source_date_epoch),
    }
    if pkg_config_paths:
        environment["PKG_CONFIG_PATH"] = ":".join(str(path) for path in pkg_config_paths)
    return environment

from __future__ import annotations

import shutil
import subprocess
import sys
from collections.abc import Mapping
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class MesonProject:
    source: Path
    build: Path
    prefix: Path
    arguments: tuple[str, ...]
    cross_file: Path | None = None


@dataclass(frozen=True)
class MesonRunner:
    script: Path
    environment: Mapping[str, str]

    def install(self, project: MesonProject) -> None:
        for stale in (project.build, project.prefix):
            if stale.exists():
                shutil.rmtree(stale)

        cross = () if project.cross_file is None else (f"--cross-file={project.cross_file}",)
        self._run(
            "setup",
            str(project.build),
            str(project.source),
            f"--prefix={project.prefix}",
            "--libdir=lib",
            *cross,
            *project.arguments,
        )
        self._run("compile", "-C", str(project.build))
        self._run("install", "-C", str(project.build), "--no-rebuild")

    def _run(self, *arguments: str) -> None:
        subprocess.run([sys.executable, str(self.script), *arguments], env=dict(self.environment), check=True)

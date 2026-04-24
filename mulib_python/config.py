"""Configuration for mulib_python (kept minimal)."""

from __future__ import annotations

from dataclasses import dataclass


@dataclass
class MulibConfig:
    max_solutions: int = 100
    max_paths: int = 10000

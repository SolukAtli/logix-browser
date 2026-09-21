#!/usr/bin/env python3
"""Sync the Chromium Android source tree for Project Logix (Faz-4).

Automates:
  1. depot_tools checkout (or reuse via --depot-tools-dir)
  2. `fetch --nohooks android`  (first run only, ~100 GB+)
  3. `gclient sync` (+ optional `gclient runhooks`)

Examples:
  python scripts/sync-chromium.py --workspace ~/chromium-logix
  python scripts/sync-chromium.py --workspace ~/chromium-logix --skip-fetch --revision 134.0.0
  python scripts/sync-chromium.py --workspace ~/chromium-logix --dry-run
"""

from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys

DEPOT_TOOLS_URL = "https://chromium.googlesource.com/chromium/tools/depot_tools.git"
MIN_FREE_GB = 150


def run(cmd: list[str], cwd: str | None = None, dry_run: bool = False) -> None:
    print(f"+ {' '.join(cmd)}" + (f"  (cwd={cwd})" if cwd else ""))
    if dry_run:
        return
    subprocess.run(cmd, cwd=cwd, check=True)


def ensure_tool(name: str) -> None:
    if shutil.which(name) is None:
        sys.exit(f"error: required tool '{name}' not found on PATH")


def ensure_disk_space(path: str, minimum_gb: int) -> None:
    parent = os.path.abspath(path)
    os.makedirs(parent, exist_ok=True)
    free_gb = shutil.disk_usage(parent).free / (1024**3)
    print(f"free disk space at {parent}: {free_gb:.1f} GB (need >= {minimum_gb} GB)")
    if free_gb < minimum_gb:
        sys.exit("error: not enough free disk space for a Chromium checkout")


def ensure_depot_tools(workspace: str, explicit_dir: str | None, dry_run: bool) -> str:
    if explicit_dir:
        depot_dir = os.path.abspath(explicit_dir)
        if not os.path.isdir(depot_dir):
            sys.exit(f"error: --depot-tools-dir does not exist: {depot_dir}")
        return depot_dir
    depot_dir = os.path.join(workspace, "depot_tools")
    if os.path.isdir(os.path.join(depot_dir, ".git")):
        print(f"depot_tools already present at {depot_dir}")
        return depot_dir
    run(["git", "clone", DEPOT_TOOLS_URL, depot_dir], dry_run=dry_run)
    return depot_dir


def main() -> int:
    parser = argparse.ArgumentParser(description="Sync Chromium Android tree for Logix")
    parser.add_argument("--workspace", required=True, help="Root dir holding depot_tools/ and src/")
    parser.add_argument("--depot-tools-dir", default=None, help="Reuse an existing depot_tools checkout")
    parser.add_argument("--skip-fetch", action="store_true", help="Skip `fetch` (src/ already exists)")
    parser.add_argument("--revision", default=None, help="Pin src/ to a tag/branch, e.g. 134.0.0")
    parser.add_argument("--no-hooks", action="store_true", help="Skip `gclient runhooks`")
    parser.add_argument("--min-free-gb", type=int, default=MIN_FREE_GB)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    for tool in ("git", "python3"):
        ensure_tool(tool)
    ensure_disk_space(args.workspace, args.min_free_gb)

    depot_dir = ensure_depot_tools(args.workspace, args.depot_tools_dir, args.dry_run)
    env_path = depot_dir + os.pathsep + os.environ.get("PATH", "")
    os.environ["PATH"] = env_path
    os.environ.setdefault("DEPOT_TOOLS_UPDATE", "0")

    src_dir = os.path.join(args.workspace, "src")
    if not args.skip_fetch and not os.path.isdir(os.path.join(src_dir, ".git")):
        run(["fetch", "--nohooks", "android"], cwd=args.workspace, dry_run=args.dry_run)
    else:
        print("fetch skipped (src/ present or --skip-fetch)")

    if args.revision and not args.dry_run:
        run(["git", "fetch", "origin", "tag", args.revision], cwd=src_dir)
        run(["git", "checkout", args.revision], cwd=src_dir)

    run(["gclient", "sync", "-D", "--with_branch_heads"], cwd=args.workspace, dry_run=args.dry_run)
    if not args.no_hooks:
        run(["gclient", "runhooks"], cwd=args.workspace, dry_run=args.dry_run)

    print("Chromium tree ready at", src_dir)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

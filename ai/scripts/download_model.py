"""Download model.onnx from a GitHub Release into models/plant-v1/.

Example:
    python scripts/download_model.py --tag v0.1.0-demo1
"""

import argparse
import sys
from pathlib import Path

import httpx

DEFAULT_REPO = "DariusTurcu22/rootine"
DEFAULT_DEST = Path(__file__).resolve().parent.parent / "models" / "plant-v1" / "model.onnx"


def download(repo: str, tag: str, dest: Path) -> None:
    url = f"https://github.com/{repo}/releases/download/{tag}/model.onnx"
    dest.parent.mkdir(parents=True, exist_ok=True)
    tmp = dest.with_suffix(".onnx.part")
    with httpx.stream("GET", url, follow_redirects=True, timeout=60) as response:
        response.raise_for_status()
        with tmp.open("wb") as f:
            for chunk in response.iter_bytes():
                f.write(chunk)
    tmp.replace(dest)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--tag", required=True, help="GitHub Release tag, e.g. v0.1.0-demo1")
    parser.add_argument("--repo", default=DEFAULT_REPO, help="owner/name of the repository")
    parser.add_argument("--dest", type=Path, default=DEFAULT_DEST)
    args = parser.parse_args()
    try:
        download(args.repo, args.tag, args.dest)
    except httpx.HTTPError as exc:
        print(f"Download failed: {exc}", file=sys.stderr)
        return 1
    print(f"Saved {args.dest}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

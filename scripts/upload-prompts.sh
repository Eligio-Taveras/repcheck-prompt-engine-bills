#!/usr/bin/env bash
# Upload the prompt fragments under prompts/<prefix>/ to a GCS bucket, preserving the layout the
# loader expects (semver already in the filenames). Idempotent — re-running overwrites in place.
#
#   ./scripts/upload-prompts.sh repcheck-prompts-dev          # default prefix dir = bills
#   ./scripts/upload-prompts.sh repcheck-prompts-stg bills
set -euo pipefail

BUCKET="${1:?usage: upload-prompts.sh <bucket> [prefix-dir=bills]}"
PREFIX_DIR="${2:-bills}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)/prompts/${PREFIX_DIR}"

[ -d "$ROOT" ] || { echo "no prompts dir at $ROOT"; exit 1; }

echo "Uploading $ROOT/** -> gs://$BUCKET/$PREFIX_DIR/"
gsutil -m rsync -r "$ROOT" "gs://$BUCKET/$PREFIX_DIR"
echo "done."

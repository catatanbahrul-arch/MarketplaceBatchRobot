#!/bin/sh
set -eu

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

echo "ERROR: Gradle executable not found. Install/use Gradle Wrapper or run on Codemagic/Android Studio." >&2
exit 127

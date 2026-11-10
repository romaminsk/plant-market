#!/bin/sh
set -eu

# Docker secrets are mounted as files. Export each basename as an environment
# variable so Spring's ${VAR} placeholders keep working unchanged.
if [ -d /run/secrets ]; then
  for file in /run/secrets/*; do
    [ -f "$file" ] || continue
    name="$(basename "$file")"
    value="$(cat "$file")"
    export "$name=$value"
  done
fi

exec "$@"

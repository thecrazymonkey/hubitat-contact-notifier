#!/bin/sh
cd "$(dirname "$0")/.." || exit 1
rc=0
out=$(mktemp); trap 'rm -f "$out"' EXIT
for f in tests/*Spec.groovy; do
  if groovy -cp tests "$f" >"$out" 2>&1; then echo "PASS $f"; else echo "FAIL $f"; tail -15 "$out"; rc=1; fi
done
exit $rc

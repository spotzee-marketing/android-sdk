#!/bin/sh
set -eu

find .github/workflows -type f \( -name '*.yml' -o -name '*.yaml' \) -exec awk '
    /^[[:space:]]*(-[[:space:]]*)?uses:[[:space:]]*/ {
        ref = $0
        sub(/^[[:space:]]*(-[[:space:]]*)?uses:[[:space:]]*/, "", ref)
        sub(/[[:space:]]*#.*/, "", ref)
        sub(/[[:space:]]*$/, "", ref)

        if (ref ~ /^\.\// || ref ~ /^docker:\/\//) {
            next
        }

        if (ref !~ /@[[:xdigit:]]{40}$/) {
            printf "%s:%d: external action must use a full commit SHA: %s\n", FILENAME, FNR, ref
            violations = 1
        }
    }
    END {
        exit violations
    }
' {} +

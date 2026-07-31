#!/bin/sh
set -eu

version=$(awk -F= '$1 == "VERSION" { print $2 }' core/gradle.properties)

if [ -z "$version" ]; then
    echo "core/gradle.properties must declare VERSION" >&2
    exit 1
fi

expected="    implementation 'com.github.spotzee-marketing:android-sdk:v${version}'"
documented=$(
    grep -F "implementation 'com.github.spotzee-marketing:android-sdk:" README.md ||
        true
)

if [ "$documented" != "$expected" ]; then
    echo "README.md must document exactly ${expected#    }" >&2
    exit 1
fi

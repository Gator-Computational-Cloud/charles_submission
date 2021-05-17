#!/bin/bash
project_dir=.
build_dir=$project_dir
sources="$(find $project_dir/src/com/web | grep .java)"

for file in $sources; do
    command echo "Compiling: $file";
done

command javac -d "$build_dir/classes" -cp "$build_dir/jar/*" $sources

#!/bin/bash
project_dir=.
build_dir=$project_dir
sources="$(find $project_dir/src/com/web | grep .java)"

for file in $sources; do
    echo "Compiling: $file";
done

command javac -d "$build_dir/classes" -cp "$project_dir/lib/*" $sources
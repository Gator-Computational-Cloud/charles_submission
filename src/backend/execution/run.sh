#!/bin/bash
main="com.web.ExecDriver"

echo "Running $main"
cd classes

command java -cp ".;../lib/*" $main "$@"
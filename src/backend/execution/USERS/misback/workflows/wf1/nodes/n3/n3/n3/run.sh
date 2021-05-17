
#!/bin/bash
main="com.web.wc3"

echo "Running $main"
cd classes

command java -cp .:../src/jar/* $main "$@"

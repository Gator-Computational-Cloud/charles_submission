
#!/bin/bash
main="com.web.wc1"

echo "Running $main"
cd classes

command java -cp .:../src/jar/* $main "$@"


#!/bin/bash
main="com.web.wc4"

echo "Running $main"
cd classes

command java -cp .:../src/jar/* $main "$@"

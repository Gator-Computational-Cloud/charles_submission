
#!/bin/bash
main="com.web.wc2"

echo "Running $main"
cd classes

command java -cp .:../src/jar/* $main "$@"

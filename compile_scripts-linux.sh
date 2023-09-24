#!/bin/sh

# If there's not bot.jar in the folder, first call ./compile_all_linux.sh
if [ ! -f bot.jar ]; then
    ./compile_all_linux.sh
fi

chmod +x apache-ant-1.10.12/bin/ant
./apache-ant-1.10.12/bin/ant -f build.xml compile-scripts

#!/bin/bash

# If there's not bot.jar in the folder, first call ./compile_all_linux.sh
if [ ! -f bot.jar ]; then
    ./compile_all_linux.sh
fi

./apache-ant-1.10.12/bin/ant -f build.xml compile-scripts
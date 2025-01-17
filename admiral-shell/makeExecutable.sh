#!/bin/sh
cat runner.sh target/admiral-shell-jar-with-dependencies.jar > target/dash
chmod ugo+x target/dash

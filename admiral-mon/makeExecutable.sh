#!/bin/sh
cat runner.sh target/admiral-mon-jar-with-dependencies.jar > target/damon
chmod ugo+x target/damon

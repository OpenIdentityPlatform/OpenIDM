#!/bin/sh

# dot-command script to allow OpenIDM to be shut down from a double-click in the finder

export JAVA_HOME=/Library/Java/Home

 `dirname "$0"`/../../shutdown.sh

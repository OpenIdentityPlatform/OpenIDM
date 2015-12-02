#!/bin/sh

notRunning() {
  cleanupPidFile
  echo "OpenIDM is not running, not stopping."
  exit 1
}

cleanupPidFile() {
  # clean up left over pid files if necessary
  if [ -f "$OPENIDM_PID_FILE" ]; then
    rm -f "$OPENIDM_PID_FILE"
  fi
}

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

echo "$PRG"

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

# Only set OPENIDM_HOME if not already set
[ -z "$OPENIDM_HOME" ] && OPENIDM_HOME=`cd "$PRGDIR" >/dev/null; pwd`

# Only set OPENIDM_PID_FILE if not already set
[ -z "$OPENIDM_PID_FILE" ] && OPENIDM_PID_FILE="$OPENIDM_HOME"/.openidm.pid

if [ -f "$OPENIDM_PID_FILE" ]; then
  START_PID=`cat "$OPENIDM_PID_FILE"`
fi
if [ -z "$START_PID" ]; then
  notRunning
fi

EXISTING_START_RUNNING=`ps -p $START_PID -o command= | grep "./startup.sh"`

# Check if the pid file points to a running process that is the openidm jvm
if [ "$EXISTING_START_RUNNING" ]; then
    echo "Stopping OpenIDM ($START_PID)"
    pkill -P $START_PID
    cleanupPidFile
    exit 0
fi

# If the PID it points to is not the start script
# then it is a stale pid file
notRunning


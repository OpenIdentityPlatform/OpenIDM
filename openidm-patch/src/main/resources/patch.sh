#!/bin/sh
#set -x

# TODO List:
# 1. Detect whether patch has already been applied
# 2. Detect if patch is applicable
# 3. Windows batch script
# 4. Some sort of actions file to specify what to do
# 5. Generate backout script and save to ARCHIVE_DIR

IFS=
PATH=/bin:/usr/bin

UPGRADE_VERSION=2.1.1
OBSOLETE_VERSIONS=("2.1.0")

DATE_TIME=`date +%Y-%m-%d_%s`
TMP_LOG_FILE=$(mktemp /tmp/openidm_patch.XXXXXXXXXX) || { echo "Failed to create temp file"; exit 1; }

# The list of obsolete bundles
obsoleteFiles=(
"bundle/openidm-audit-2.1.0.jar"
"bundle/openidm-config-2.1.0.jar"
"bundle/openidm-core-2.1.0.jar"
"bundle/openidm-crypto-2.1.0.jar"
"bundle/openidm-customendpoint-2.1.0.jar"
"bundle/openidm-enhanced-config-2.1.0.jar"
"bundle/openidm-external-email-2.1.0.jar"
"bundle/openidm-external-rest-2.1.0.jar"
"bundle/openidm-filter-2.1.0.jar"
"bundle/openidm-httpcontext-2.1.0.jar"
"bundle/openidm-infoservice-2.1.0.jar"
"bundle/openidm-jaas-loginmodule-repo-2.1.0.jar"
"bundle/openidm-jetty-fragment-2.1.0.jar"
"bundle/openidm-policy-2.1.0.jar"
"bundle/openidm-provisioner-2.1.0.jar"
"bundle/openidm-provisioner-openicf-2.1.0.jar"
"bundle/openidm-quartz-fragment-2.1.0.jar"
"bundle/openidm-repo-2.1.0.jar"
"bundle/openidm-repo-jdbc-2.1.0.jar"
"bundle/openidm-repo-orientdb-2.1.0.jar"
"bundle/openidm-restlet-2.1.0.jar"
"bundle/openidm-scheduler-2.1.0.jar"
"bundle/openidm-security-jetty-2.1.0.jar"
"bundle/openidm-servletfilter-registrator-2.1.0.jar"
"bundle/openidm-smartevent-2.1.0.jar"
"bundle/openidm-system-2.1.0.jar"
"bundle/openidm-ui-2.1.0.jar"
"bundle/openidm-util-2.1.0.jar"
"bundle/openidm-workflow-activiti-2.1.0.jar")

# The list of updated bundles
upgradeFiles=(
"bundle/openidm-audit-2.1.1.jar"
"bundle/openidm-config-2.1.1.jar"
"bundle/openidm-core-2.1.1.jar"
"bundle/openidm-crypto-2.1.1.jar"
"bundle/openidm-customendpoint-2.1.1.jar"
"bundle/openidm-enhanced-config-2.1.1.jar"
"bundle/openidm-external-email-2.1.1.jar"
"bundle/openidm-external-rest-2.1.1.jar"
"bundle/openidm-filter-2.1.1.jar"
"bundle/openidm-httpcontext-2.1.1.jar"
"bundle/openidm-infoservice-2.1.1.jar"
"bundle/openidm-jaas-loginmodule-repo-2.1.1.jar"
"bundle/openidm-jetty-fragment-2.1.1.jar"
"bundle/openidm-policy-2.1.1.jar"
"bundle/openidm-provisioner-2.1.1.jar"
"bundle/openidm-provisioner-openicf-2.1.1.jar"
"bundle/openidm-quartz-fragment-2.1.1.jar"
"bundle/openidm-repo-2.1.1.jar"
"bundle/openidm-repo-jdbc-2.1.1.jar"
"bundle/openidm-repo-orientdb-2.1.1.jar"
"bundle/openidm-restlet-2.1.1.jar"
"bundle/openidm-scheduler-2.1.1.jar"
"bundle/openidm-security-jetty-2.1.1.jar"
"bundle/openidm-servletfilter-registrator-2.1.1.jar"
"bundle/openidm-smartevent-2.1.1.jar"
"bundle/openidm-system-2.1.1.jar"
"bundle/openidm-ui-2.1.1.jar"
"bundle/openidm-util-2.1.1.jar"
"bundle/openidm-workflow-activiti-2.1.1.jar")

printBanner() {
  log "\n====================================================\n"
  log "OpenIDM $UPGRADE_VERSION Maintenance Release - Patch Script\n"
  log "Copyright (c) 2013 ForgeRock AS. All Rights Reserved\n"
  log "====================================================\n"
  log "DATE: $DATE_TIME\n"
}

printUsage() {
  printf "Usage: patch.sh -h OPENIDM_HOME [-r PATCH_ARCHIVE] [-a ARCHIVE_DIR]\n"
  printf "\nRequired:\n"
  printf " -h OPENIDM_HOME: Specifies the full path to the OpenIDM instance to patch.\n"
  printf "\nOptional:\n"
  printf " -a ARCHIVE_DIR: Specifies the full path to the directory in which to generate the patch archive.\n"
  printf " -r PATCH_ARCHIVE: Specifies the full path to the patch archive to restore.\n"
}

invalidOption() {
  log "ERROR: Invalid option specified: -$1\n"
  printUsage
  exit 1
}

missingOption() {
  log "ERROR: Required option not specified: -$1\n"
  printUsage
  exit 1
}

verifyIdmHome() {
  # Perform a basic sanity check
  log "Verifying OpenIDM instance in $OPENIDM_HOME\n"
  if [ ! -f "$OPENIDM_HOME/bin/launcher.json" ]; then
    log "Invalid OPENIDM_HOME specified: $OPENIDM_HOME\n"
    abort "BAD_OPENIDM_HOME"
  fi
}

abortIfRunning() {
  log "OpenIDM instance is: "
  # Only set OPENIDM_PID_FILE if not already set
  [ -z "$OPENIDM_PID_FILE" ] && OPENIDM_PID_FILE=$OPENIDM_HOME/.openidm.pid

  if [ -f $OPENIDM_PID_FILE ]; then
    START_PID=`cat "$OPENIDM_PID_FILE"`
  fi

  if [ -n "$START_PID" ]; then
    EXISTING_START_RUNNING=`ps -p $START_PID -o command= | grep "openidm"`

    # Check if the pid file points to a running process that is the openidm jvm
    if [ "$EXISTING_START_RUNNING" ]; then
      log "RUNNING (pid=%s)\n" "$START_PID\n"
      log "Before applying this patch please shutdown the OpenIDM instance by executing:\n"
      log "$OPENIDM_HOME/shutdown.sh\n"
      abort "OPENIDM_RUNNING"
    fi
  fi
  log "STOPPED\n"
}

abortBackupAndFail() {
  log "\n\nERROR: Failure while creating backup of obsolete files.\nUpgrade aborting, please contact Forgerock Support for assistance.\n"
  log "\nRemoving archive directory $ARCHIVE_DIR\n"
  auditCmd "rm -rf $ARCHIVE_DIR"
  exit 1
}

performBackup() {
  # Backup the files being removed or replaced
  [ -z $ARCHIVE_DIR ] && ARCHIVE_DIR=$OPENIDM_HOME/.patch/$DATE_TIME
  if [ ! -d "$ARCHIVE_DIR" ]; then
     auditCmd "mkdir -p ${ARCHIVE_DIR}/bundle"
  fi
  log "Archive directory is: $ARCHIVE_DIR\n\n"

  log "Archiving felix-cache..."
  if [ -d $OPENIDM_HOME/felix-cache ]; then
    auditCmd "cp -pr $OPENIDM_HOME/felix-cache $ARCHIVE_DIR/"
    log "DONE.\n"
  else
    log "\nWARNING: Could not locate the felix-cache directory. This is probably not an issue, \
however if you have changed the location of the felix-cache from the default location you MUST \
purge the cache prior to restarting OpenIDM. Failing to do so we leave OpenIDM in an \
inconsistent state.\n\n"
  fi

  log "Archiving obsolete files..."
  for f in "${obsoleteFiles[@]}"
  do
    if [ -f $OPENIDM_HOME/$f ]; then
      auditCmd "cp -pr $OPENIDM_HOME/$f $ARCHIVE_DIR/$f"
    else
      log "\nERROR: Missing $OPENIDM_HOME/$f while creating archive."
      abortBackupAndFail
    fi
  done
  log "DONE.\n"
}

performUpgrade() {
  # Delete the existing OpenIDM bundle jars
  log "Deleting obsolete files..."
  for f in "${obsoleteFiles[@]}"
  do
  if [ -f "$ARCHIVE_DIR/$f" ]; then
     auditCmd "rm $OPENIDM_HOME/$f"
  fi
  done
  log "DONE.\n"

  # Copy the updated OpenIDM bundle jars
  log "Copying upgraded files..."
  for f in "${upgradeFiles[@]}"
  do
    auditCmd "cp $PRGDIR/$f $OPENIDM_HOME/$f"
  done
  log "DONE.\n"

  # Purge the felix-cache
  log "Purging the felix-cache..."
  if [ -d "$OPENIDM_HOME/felix-cache" ]; then
    auditCmd "rm -rf $OPENIDM_HOME/felix-cache"
  fi
  log "DONE.\n"
}

performRestore() {
 if [ -d $DO_RESTORE ]; then
    log "\nRestoring patch archive..."

    # Delete upgrade files
    for f in "${upgradeFiles[@]}"
    do
     auditCmd "rm $OPENIDM_HOME/$f"
    done

    # Restore obsolete files
    for f in "${obsoleteFiles[@]}"
    do
     auditCmd "cp -p ${1%/}/$f $OPENIDM_HOME/$f"
    done
   
    # Restore felix-cache
    if [ -d $OPENIDM_HOME/felix-cache ]; then
      auditCmd "rm -rf $OPENIDM_HOME/felix-cache"
    fi
    if [ -d $ARCHIVE_DIR/felix-cache ]; then
      auditCmd "cp -pr ${1%/}/felix-cache $OPENIDM_HOME/"
    fi
    log "DONE.\n"
 else
    log "\nERROR: Unable to restore patch archive from $DO_RESTORE\n"
 fi
}

logPatchHistory() {
  PATCH_HISTORY_FILE=$OPENIDM_HOME/.patch_history
  if [ -n "$PATCH_STARTED" ]; then
    if [ -n "$DO_RESTORE" ]; then
      if [ -n "$PATCH_FAILED" ]; then
        printf "$DATE_TIME - OpenIDM restore from archive failed.\n" >> $PATCH_HISTORY_FILE
      else
        printf "$DATE_TIME - OpenIDM restore from archive completed successfully.\n" >> $PATCH_HISTORY_FILE
      fi
    else
      printf "  Archive dir: $ARCHIVE_DIR\n" >> $PATCH_HISTORY_FILE
      if [ -n "$PATCH_FAILED" ]; then
        printf "$DATE_TIME - OpenIDM $UPGRADE_VERSION upgrade failed.\n" >> $PATCH_HISTORY_FILE
      else
        printf "$DATE_TIME - OpenIDM $UPGRADE_VERSION upgrade completed successfully.\n" >> $PATCH_HISTORY_FILE
      fi
    fi
  else
    if [ -n "$DO_RESTORE" ]; then
      printf "$DATE_TIME - OpenIDM restore from archive started.\n" >> $PATCH_HISTORY_FILE
      printf "  Archive dir: $DO_RESTORE\n" >> $PATCH_HISTORY_FILE
    else
      printf "$DATE_TIME - OpenIDM $UPGRADE_VERSION upgrade started.\n" >> $PATCH_HISTORY_FILE
    fi
    PATCH_STARTED=true
  fi
}

abort() {
  log "$DATE_TIME: $1\n"
  PATCH_FAILED=$1
  logPatchHistory
  exit 1
}

log() {
  printf "$1"
  logAudit $1
}

logAudit() {
  printf "$1" >> $TMP_LOG_FILE
}

auditCmd() {
  logAudit "\nEXECUTING: $1\n"
  eval "$1">/dev/null
  logAudit "RESULT: $?\n"
}

# END OF FUNCTIONS
# BEGINING OF SCRIPT EXECUTION
# -----------------------------------------------------------------------------

# Print the banner
printBanner

if [ $# -eq 0 ] ; then
    printUsage
    exit 1
fi

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# resolve links - $0 may be a softlink
PRG="$0"

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

# get the options specified by the user
while getopts ":r:h:a:" opt
do 
  case $opt
  in
     r) DO_RESTORE=${OPTARG%/};;
     h) OPENIDM_HOME=${OPTARG%/};;
     a) ARCHIVE_DIR=${OPTARG%/};;
     :) missingOption ${OPTARG};;
     \?) invalidOption ${OPTARG};;
  esac
done

log "Logfile: $TMP_LOG_FILE\n"

# Log the upgrade and status to the history
logPatchHistory

# Validate OPENIDM_HOME and ensure it is a valid OpenIDM directory
verifyIdmHome

# Abort if OpenIDM is currently running
abortIfRunning

# Check if we are restoring a previous backup
if [ -n "$DO_RESTORE" ]; then
  performRestore $DO_RESTORE
else
  # Backup the files being removed or replaced
  performBackup

  # Perform the upgrade
  performUpgrade
fi

# Log the upgrade and status to the history
logPatchHistory

if [ -n "$DO_RESTORE" ]; then
  log "\nOpenIDM has been successfully restored from $DO_RESTORE!\n\n"
else
  log "\nOpenIDM has been successfully upgraded to $UPGRADE_VERSION!\n\n"
  auditCmd "cp ${TMP_LOG_FILE} ${ARCHIVE_DIR}/openidm_patch.log"
fi

exit 0

#!/bin/sh
#
# This script creates Megatron reports (--create-reports) and using a semaphore 
# file to signal when it is safe to copy the generated files.

export SEMAPHORE_FILE=/var/megatron/flash-xml/reports-generated-successfully

if test -f /var/megatron/megatron.pid ; then
  echo `date`: "Megatron already started; aborting... (lock-file '/var/megatron/megatron.pid' exists)."
else
  echo `date`: "Megatron Starts to Generate Reports."

  if test -f $SEMAPHORE_FILE ; then
    rm $SEMAPHORE_FILE
  fi
  
  /usr/local/megatron/bin/megatron.sh --create-reports
  MEGATRON_EXIT_CODE=${?}
  if [ $MEGATRON_EXIT_CODE -eq "0" ] ; then
    echo `date`: "Megatron Finished Successfully; reports generated"
    echo "Reports created:" `date` > $SEMAPHORE_FILE 
  else
    echo `date`: "Megatron Finished with Errors; no reports generated. Exit-code:" $MEGATRON_EXIT_CODE
  fi
fi

#!/bin/sh
#
# This script creates Megatron organization reports (emails abuse reports).

export MEGATRON_LOCK_FILE=/var/megatron/megatron.pid

i=0
while [ $i -lt 5 ] 
do
  i=`expr $i + 1`
  if test -f $MEGATRON_LOCK_FILE ; then
    echo `date`: "Megatron already started; sleeping... (lock-file" $MEGATRON_LOCK_FILE "exists)."
    sleep 600
  fi
done

if test -f $MEGATRON_LOCK_FILE ; then
  echo `date`: "Lock-file still present; aborting (generate organization reports)..."
else
  echo `date`: "Megatron Starts to Generate Organization Reports."  
  /usr/local/megatron/bin/megatron.sh --create-report se.sitic.megatron.report.OrganizationReportGenerator
  MEGATRON_EXIT_CODE=${?}
  if [ $MEGATRON_EXIT_CODE -eq "0" ] ; then
    echo `date`: "Megatron Finished Successfully; organization reports generated"
  else
    echo `date`: "Megatron Finished with Errors. Exit-code:" $MEGATRON_EXIT_CODE
  fi
fi

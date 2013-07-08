#!/bin/sh

# Checks if lock-file for Megatron exists and removes it if it's stale.
# Execute this script before Megatron is called from cron during nights 
# and weekends.

if test -f /var/megatron/megatron.pid ; then
  # check if there is a java process running, if not the lockfile is probably stale
  MEGATRON_RUNNING=`ps auxwww|grep ".*java.*Megatron.*"|grep -v grep| wc -l | sed 's/ //g'`
  if [ $MEGATRON_RUNNING = 0 ]; then
      echo "Removing stale lock-file '/var/megatron/megatron.pid'."
      rm /var/megatron/megatron.pid
  else
    echo "Keeping lock-file '/var/megatron/megatron.pid'; Megatron seems to be running (a Megatron-process exists)."
  fi
fi

#!/usr/local/bin/bash

# a script that creates a ticket in the Megatron queue in RT. Script returns a ticked id from RT

no_of_args=$#

Comment=""
DATE=`date "+%Y%m%d"`

pid=/var/megatron/megatron.pid


# small help function
HELP ()
{
    echo ""
    echo "Usage: $0 <OPTIONS>"
    echo ""
    echo "Script for creating a ticket in the Megatron-queue in RT. Script returns the ticket id from RT"
    echo ""
    echo "OPTIONS:"
    echo "-j Jobname - The *same* name as the Megatron job in the mail sent from Megatron"
    echo "                               Example: -j shadowserver-drone_2009-06-22_160142"
    echo ""

    exit 1
}

if [ -f $pid ]; then
    echo "Megatron already started.  (lock-file '/var/megatron/megatron.pid' exists)."
    exit 1
fi


if [ $(whoami) != "u_megatron" ]; then
    echo "Error: This script must be executed by the u_megatron user."
    echo "Solution: sudo su u_megatron + rerun this script"
    echo ""
    exit 1
fi


# this script needs options
if [ X$1 = "X" ]; then
    HELP
    exit 1
fi

# this script needs at least an option with an argument
if [ $no_of_args == 1 ]; then
    HELP
fi


while getopts ":hp:j:" opt; do
    case $opt in

    h) HELP;;

    p)
        echo "-p is no longer supported. Switch is ignored. Priority is assigned in config."
#        if [ $(echo "$OPTARG" | grep -E "^[0-9]+$") ]; then
#            if [ $OPTARG -gt 0 ]; then
#                Priority=$OPTARG
#            fi
#        else
#            echo "ERROR: -p requires an integer as input"
#            HELP
#        fi
    ;;
    
    j)  JobName=$OPTARG ;;
    
    \?)
        echo "Invalid option: -$OPTARG" >&2
        HELP    
    ;;
    
    :)
        echo "Option -$OPTARG requires an argument."
        HELP
    ;;
    
    *)
        echo "ERROR: no argument given"
        HELP
    ;;    

    esac
done

echo "Creating ticket in RT with Subject: $JobName"
echo "This will take a few seconds..."  

# First create a ticket in the Megatron queue
ticket_id=`/usr/local/megatron/bin/rt create -t ticket \
set subject="$JobName" \
set queue=Megatron \
set owner=Nobody |awk '{print $3}'`

echo "Ticket ID: $ticket_id" 
echo ""
echo "Now running Megatron dry run..."

echo "/usr/local/megatron/bin/megatron.sh --job $JobName --id $ticket_id --mail-dry-run"
cd /usr/local/megatron/bin
#save output from megatron in a temporary file
/usr/local/megatron/bin/megatron.sh --job $JobName --id $ticket_id --mail-dry-run > /tmp/$JobName
cat /tmp/$JobName

echo "Do you want to send the mails? y/n [No]"
read YN

# a comment with the output from megatron.sh is sent to the ticket in RT
case $YN in
    [yY]|[yY][eE][sS])
        logger "$0 -> mail sent -> /usr/local/megatron/bin/megatron.sh --job $JobName --id $ticket_id --mail"
        echo "/usr/local/megatron/bin/megatron.sh --job $JobName --id $ticket_id --mail"
        /usr/local/megatron/bin/megatron.sh --job $JobName --id $ticket_id --mail
    
        # send a comment to the ticket for stuff that's not sent
        send_comment=`cat /tmp/$JobName`
        echo "Mail sent unless items quarantined. Ticket exists at https://rt.sitic.se/Ticket/Display.html?id=$ticket_id"

        #    rt comment -m "$send_comment" $ticket_id
    ;;
    
    *)
        echo "Mail not sent. Ticket exists at https://rt.sitic.se/Ticket/Display.html?id=$ticket_id"
        send_comment=`cat /tmp/$JobName`
        rt comment -m "$send_comment" $ticket_id
    ;;
esac

#remove the temporary job-file
#rm -f /tmp/$JobName-$DATE
#rm -f /tmp/$JobName-$Date-header

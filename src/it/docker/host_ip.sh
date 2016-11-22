#!/usr/bin/env bash
#
# Computes IP address on either MacOS or Linux.
#

if [ "$(uname)" == "Darwin" ]; then
    # MacOS
    route -n get default|grep interface|awk '{print $2}'|xargs ipconfig getifaddr
elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
    /sbin/ip route|awk '/default/ { print $3 }'
else
    # An unsupported OS.
    exit 1
fi

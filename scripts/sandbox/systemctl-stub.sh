#!/bin/bash

# Simple systemctl emulator wrapper for Ubuntu Sandbox and Docker environments
# Maps systemctl commands (start, stop, restart, status, is-active) to traditional service scripts.

# Strip quotes if any
action=$(echo "$1" | tr -d "'\"")
shift

if [ "$1" = "--" ]; then shift; fi
service=$(echo "$1" | tr -d "'\"")

case "$action" in
    restart)
        service "$service" restart
        ;;
    start)
        service "$service" start
        ;;
    stop)
        service "$service" stop
        ;;
    status)
        service "$service" status
        ;;
    is-active)
        service "$service" status | grep -q 'is running' && echo 'active' || echo 'inactive'
        ;;
    *)
        echo "systemctl-stub: action '$action' not implemented" >&2
        exit 1
        ;;
esac
exit 0

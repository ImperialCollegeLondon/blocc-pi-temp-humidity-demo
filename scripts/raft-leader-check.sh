#!/bin/bash

retries=20

while [[ $retries -gt 0 ]]; do
    curl -s http://${FABRIC_ORDERER_ADDRESS}/metrics > /tmp/orderer_metrics 2> /dev/null

    if [ $? -ne 0 ]; then
        echo "Failed to fetch metrics from orderer. Retrying..."
        ((retries--))
        sleep 3
        continue
    fi

    STATUS=$(grep 'consensus_etcdraft_is_leader' /tmp/orderer_metrics | grep -o '[0-1]$')

    if [ "$STATUS" == "1" ]; then
        echo "Orderer is RAFT leader. Starting blocc-temp-humidity-app..."
        ./bin/blocc-temp-humidity-app  # Start the app here
        break
    else
        echo "Orderer is not RAFT leader. Retrying..."
        ((retries--))
        sleep 3
    fi
done

if [[ $retries -eq 0 ]]; then
    echo "Failed to confirm RAFT leader status after several attempts."
    exit 1
fi

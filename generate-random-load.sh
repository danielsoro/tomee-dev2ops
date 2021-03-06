#!/bin/bash

SERVICES=(
"GET https://localhost:8443/tomee-dev2ops-1.0-SNAPSHOT/color/"
"GET https://localhost:8443/tomee-dev2ops-1.0-SNAPSHOT/color/object"
"GET https://localhost:8443/tomee-dev2ops-1.0-SNAPSHOT/color/object"
"POST https://localhost:8443/tomee-dev2ops-1.0-SNAPSHOT/color/orange"
"POST https://localhost:8443/tomee-dev2ops-1.0-SNAPSHOT/color/yellow"
"POST https://localhost:8443/tomee-dev2ops-1.0-SNAPSHOT/color/green"
)

while true; do
echo ${SERVICES[$RANDOM % ${#SERVICES[@]} ]}
done | xargs -P8 -n 2 curl --insecure -u snoopy:pass -X 

#!/usr/bin/env bash
# Shows UP / DOWN for every backend app. Usage: bash status.sh
for p in "8080 api-gateway" "8081 auth" "8082 habitat" "8083 wildlife" "8084 resource" \
         "8085 patrol" "8086 incident" "8087 rescue" "8088 programme" "8089 reporting"; do
  set -- $p
  if curl -s "http://localhost:$1/actuator/health" | grep -q '"UP"'; then s=UP; else s=DOWN; fi
  printf "%-12s %-5s %s\n" "$2" "$1" "$s"
done
echo "A service that stays DOWN: read its log, e.g.  tail -50 logs/incident-service.log"

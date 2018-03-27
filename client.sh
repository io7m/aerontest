#!/bin/sh

if [ $# -ne 3 ]
then
  echo "usage: server-address client-address client-port" 1>&2
  exit 1
fi

SERVER_ADDRESS="$1"
shift
CLIENT_ADDRESS="$1"
shift
CLIENT_PORT="$1"
shift

exec java -cp target/com.io7m.aerontest-0.0.1.jar com.io7m.api_exp2_nat.Client \
  --server-address "${SERVER_ADDRESS}" \
  --client-local-address "${CLIENT_ADDRESS}" \
  --client-local-port "${CLIENT_PORT}"

#!/bin/sh

if [ $# -ne 1 ]
then
  echo "usage: address" 1>&2
  exit 1
fi

ADDRESS="$1"

exec java -cp target/com.io7m.aerontest-0.0.1.jar com.io7m.api_exp2_nat.Server --local-address "${ADDRESS}"

#!/bin/bash

QDIR="_local_queue"
QUEUE="montecarlo_queue"
OUTDIR=output
INDIR=input
DIR=`dirname $0`
ARGS=""

halt() {
  echo $*
  exit 1
}

function check_docker_exec() {
  DOCKER=`which docker 2>&1` || halt "Cannot find docker client"
}

function check_docker_connect() {
  [ -n "$DOCKER" ] || check_docker_exec
  $DOCKER ps > /dev/null 2>&1 || halt "Cannot connect to docker. Is it started? is DOCKER_HOST set? use sudo?"
}

[ "$1" == "--nopull" ] && ARGS+=" $1" && shift
[[ "$1" == "" || ! -f "$1" ]] && halt "Usage: $0 [--nopull] TEST_JD*"
check_docker_connect 

rm -rf $QDIR* $OUTDIR $INDIR
mkdir -p $QDIR
mkdir $OUTDIR
mkdir $INDIR
i=1
for f in $* ; do
  cp $f $QDIR/$i.json
  i=$[$i+1]
done

$DIR/../executors/flat/wooster.py -d $QDIR -v -n 2 -o $OUTDIR $ARGS

ls -lR $OUTDIR*
ls -lR $INDIR*

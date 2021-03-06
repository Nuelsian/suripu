#!/bin/sh

VERSION=$1

if [ -z "$1" ]
  then
    echo "[ERROR] \xE2\x9A\xA0 Missing version number"
    exit 1
fi

s3cmd get s3://hello-deploy/configs/com/hello/suripu/suripu-app/$VERSION/suripu-app.staging.yml . --force
s3cmd get s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/pill.staging.yml . --force
s3cmd get s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/pill_save_ddb.staging.yml . --force
s3cmd get s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/alarm_worker.staging.yml . --force
s3cmd get s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/sense_save.staging.yml . --force
s3cmd get s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/sense_save_ddb.staging.yml . --force
s3cmd get s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/index_logs_worker.staging.yml . --force
s3cmd get s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/insights_generator.staging.yml . --force
s3cmd get s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/timeline_worker.staging.yml . --force
s3cmd get s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/push-notifications.staging.yml . --force
s3cmd get s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/sense_last_seen.staging.yml . --force
s3cmd get s3://hello-deploy/configs/com/hello/suripu/suripu-workers/$VERSION/timeline_logs.staging.yml . --force
s3cmd get s3://hello-deploy/configs/com/hello/suripu/suripu-queue/$VERSION/suripu-queue.staging.yml . --force

s3cmd get s3://hello-maven/release/com/hello/suripu/suripu-app/$VERSION/suripu-app-$VERSION.jar . --force
s3cmd get s3://hello-maven/release/com/hello/suripu/suripu-workers/$VERSION/suripu-workers-$VERSION.jar . --force
s3cmd get s3://hello-maven/release/com/hello/suripu/suripu-queue/$VERSION/suripu-queue-$VERSION.jar . --force

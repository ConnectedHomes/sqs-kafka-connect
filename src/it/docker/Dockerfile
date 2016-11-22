FROM bgchops-docker-dockerv2-local.artifactoryonline.com/bgch-dp/dp-spark-driver

COPY target/*.deb /app

RUN dpkg -i -R /app
EXPOSE  7001 7002 7003 7004 7006 8888

ENV PROJECT=dp-heating-period-sample-job
ENV PROJECT_JOB=run

ENTRYPOINT ["/usr/lib/dataplatform/dp-heating-period-sample-job/run.sh"]

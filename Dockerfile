FROM amazoncorretto:25

RUN yum install findutils -y && \
    yum clean all && \
    rm -rf /var/cache/yum

# entrypoint
COPY docker-entrypoint.sh /agent/

# jar app
COPY build/quarkus-app/lib/ /agent/lib/
COPY build/quarkus-app/*.jar /agent/
COPY build/quarkus-app/app/ /agent/app/
COPY build/quarkus-app/quarkus/ /agent/quarkus/

# native app
COPY --chmod=0755 build/quarkus-run /agent/

ENTRYPOINT ["/agent/docker-entrypoint.sh"]
CMD ["jvm"]

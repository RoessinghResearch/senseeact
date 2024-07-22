# Build from git directory:
# docker build -t senseeacttomcat \
#     -f senseeact/ExampleSenSeeActService/docker/senseeacttomcat/Dockerfile \
#     --build-arg GOOGLE_APPLICATION_CREDENTIALS=path/to/credentials.json \
#     --build-arg TOMCAT_ADMIN_USERNAME=admin \
#     --build-arg TOMCAT_ADMIN_PASSWORD=secret \
#     .

FROM tomcat:10.1-jdk17 AS build
ARG GOOGLE_APPLICATION_CREDENTIALS
WORKDIR /app
COPY ./rrd-utils/RRDUtils/ ./rrd-utils/RRDUtils/
COPY ./senseeact/ ./senseeact/
COPY $GOOGLE_APPLICATION_CREDENTIALS google-application-credentials.json
WORKDIR /app/senseeact/ExampleSenSeeActService
RUN chmod 755 gradlew && sed -i 's/\r//' gradlew && ./gradlew build

FROM tomcat:10.1-jdk17
ARG TOMCAT_ADMIN_USERNAME
ARG TOMCAT_ADMIN_PASSWORD
COPY --from=build /app/google-application-credentials.json /app/
ENV GOOGLE_APPLICATION_CREDENTIALS=/app/google-application-credentials.json
WORKDIR $CATALINA_HOME
COPY --from=build /app/senseeact/ExampleSenSeeActService/build/libs/example-senseeact-service-6.0.8.war webapps/servlets#senseeact.war
COPY senseeact/ExampleSenSeeActService/docker/senseeacttomcat/tomcat-users.xml conf/
RUN sed -i -e "s/\${TOMCAT_ADMIN_USERNAME}/$TOMCAT_ADMIN_USERNAME/" \
	-e "s/\${TOMCAT_ADMIN_PASSWORD}/$TOMCAT_ADMIN_PASSWORD/" \
	conf/tomcat-users.xml
RUN start=`grep -n -m 1 "<Connector protocol=\"AJP/1.3\"" conf/server.xml | sed -e "s/:.*$//"` && \
	end=`tail -n +$start conf/server.xml | grep -n -m 1 "\-->" | sed -e "s/:.*$//"` && \
	start=`expr $start - 1` && end=`expr $start + $end` && \
	sed -i -e "${start},${end}d" conf/server.xml && \
	sed -i -e "${start}i <Connector protocol=\"AJP/1.3\" address=\"0.0.0.0\" port=\"8009\" redirectPort=\"8443\" secretRequired=\"false\" />" conf/server.xml
RUN	cp -r webapps.dist/* webapps
COPY senseeact/ExampleSenSeeActService/docker/senseeacttomcat/context.xml webapps/manager/META-INF/
EXPOSE 8009

# Build from git directory:
# docker build -t senseeactweb -f senseeact/ExampleSenSeeActService/docker/senseeactweb/Dockerfile .

FROM httpd:2.4-alpine AS dev
WORKDIR /usr/local/apache2
COPY senseeact/ExampleSenSeeActService/docker/senseeactweb/*.conf conf/extra/
RUN sed -i \
	-e "s/^#\(LoadModule xml2enc_module\)/\1/" \
	-e "s/^#\(LoadModule proxy_html_module\)/\1/" \
	-e "s/^#\(LoadModule proxy_module\)/\1/" \
	-e "s/^#\(LoadModule proxy_http_module\)/\1/" \
	-e "s/^#\(LoadModule proxy_fcgi_module\)/\1/" \
	-e "s/^#\(LoadModule proxy_ajp_module\)/\1/" \
	-e "s/^#\(LoadModule rewrite_module\)/\1/" \
	-e "s/^#\(Include conf\/extra\/httpd-vhosts.conf\)/\1/" \
	-e "/<Directory \"\/usr\/local\/apache2\/htdocs\">/,/<\/Directory>/s/^\(\s*\)Options .*$/\1Options FollowSymLinks/" \
	-e "/<Directory \"\/usr\/local\/apache2\/htdocs\">/,/<\/Directory>/s/^\(\s*\)AllowOverride .*$/\1AllowOverride AuthConfig FileInfo Limit/" \
	conf/httpd.conf
RUN line=`grep -n -m 1 "Listen 80" conf/httpd.conf | sed -e "s/:.*$//"` && \
	line=`expr $line + 1` && \
	sed -i -e "${line}i Listen 8081" conf/httpd.conf
EXPOSE 8081

FROM eclipse-temurin:23 AS build
WORKDIR /app
COPY ./ ./
WORKDIR /app/senseeact/ExampleSenSeeActService
RUN apt update && apt install curl -y
RUN curl -fsSL https://deb.nodesource.com/setup_20.x -o nodesource_setup.sh && chmod 755 nodesource_setup.sh && ./nodesource_setup.sh
RUN apt install nodejs -y
RUN npm install handlebars uglify-js -g
RUN npm install cssnano postcss postcss-cli --save-dev
RUN chmod 755 gradlew && sed -i 's/\r//' gradlew && ./gradlew webBuild

FROM httpd:2.4-alpine AS prod
ARG WEB_PATH
WORKDIR /usr/local/apache2
COPY senseeact/ExampleSenSeeActService/docker/senseeactweb/*.conf conf/extra/
COPY --from=build /app/senseeact/ExampleSenSeeActService/build/web-src/ htdocs${WEB_PATH}/
RUN sed -i \
	-e "s|var basePath = '.*';|var basePath = '${WEB_PATH}';|" \
	-e "s|var servicePath = '.*';|var servicePath = '/servlets/senseeact/v6.1.0';|" \
	htdocs${WEB_PATH}/index.html && \
	sed -i \
	-e "s/\$WEB_PATH/$escapedWebPath/g" \
	conf/extra/httpd-vhosts.conf
RUN sed -i \
	-e "s/^#\(LoadModule xml2enc_module\)/\1/" \
	-e "s/^#\(LoadModule proxy_html_module\)/\1/" \
	-e "s/^#\(LoadModule proxy_module\)/\1/" \
	-e "s/^#\(LoadModule proxy_http_module\)/\1/" \
	-e "s/^#\(LoadModule proxy_fcgi_module\)/\1/" \
	-e "s/^#\(LoadModule proxy_ajp_module\)/\1/" \
	-e "s/^#\(LoadModule rewrite_module\)/\1/" \
	-e "s/^#\(Include conf\/extra\/httpd-vhosts.conf\)/\1/" \
	-e "/<Directory \"\/usr\/local\/apache2\/htdocs\">/,/<\/Directory>/s/^\(\s*\)Options .*$/\1Options FollowSymLinks/" \
	-e "/<Directory \"\/usr\/local\/apache2\/htdocs\">/,/<\/Directory>/s/^\(\s*\)AllowOverride .*$/\1AllowOverride AuthConfig FileInfo Limit/" \
	conf/httpd.conf
RUN line=`grep -n -m 1 "Listen 80" conf/httpd.conf | sed -e "s/:.*$//"` && \
	line=`expr $line + 1` && \
	sed -i -e "${line}i Listen 8081" conf/httpd.conf
EXPOSE 8081

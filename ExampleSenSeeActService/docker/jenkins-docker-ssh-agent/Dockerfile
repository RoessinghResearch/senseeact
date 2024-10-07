FROM jenkins/ssh-agent:jdk21
ARG GROUP
ARG SSH_PRIVATE_KEY
ARG SSH_PUBLIC_KEY
ARG PROD_HOST
ENV SENSEEACT_PROD_HOST=$PROD_HOST
WORKDIR /home/jenkins
COPY $SSH_PRIVATE_KEY ./.ssh/id_rsa
COPY $SSH_PUBLIC_KEY ./.ssh/id_rsa.pub
RUN groupadd -g $GROUP docker && \
    adduser jenkins docker
RUN chown -R jenkins:jenkins .ssh && \
    chmod 700 .ssh && \
    chmod 600 .ssh/*
RUN su jenkins -c "ssh -o StrictHostKeyChecking=no jenkins@$PROD_HOST :"
RUN apt update && \
    apt -y install ca-certificates curl gnupg lsb-release && \
    mkdir -p /etc/apt/keyrings && \
    curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg && \
    echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian \
    "$(. /etc/os-release && echo "$VERSION_CODENAME")" stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null && \
    apt update && \
    apt -y install docker-ce docker-compose

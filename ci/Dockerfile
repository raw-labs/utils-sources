FROM sbtscala/scala-sbt:eclipse-temurin-jammy-21.0.2_13_1.9.9_2.12.19

SHELL ["/bin/bash", "-o", "pipefail", "-c"]

# To avoid sbt yelling about us running in /root
WORKDIR /root/workdir
# This layer might not change often
RUN set -ex \
  && apt-get update \
  && apt-get install -y \
    curl \
    unzip \
    zip \
  && rm -rf /var/lib/apt/lists/* \
  && curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip" \
  && unzip awscliv2.zip \
  && ./aws/install \
  && rm -rf awscliv2.zip aws

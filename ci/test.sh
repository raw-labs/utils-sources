#!/bin/bash -e

SCRIPT_HOME="$(cd "$(dirname "$0")"; pwd)"
COMPONENT_HOME="$(cd "${SCRIPT_HOME}/.."; pwd)"

cd "${COMPONENT_HOME}"

ENV_FILE_PATH="${COMPONENT_HOME}/.env"
ENV_FILE_S3_PATH="s3://rawlabs-credentials/tests/.env"

export GITHUB_TOKEN

# Check for GITHUB_TOKEN
if [ -z "$GITHUB_TOKEN" ]; then
  echo "Error: GITHUB_TOKEN is not set. Exiting..."
  exit 1
fi

if ! command -v aws &> /dev/null; then
  echo "AWS CLI not found. Installing..."
  curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip" || { echo "Failed to download AWS CLI"; exit 1; }
  unzip awscliv2.zip || { echo "Failed to unzip AWS CLI"; exit 1; }
  sudo ./aws/install || { echo "AWS CLI installation failed"; exit 1; }
  rm -rf awscliv2.zip aws
  echo "AWS CLI installation complete."
fi

if [ ! -s "$ENV_FILE_PATH" ]; then
  if ! aws s3 cp "$ENV_FILE_S3_PATH" "$ENV_FILE_PATH"; then
    echo "Warning: .env file not found. Some tests will not work."
  else
    echo ".env file successfully downloaded from S3."
  fi
fi

sbt clean test

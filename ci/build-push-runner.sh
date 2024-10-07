#!/usr/bin/env -S bash -e
SCRIPT_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

PUBLISH_REGISTRY=${PUBLISH_REGISTRY:-"ghcr.io"}
PUBLISH_REPOSITORY=${PUBLISH_REPOSITORY:-"${PUBLISH_REGISTRY}/raw-labs/utils-sources"}
BASE_REGISTRY=${BASE_REGISTRY:-"ghcr.io"}
IMAGE_NAME=${IMAGE_NAME:-"utils-ci-env"}
# read version from VERSION file
VERSION=${VERSION:-"0.1.0"}

IMAGE_FULL_TAG="${PUBLISH_REPOSITORY}/${IMAGE_NAME}:${VERSION}"

docker build . \
  -t ${IMAGE_FULL_TAG}

echo "Going to push ${IMAGE_FULL_TAG}"
if [ "$CI" != "true" ]; then
  while true; do
    read -p "Continue (y/n)? " choice
    case "$choice" in
      y|Y ) echo "Running docker build..."
            break;;
      n|N ) echo "Operation cancelled"
            exit 125;;
      * ) echo "Please answer y or n.";;
    esac
  done
fi

docker push $IMAGE_FULL_TAG

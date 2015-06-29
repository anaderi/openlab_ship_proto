import os
import re

import docker
from docker import Client

from lockfile import LockFile

from ..log import logger
from ..config import config



client = Client(base_url=config.DOCKER_URL, version=config.DOCKER_API_VERSION, timeout=config.DOCKER_TIMEOUT)


def pull_image(image, *args, **kwargs):
    with LockFile("/tmp/pull_lock_%s" % re.sub("[:/]", "_", image)):
        logger.debug("Pulling image {}".format(image))
        client.pull(image, *args, **kwargs)


def is_running(containter_id):
    running_ids = [c['Id'] for c in client.containers()]
    return containter_id in running_ids


def create_container(image, **kwargs):
    logger.debug("Creating container for image {} with arguments: {}".format(image, kwargs))
    c = client.create_container(image, **kwargs)
    return c['Id']


def start_container(container_id, **kwargs):
    attempts = 0
    while attempts < config.DOCKER_START_ATTEMPTS:
        logger.debug("Trying to start container id={}".format(container_id))
        try:
            client.start(container_id, **kwargs)
            break
        except Exception, e:
            logger.debug("Failed to start container id={}, error: {}".format(container_id, e))
            attempts += 1

    if attempts < config.DOCKER_START_ATTEMPTS:
        logger.debug("Started container id={}".format(container_id))
        return True
    else:
        raise Exception('Failed to start container id={}'.format(container_id))


def logs(container_id, **kwargs):
    return client.logs(container_id, **kwargs)


def remove(container_id, **kwargs):
    return client.remove_container(container_id, **kwargs)
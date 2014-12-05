import json
import datetime

import pymongo
from flask import request, jsonify
from flask.ext.restful import reqparse

from ..models import *
from ..rabbit import (
    rmq_push_to_queue,
    rmq_pull_from_queue,
    rmq_delete_queue,
    rmq_queue_length,
    rmq_declare,
)

from api import MetaschedulerResource, ExistingQueueResource, queue_exists


class QueueManagementResource(MetaschedulerResource):
    def get(self):
        jsoned_queues = []

        queues = Queue.objects().all()

        for queue in queues:
            q_dict = queue.to_dict()
            q_dict['length'] = rmq_queue_length(queue.job_type)

            jsoned_queues.append(q_dict)

        return {'queues': jsoned_queues}

    def put(self):
        queue_dict = json.loads(request.data)
        queue_name = queue_dict['job_type']

        if len(Queue.objects(job_type=queue_name)) > 0:
            raise Exception('Queue with same job_type already exists')


        queue = Queue(**queue_dict)
        queue.save()

        rmq_declare(queue_name)

        return {'queue': queue.to_dict()}



class QueueResource(ExistingQueueResource):
    def get(self, job_type):
        return {'job': rmq_pull_from_queue(job_type) }


    def post(self, job_type):
        descriptor = request.json.get('descriptor')
        assert descriptor

        rmq_declare(job_type)

        callback = request.json.get('callback')
        replicate = request.json.get('multiply') or 1

        jobs = [Job(job_type=job_type, descriptor=descriptor, callback=callback) for _ in xrange(replicate)]
        jobs = Job.objects.insert(jobs)


        for i in xrange(replicate):
            rmq_push_to_queue(job_type, json.dumps(jobs[i].to_dict()))

        if replicate == 1:
            return {'job': jobs[0].to_dict()}
        else:
            return {"job_ids": [str(j.pk) for j in jobs]}

    def delete(self, job_type):
        queue = Queue.objects.get(job_type=job_type)
        queue.delete()
        rmq_delete_queue(job_type)




class QueueInfoResource(MetaschedulerResource):
    def get(self, job_type):
        if not queue_exists(job_type):
            return {'exists': False}

        return {'length': rmq_queue_length(job_type), 'exists': True}

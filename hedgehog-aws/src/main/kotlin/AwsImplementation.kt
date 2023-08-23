package com.alexmherrmann.utils.hedgehog.aws;

import com.alexmherrmann.util.hedgehog.queue.Dealable
import com.alexmherrmann.util.hedgehog.queue.Squeese
import com.alexmherrmann.util.hedgehog.queue.fromJson
import java.util.*
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient


/**
 * Track a single SQS queue that has already been made
 */

class SimpleAwsImplementation<T>(
	queueName: String,
	region: Region = Region.US_WEST_2,
	val clazz: Class<T>) : Squeese<T> {
	private val client = SqsClient
		.builder()
		.region(region)
		.build()

	private val sqsUrl = client.getQueueUrl { it.queueName(queueName) }.queueUrl()

	private inner class SqsDealable<T>(val obj: T) : Dealable<T> {
		override fun markAsDealtWith() {
			client.deleteMessage { delete -> delete.queueUrl(sqsUrl) }
		}

		override fun getObj(): T = obj
	}

	override fun send(obj: T) {

	}

	override fun receive(): Optional<Dealable<T>> =
		client.receiveMessage { receive ->
			receive.queueUrl(sqsUrl)
		}.messages().firstOrNull()?.let { message ->
			Optional.of(SqsDealable(fromJson(message.body(), clazz)))
		} ?: Optional.empty()

}
package com.alexmherrmann.util.hedgehog.queue

import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Flow.Publisher


interface Received {
	fun isSuccessful(): Boolean
	fun getError(): Throwable?
}

/**
 * A library to help manage SQS queues
 *
 * Takes care of creation, JSON serialization, among others
 *
 * @param T The type of object that will be sent to, and received from the queue
 */
interface Squeese<T> {
	/**
	 * Send an object to the queue
	 *
	 * @param obj The object to send
	 */
	fun send(obj: T)

	/**
	 * Receive an object from the queue if there is one
	 *
	 * @return The object received
	 */
	fun receive(): Optional<T>
}

/**
 * Adds on to the Squeese interface to allow for some asynchronous operations
 */
interface AsyncSqueese<T> : Squeese<T> {
	/**
	 * Subscribe to the queue
	 */
	fun subscribe(subscriber: (T) -> Unit, onErr: (Throwable) -> Unit)
	fun subscribe(subscriber: (T) -> Unit)

	/**
	 * Get a Publisher that will emit objects as they are received from the queue
	 */
	fun asPublisher(): Publisher<T>

	/**
	 * Send an object in the background
	 */
	fun sendAsync(obj: T): CompletableFuture<Received>

}
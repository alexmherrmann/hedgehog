package com.alexmherrmann.util.hedgehog.queue

import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Flow.Publisher


interface SentResult {
	fun isSuccessful(): Boolean
	fun getError(): Throwable?
}

/**
 * Mark an object as having been dealt with
 */
interface Dealable<T> {
	/**
	 * Mark this object as having been dealt with,
	 * this could mean deleting from an SQS queue, or a no op if reading inherently removes it
	 */
	fun markAsDealtWith()

	/**
	 * The actual object from the queue
	 */
	fun getObj(): T;
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
	 * @implSpec This method should NOT pull from the queue unless called.
	 *
	 * @return The object received
	 */
	fun receive(): Optional<Dealable<T>>
}

/**
 * Asynchronously receive objects from the queue.
 *
 * @implSpec This class should NOT pull from the queue at all unless subscribed to.
 */
interface AsyncSqueeseReceiver<T> {
	/**
	 * Subscribe to the queue, automatically dealing with the item after the method runs
	 */
	fun subscribe(subscriber: (T) -> Unit, onErr: (Throwable) -> Unit)
	fun subscribe(subscriber: (T) -> Unit) = subscribe(subscriber) {}

	// The Java versions
	fun jSubscribe(subscriber: java.util.function.Consumer<T>, onErr: java.util.function.Consumer<Throwable>) {
		subscribe({ subscriber.accept(it) }, { onErr.accept(it) })
	}
	fun jSubscribe(subscriber: java.util.function.Consumer<T>) {
		subscribe { subscriber.accept(it) }
	}

	/**
	 * Get a Publisher that will emit objects as they are received from the queue
	 */
	fun asPublisher(): Publisher<Dealable<T>>
}

/**
 * Asynchronously send objects to the queue
 */
interface AsyncSqueeseSender<T> {
	/**
	 * Send an object in the background
	 */
	fun sendAsync(obj: T): CompletableFuture<SentResult>

}
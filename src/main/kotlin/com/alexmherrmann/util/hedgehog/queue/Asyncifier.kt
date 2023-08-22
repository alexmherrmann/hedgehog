package com.alexmherrmann.util.hedgehog.queue

import java.util.*
import java.util.concurrent.*
import java.util.concurrent.Flow.Subscriber
import java.util.concurrent.Flow.Subscription
import kotlin.math.min

private val pollers = Executors.newScheduledThreadPool(3)

object Asyncifier {
	private class SentResultImpl(
		private val error: Throwable? = null
	) : SentResult {
		override fun isSuccessful(): Boolean = error == null
		override fun getError(): Throwable? = error
	}

	private class AsyncSendWrapper<T>(
		private val squeese: Squeese<T>
	) : AsyncSqueeseSender<T> {
		private val MAX_QUE_SIZE = 100
		val toGoIntoSqueese: Queue<T> = ConcurrentLinkedQueue()

		override fun sendAsync(obj: T): CompletableFuture<SentResult> = CompletableFuture.supplyAsync({
			if (toGoIntoSqueese.size > MAX_QUE_SIZE) {
				return@supplyAsync SentResultImpl(IllegalStateException("Cannot take any more elements"))
			}

			try {
				toGoIntoSqueese.add(obj)
				return@supplyAsync SentResultImpl()
			} catch (e: Throwable) {
				return@supplyAsync SentResultImpl(e)
			}

		}, pollers)
		init {
			// Pull from our queue to send to the squeese
			pollers.scheduleAtFixedRate({
				// DO up to 5 sends to the squeese
				for (i in 0..5) {
					val obj: T = toGoIntoSqueese.poll() ?: break
					squeese.send(obj)
				}
			}, 0, 50, TimeUnit.MILLISECONDS)
		}

	}

	private class AsyncReceiveWrapper<T>(
		private val squeese: Squeese<T>
	) : AsyncSqueeseReceiver<T> {
		// TODO AH: buffer in some amount of

		private inner class AsyncSubscription(
			val subscriber: Subscriber<in Dealable<T>>
		) : Subscription {
			override fun request(n: Long) {
				pollers.submit {
					// Only give them 3 at a time
					for (i in 0..<min(n, 3)) {
						val obj: Dealable<T> = squeese.receive().orElse(null) ?: break
						subscriber.onNext(obj)
					}
				}
			}

			override fun cancel() {
				TODO()
			}
		}


		private val subscriptions: MutableList<AsyncSubscription> = mutableListOf()


		/**
		 * Give up to n items to subscribers
		 */
		fun giveTo(n: Long = 3) {
			// Go get from the squeese
			for (i in 0 until n) {
				val obj: Dealable<T> = squeese.receive().orElse(null) ?: break
				// Distribute to all subscribers
				for (subscription in subscriptions) {
					subscription.subscriber.onNext(obj)
				}
			}
		}

		init {


			// Pull from the squeese to give to subscribers
			pollers.scheduleAtFixedRate({
				giveTo()
			}, 0, 50, TimeUnit.MILLISECONDS)


		}

		val published: Flow.Publisher<Dealable<T>> = Flow.Publisher<Dealable<T>> { subscriber ->
			val subscription = AsyncSubscription(subscriber)
			subscriptions.add(subscription)

			subscriber?.onSubscribe(subscription)
		}

		override fun asPublisher(): Flow.Publisher<Dealable<T>> = published;

		override fun subscribe(subscriber: (T) -> Unit, onErr: (Throwable) -> Unit) {
			published.subscribe(object : Subscriber<Dealable<T>> {
				override fun onSubscribe(subscription: Subscription?) {
					subscription?.request(Long.MAX_VALUE)
				}

				override fun onNext(item: Dealable<T>?) {
					if (item != null) {
						subscriber(item.getObj())
						item.markAsDealtWith()
					}
				}

				override fun onError(throwable: Throwable?) {
					if (throwable != null) {
						onErr(throwable)
					}
				}

				override fun onComplete() {
				}
			})
		}
	}


	/**
	 * Take a simple implementation of Squeese and make it asynchronous
	 *
	 */
	@JvmStatic
	fun <T> asyncifySender(squeese: Squeese<T>): AsyncSqueeseSender<T> = AsyncSendWrapper(squeese)

	/**
	 * Take a simple implementation of Squeese and make it asynchronous
	 *
	 */
	@JvmStatic
	fun <T> asyncifyReceiver(squeese: Squeese<T>): AsyncSqueeseReceiver<T> = AsyncReceiveWrapper(squeese)
}
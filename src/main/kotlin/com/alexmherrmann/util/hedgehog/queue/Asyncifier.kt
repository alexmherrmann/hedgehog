package com.alexmherrmann.util.hedgehog.queue

import java.util.*
import java.util.concurrent.*
import java.util.concurrent.Flow.Subscriber
import java.util.concurrent.Flow.Subscription
import kotlin.math.min

private val pollers = Executors.newScheduledThreadPool(3)

object Asyncifier {
	private class ReceivedImpl(
		private val error: Throwable? = null
	) : Received {
		override fun isSuccessful(): Boolean = error == null
		override fun getError(): Throwable? = error
	}

	private class AsyncWrapper<T>(
		private val squeese: Squeese<T>
	) : AsyncSqueese<T>, Squeese<T> by squeese {

		private inner class AsyncSubscription(val subscriber: Subscriber<in T>) : Subscription {
			override fun request(n: Long) {
				pollers.submit {
					// Only give them 3 at a time
					for (i in 0..<min(n, 3)) {
						val obj: T = toGoIntoSqueese.poll() ?: break
						subscriber.onNext(obj)
					}
				}
			}

			override fun cancel() {
				TODO()
			}
		}


		private val subscriptions: MutableList<AsyncSubscription> = mutableListOf()
		private val MAX_QUE_SIZE = 150
		val toGoIntoSqueese: Queue<T> = ConcurrentLinkedQueue()


		/**
		 * Give up to n items to subscribers
		 */
		fun giveTo(n: Long = 3) {
			// Go get from the squeese
			for (i in 0 until n) {
				val obj: T = squeese.receive().orElse(null) ?: break
				// Distribute to all subscribers
				for (subscription in subscriptions) {
					subscription.subscriber.onNext(obj)
				}
			}
		}

		init {
			// Pull from our queue to send to the squeese
			pollers.scheduleAtFixedRate({
				// DO up to 5 sends to the squeese
				for (i in 0..5) {
					val obj: T = toGoIntoSqueese.poll() ?: break
					squeese.send(obj)
				}
			}, 0, 50, TimeUnit.MILLISECONDS)

			// Pull from the squeese to give to subscribers
			pollers.scheduleAtFixedRate({
				giveTo()
			}, 0, 50, TimeUnit.MILLISECONDS)


		}

		val published: Flow.Publisher<T> = Flow.Publisher<T> { subscriber ->
			val subscription = AsyncSubscription(subscriber)
			subscriptions.add(subscription)

			subscriber?.onSubscribe(subscription)
		}

		override fun asPublisher(): Flow.Publisher<T> = published;


		override fun sendAsync(obj: T): CompletableFuture<Received> = CompletableFuture.supplyAsync({
			if (toGoIntoSqueese.size > MAX_QUE_SIZE) {
				return@supplyAsync ReceivedImpl(IllegalStateException("Cannot take any more elements"))
			}

			try {
				toGoIntoSqueese.add(obj)
				return@supplyAsync ReceivedImpl()
			} catch (e: Throwable) {
				return@supplyAsync ReceivedImpl(e)
			}

		}, pollers)

		override fun subscribe(subscriber: (T) -> Unit) = subscribe(subscriber) {}

		override fun subscribe(subscriber: (T) -> Unit, onErr: (Throwable) -> Unit) {
			published.subscribe(object : Subscriber<T> {
				override fun onSubscribe(subscription: Subscription?) {
					subscription?.request(Long.MAX_VALUE)
				}

				override fun onNext(item: T?) {
					if (item != null) {
						subscriber(item)
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
	fun <T> asyncify(squeese: Squeese<T>): AsyncSqueese<T> = AsyncWrapper(squeese)
}
package com.alexmherrmann.util.hedgehog.queue;

import com.alexmherrmann.util.hedgehog.queue.Asyncifier
import com.alexmherrmann.util.hedgehog.queue.Dealable
import com.alexmherrmann.util.hedgehog.queue.Squeese
import io.kotest.assertions.timing.eventually
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import kotlin.test.Test
import kotlin.test.asserter
import kotlin.time.Duration.Companion.seconds

class QueueImpl<T> : Squeese<T> {
	private class DealableImpl<T>(
		private val obj: T,
	) : Dealable<T> {
		override fun markAsDealtWith() = Unit
		override fun getObj(): T = obj
	}
	val que = ArrayBlockingQueue<T>(200)
	override fun send(obj: T) {
		que.put(obj)
	}

	override fun receive(): Optional<Dealable<T>> = que.poll().toOptional().map { DealableImpl(it) }
}

class TestQueue {
	@Test
	fun simpleTest() {
		val queue = QueueImpl<Int>()
		queue.send(1)
		queue.send(2)
		queue.send(3)


		asserter.assertEquals("got 1", 1, queue.receive().get())
		asserter.assertEquals("got 2", 2, queue.receive().get())
		asserter.assertEquals("got 3", 3, queue.receive().get())
		asserter.assertEquals("got nothing", Optional.empty<Int>(), queue.receive())
		asserter.assertEquals("got nothing", Optional.empty<Int>(), queue.receive())

	}


	@Test
	fun asyncTest() {
		val queue = QueueImpl<Int>()
		val asyncQueue = Asyncifier.asyncifyReceiver(queue)
		var sum = 0
		asyncQueue.subscribe { sum += it }

		val asyncSender = Asyncifier.asyncifySender(queue)


		val range = 0..<50
		range.forEach {
			asyncSender
				.sendAsync(it)
				.thenAccept {
					it.isSuccessful() shouldBe true
				}
		}

		range.sum() shouldBe 1225


		runBlocking {
			eventually(1.seconds) {
				sum shouldBe 1225
			}
		}


	}
}

fun <T> T?.toOptional(): Optional<T> =
	Optional.ofNullable(this) as Optional<T>
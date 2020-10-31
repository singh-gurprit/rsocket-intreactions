package gameloft.interactions

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.retrieveAndAwait
import org.springframework.messaging.rsocket.retrieveFlow
import java.util.*
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

@SpringBootTest
class RsocketControllerTests {

	@Autowired
	private lateinit var builder: RSocketRequester.Builder

//	@Value("spring.rsocket.server.port")
	private var port: Int = 5555

	@Test
	fun requestResponse() {
		runBlocking {
			var requester = builder
					.connectTcp("localhost", port)
					.block()!!

			try {
				val randomUUID = UUID.randomUUID()
				val result  = requester
						.route("requestResponse")
						.data(randomUUID)
						.retrieveAndAwait<Transaction>()

				Assertions.assertEquals(randomUUID, result.id)
			} finally {
			    requester.rsocket()?.dispose()
			}
		}
	}

	@Test
	fun fireAndForget() {
		runBlocking {
			var requester = builder
					.connectTcp("localhost", port)
					.block()!!
			try {
				requester.route("fireAndForget").data("Gamelofter").send().block()
			} finally {
				requester.rsocket()?.dispose()
			}
		}
	}

	@Test
	fun channel() {
		runBlocking {
			var requester = builder
					.connectTcp("localhost", port)
					.block()!!

			val flowToSend = flow {
				repeat (20) {
					emit(UUID.randomUUID())
				}
			}

			try {
				requester.route("channel")
						.data(flowToSend)
						.retrieveFlow<Transaction>()
						.collect {
							println(it)
						}
			} finally {
				requester.rsocket()?.dispose()
			}
		}
	}

	@Test
	fun withThreads() {
		val time = measureTimeMillis {
			List(100_000) {
				thread {
					Thread.sleep(1000)
					println("${Thread.currentThread().name} Executed Thread: $it")
				}
			}.forEach {
				it.join()
			}
		}
		println("Total Time taken: $time")
	}

	@Test
	fun withCoroutines() {
		runBlocking {
			val time = measureTimeMillis {
				runBlocking {
					List(100_000) {
						launch {
							delay(1000)
							println("${Thread.currentThread().name} Executed Job: $it")
						}
					}
				}
			}
			println("Total time taken: $time")
		}
	}
}

package gameloft.interactions

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.retrieveAndAwait
import org.springframework.messaging.rsocket.retrieveFlow
import java.util.*

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


}

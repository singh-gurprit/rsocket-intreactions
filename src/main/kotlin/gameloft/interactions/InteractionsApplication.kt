package gameloft.interactions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.withContext
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service
import java.util.*

@SpringBootApplication
class InteractionsApplication

fun main(args: Array<String>) {
	runApplication<InteractionsApplication>(*args)
}

@Controller
class RSocketController(val rSocketService: RSocketService) {

	@MessageMapping("requestResponse")
	suspend fun requestResponse(id: UUID): Transaction {
		return rSocketService.getById(id)
	}

	@MessageMapping("fireAndForget")
	suspend fun fireAndForget(name: String) = coroutineScope {
		withContext(Dispatchers.Default) {// where we want to run our block of code?
			Thread.sleep(2000)
		}
		print("Thanks for the request $name, I will take a look :)")
	}

	@MessageMapping("stream")
	suspend fun stream(): Flow<Transaction> = flow {
		repeat(10) {
			emit(rSocketService.getById(UUID.randomUUID()))
		}
	}

	@MessageMapping("channel")
	suspend fun channel(channel: Flow<UUID>): Flow<Transaction> {
		return flow {
			channel.collect {
				println("Got Id: $it")
				emit(rSocketService.getById(UUID.randomUUID()))
				println("Respopnse sent")
			}
		}
	}
}

@Service
class RSocketService {
	fun getById(id: UUID) = randomTransaction(id)
}

fun randomTransaction(id: UUID?) = Transaction(
		id = id ?: UUID.randomUUID(),
		description = "Description"
)

data class Transaction(
		val id: UUID,
		val description: String? = null
)
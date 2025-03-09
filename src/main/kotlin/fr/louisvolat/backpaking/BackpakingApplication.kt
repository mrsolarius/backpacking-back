package fr.louisvolat.backpaking

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BackpakingApplication

fun main(args: Array<String>) {
	runApplication<BackpakingApplication>(*args)
}

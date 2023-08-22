package com.alexmherrmann.util.hedgehog.queue

import com.fasterxml.jackson.databind.ObjectMapper


val mapper = ObjectMapper()

fun toJson(obj: Any): String {
	return mapper.writeValueAsString(obj)
}

fun <T> fromJson(json: String, clazz: Class<T>): T {
	return mapper.readValue(json, clazz)
}
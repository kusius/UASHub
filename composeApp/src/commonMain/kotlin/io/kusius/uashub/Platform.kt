package io.kusius.uashub

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
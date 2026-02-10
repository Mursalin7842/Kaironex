package com.mursaline.kaironex

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
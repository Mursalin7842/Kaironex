package com.mursaline.kaironex.core

import java.util.Base64

actual fun encodeBase64(bytes: ByteArray): String {
    return Base64.getEncoder().encodeToString(bytes)
}

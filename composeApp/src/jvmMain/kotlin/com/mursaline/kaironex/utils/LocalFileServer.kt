package com.mursaline.kaironex.utils

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.net.InetSocketAddress

object LocalFileServer {
    private var server: HttpServer? = null
    var port: Int = 0
        private set

    // Fixed port for consistency with CEF security origin flags
    private const val FIXED_PORT = 18080

    fun start(assetRootDir: File) {
        if (server != null) return

        try {
            // Try fixed port first, fall back to random if busy
            server = try {
                HttpServer.create(InetSocketAddress("localhost", FIXED_PORT), 0)
            } catch (e: Exception) {
                println("⚠️ Port $FIXED_PORT busy, using random port")
                HttpServer.create(InetSocketAddress("localhost", 0), 0)
            }
            server?.createContext("/", AssetHandler(assetRootDir))
            server?.executor = null
            server?.start()
            
            port = server?.address?.port ?: 0
            println("LocalFileServer started on port $port serving ${assetRootDir.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        server?.stop(0)
        server = null
    }

    private class AssetHandler(val rootDir: File) : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            try {
                // Request Path (e.g., /interviewer/index.html)
                var path = exchange.requestURI.path
                if (path == "/") path = "/index.html"
                
                // Security: Prevent breaking out of root
                if (path.contains("..")) {
                    send403(exchange)
                    return
                }

                val file = File(rootDir, path)
                
                if (file.exists() && file.isFile) {
                    val mimeType = when (file.extension.lowercase()) {
                        "html" -> "text/html"
                        "js" -> "application/javascript"
                        "css" -> "text/css"
                        "png" -> "image/png"
                        "svg" -> "image/svg+xml"
                        "json" -> "application/json"
                        else -> "application/octet-stream"
                    }
                    
                    exchange.responseHeaders.add("Content-Type", mimeType)
                    // Enable CORS
                    exchange.responseHeaders.add("Access-Control-Allow-Origin", "*")
                    // Add Permissions-Policy header to allow microphone
                    exchange.responseHeaders.add("Permissions-Policy", "microphone=*")

                    exchange.sendResponseHeaders(200, file.length())
                    
                    val os: OutputStream = exchange.responseBody
                    val fs = FileInputStream(file)
                    fs.copyTo(os)
                    fs.close()
                    os.close()
                } else {
                    send404(exchange)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                try { exchange.sendResponseHeaders(500, 0) } catch (ignore: Exception) {}
            }
        }

        private fun send404(exchange: HttpExchange) {
            val response = "404 Not Found"
            exchange.sendResponseHeaders(404, response.length.toLong())
            val os = exchange.responseBody
            os.write(response.toByteArray())
            os.close()
        }

        private fun send403(exchange: HttpExchange) {
            val response = "403 Forbidden"
            exchange.sendResponseHeaders(403, response.length.toLong())
            val os = exchange.responseBody
            os.write(response.toByteArray())
            os.close()
        }
    }
}

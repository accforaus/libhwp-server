package com.accforaus.libhwp.server.exception

import java.lang.RuntimeException

class FileStorageException : RuntimeException {
    constructor(message: String) : super(message)

    constructor(message: String, e: Throwable) : super(message, e)
}

class MyFileNotFoundException : RuntimeException {
    constructor(message: String) : super(message)

    constructor(message: String, e: Throwable) : super(message, e)
}
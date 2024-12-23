package com.efedorchenko.timely.exception

class NetworkException(val httpStatus: Int, override val message: String) :
    RuntimeException(message)
package com.efedorchenko.timely.service

import com.efedorchenko.timely.model.auth.RegisterRequest

interface OnTryRegisterListener {

    fun tryRegister(registerRequest: RegisterRequest)

}
package com.efedorchenko.timely.service

interface OnTryLoginListener {

    fun tryLogin(loginData: Pair<String, String>)

}
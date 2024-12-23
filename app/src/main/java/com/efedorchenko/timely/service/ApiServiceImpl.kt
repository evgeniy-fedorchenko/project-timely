package com.efedorchenko.timely.service

import com.efedorchenko.timely.exception.NetworkException
import com.efedorchenko.timely.model.AuthRequest
import com.efedorchenko.timely.model.auth.AuthResponse
import com.efedorchenko.timely.model.auth.RegisterRequest
import com.efedorchenko.timely.security.SecurityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONException
import java.util.UUID
import javax.inject.Inject

class ApiServiceImpl @Inject constructor(
    private val securityService: SecurityService
) : ApiService {

    companion object {
        private const val RQUID = "RqUID"
        private const val AUTHORIZATION = "Authorization"
        private const val BASE_URL = "http://192.168.1.104:8080/api/v1"
        private const val REG_PATH = "/auth/reg"
        private const val LOGIN_PATH = "/auth/login"
        private val APPLICATION_JSON = "application/json".toMediaType()
    }

    private val client = OkHttpClient()

    override suspend fun login(authRequest: AuthRequest): Result<AuthResponse> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(BASE_URL + LOGIN_PATH)
                .header(RQUID, UUID.randomUUID().toString())
                .post(Json.encodeToString(authRequest).toRequestBody(APPLICATION_JSON))
                .build()

            val execute = execute<AuthResponse>(request)
            return@withContext execute
        }

    override suspend fun register(registerRequest: RegisterRequest): Result<AuthResponse> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(BASE_URL + REG_PATH)
                .header(RQUID, UUID.randomUUID().toString())
                .post(Json.encodeToString(registerRequest).toRequestBody(APPLICATION_JSON))
                .build()

            return@withContext execute<AuthResponse>(request)
        }

    private inline fun <reified T> execute(request: Request): Result<T> = runCatching {
        var response: Response? = null;
        try {

            response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw NetworkException(response.code, "Request is not successful. ")
            }
            val body = response.body?.string()
                ?: throw NetworkException(response.code, "Response body is null or empty")

            return Result.success(Json.decodeFromString<T>(body))
        } catch (ex: JSONException) {
            throw NetworkException(
                response?.code ?: 0,
                "Unexpected response body. Ex: " + ex.message
            )
        }
    }

}

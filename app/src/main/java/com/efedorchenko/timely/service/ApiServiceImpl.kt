package com.efedorchenko.timely.service

import android.util.Log
import com.efedorchenko.timely.data.EncProfileStorage
import com.efedorchenko.timely.model.api.ApiErrorCode
import com.efedorchenko.timely.model.api.ApiResponse
import com.efedorchenko.timely.model.auth.AuthResponse
import com.efedorchenko.timely.model.auth.Credentials
import com.efedorchenko.timely.model.auth.RegisterRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import javax.inject.Inject

class ApiServiceImpl @Inject constructor(
    private val encProfileStorage: EncProfileStorage
) : ApiService {

    companion object {

        /* Headers */
        private const val RQUID = "RqUID"
        private const val AUTHORIZATION = "Authorization"
        private val APPLICATION_JSON_MT = "application/json".toMediaType()

        /* Paths */
        private const val BASE_URL = "http://192.168.1.104:8080/api/v1"
        private const val REG_PATH = "/auth/reg"
        private const val LOGIN_PATH = "/auth/login"
    }

    private val client = OkHttpClient()

    override suspend fun login(credentials: Credentials): ApiResponse<AuthResponse> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(BASE_URL + LOGIN_PATH)
            .header(RQUID, UUID.randomUUID().toString())
            .post(Json.encodeToString(credentials).toRequestBody(APPLICATION_JSON_MT))
            .build()

        val execute = execute<AuthResponse>(request)
        return@withContext execute
    }

    override suspend fun register(registerRequest: RegisterRequest): ApiResponse<AuthResponse> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(BASE_URL + REG_PATH)
                .header(RQUID, UUID.randomUUID().toString())
                .post(Json.encodeToString(registerRequest).toRequestBody(APPLICATION_JSON_MT))
                .build()

            return@withContext execute<AuthResponse>(request)
        }

    private inline fun <reified T> execute(request: Request): ApiResponse<T> {
        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string()

            when (response.code) {
                in 200..299 -> handleSuccess<T>(body)
                400, in 404..408, in 410..499 -> handleClientError(body)
                401, 403, 409 -> handleAuthError<T>(body)
                else -> ApiResponse.Error(ApiErrorCode.SERVER)
            }

        } catch (e: Exception) {
            e.message?.let { Log.d("e", it) }
            ApiResponse.Error(ApiErrorCode.SERVER)
        }
    }

    private inline fun <reified T> handleSuccess(body: String?): ApiResponse<T> {
        val success = if (body.isNullOrEmpty()) {
            ApiResponse.Success()
        } else {
            ApiResponse.Success(Json.decodeFromString<T>(body))
        }
        return success
    }

    private inline fun <reified T> handleClientError(body: String?): ApiResponse<T> {
        val error = if (body.isNullOrEmpty()) {
            ApiResponse.Error(ApiErrorCode.VALIDATION)
        } else {
            ApiResponse.Error(
                apiErrorCode = ApiErrorCode.CLIENT,
                errorData = Json.decodeFromString<T>(body)
            )
        }
        return error
    }

    private inline fun <reified T> handleAuthError(body: String?): ApiResponse<T> {
        val error = if (body.isNullOrEmpty()) {
            ApiResponse.Error(ApiErrorCode.AUTH)
        } else {
            ApiResponse.Error(
                apiErrorCode = ApiErrorCode.AUTH,
                errorData = Json.decodeFromString<T>(body)
            )
        }
        return error
    }

}
/*
- 2xx + какие-то данные (json типа Т)
- 400 + какие-то данные (json типа ErrorResponse)
- 401, 403, 409 + какие-то данные (json типа AuthResponse)
- 401, 403 без данных (пустое тело)
- 500 + какие-то данные или без данных, если что-то сломалось
*/

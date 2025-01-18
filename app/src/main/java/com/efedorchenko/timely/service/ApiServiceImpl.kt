package com.efedorchenko.timely.service

import android.util.Log
import com.efedorchenko.timely.data.EncProfileStorage
import com.efedorchenko.timely.model.AbstractData
import com.efedorchenko.timely.model.DataRangeRequest
import com.efedorchenko.timely.model.DataType
import com.efedorchenko.timely.model.MembersResult
import com.efedorchenko.timely.model.api.ApiErrorCode
import com.efedorchenko.timely.model.api.ApiResponse
import com.efedorchenko.timely.model.auth.AuthResponse
import com.efedorchenko.timely.model.auth.Credentials
import com.efedorchenko.timely.model.auth.RegisterRequest
import com.efedorchenko.timely.model.auth.SpaceConnectResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.threeten.bp.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
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

        private const val REG_PATH = "$BASE_URL/auth/reg"
        private const val LOGIN_PATH = "$BASE_URL/auth/login"
        private const val DATA_PATH = "$BASE_URL/data"
        private const val DATA_PATTERN = "$BASE_URL/data/"
        private const val SPACE_PATH = "$BASE_URL/spaces"
        private const val KICK_PATH = "$BASE_URL/spaces/kick"

        /* Query parameters */
        private const val USER_ID_QPARAM_NAME = "userId"
        private const val SINCE_QPARAM_NAME = "since"
        private const val KEY_QPARAM_NAME = "key"
    }

    //    for dev
    private val client = OkHttpClient.Builder().readTimeout(1, TimeUnit.HOURS).build()

//    for prod
//    private val client = OkHttpClient()

    override suspend fun login(credentials: Credentials): ApiResponse<AuthResponse> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(LOGIN_PATH)
            .header(RQUID, UUID.randomUUID().toString())
            .post(Json.encodeToString(credentials).toRequestBody(APPLICATION_JSON_MT))
            .build()

        return@withContext execute<AuthResponse>(request)
    }

    override suspend fun register(registerRequest: RegisterRequest): ApiResponse<AuthResponse> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(REG_PATH)
                .header(RQUID, UUID.randomUUID().toString())
                .post(Json.encodeToString(registerRequest).toRequestBody(APPLICATION_JSON_MT))
                .build()

            return@withContext execute<AuthResponse>(request)
        }

    override suspend fun save(data: AbstractData): ApiResponse<AbstractData> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(DATA_PATH)
            .header(RQUID, UUID.randomUUID().toString())
            .header(AUTHORIZATION, getJwtToken())
            .post(Json.encodeToString(data).toRequestBody(APPLICATION_JSON_MT))
            .build()

        return@withContext execute<AbstractData>(request)
    }

    override suspend fun getMembers(): ApiResponse<MembersResult> = withContext(Dispatchers.IO) {
        return@withContext getMembers(SPACE_PATH.toHttpUrl())
    }

    override suspend fun getMembers(since: Instant?): ApiResponse<MembersResult> = withContext(Dispatchers.IO) {
        val urlBuilder = SPACE_PATH.toHttpUrl().newBuilder()
        since?.let { urlBuilder.addQueryParameter(SINCE_QPARAM_NAME, it.toString()) }
        return@withContext getMembers(urlBuilder.build())
    }

    private fun getMembers(url: HttpUrl): ApiResponse<MembersResult> {
        val request = Request.Builder()
            .url(url)
            .header(RQUID, UUID.randomUUID().toString())
            .header(AUTHORIZATION, getJwtToken())
            .get()
            .build()

        return execute<MembersResult>(request)
    }

    override suspend fun getRange(
        dataRangeRequest: DataRangeRequest, dataType: DataType
    ): ApiResponse<List<AbstractData>> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$DATA_PATTERN$dataType")
            .header(RQUID, UUID.randomUUID().toString())
            .header(AUTHORIZATION, getJwtToken())
            .post(Json.encodeToString(dataRangeRequest).toRequestBody(APPLICATION_JSON_MT))
            .build()

        return@withContext execute<List<AbstractData>>(request)
    }


    override suspend fun getUpdates(
        userId: String?, dataType: DataType, since: Instant?
    ): ApiResponse<List<AbstractData>> = withContext(Dispatchers.IO) {
        HttpUrl.Builder()

        val urlBuilder = "$DATA_PATTERN$dataType".toHttpUrl().newBuilder()
        userId?.let { urlBuilder.addQueryParameter(USER_ID_QPARAM_NAME, it) }
        since?.let { urlBuilder.addQueryParameter(SINCE_QPARAM_NAME, it.toString()) }

        val request = Request.Builder()
            .url(urlBuilder.build())
            .header(RQUID, UUID.randomUUID().toString())
            .header(AUTHORIZATION, getJwtToken())
            .get()
            .build()

        return@withContext execute<List<AbstractData>>(request)
    }

    override suspend fun connectToSpace(key: String): ApiResponse<SpaceConnectResponse> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(SPACE_PATH.toHttpUrl().newBuilder().addQueryParameter(KEY_QPARAM_NAME, key).build())
            .header(RQUID, UUID.randomUUID().toString())
            .header(AUTHORIZATION, getJwtToken())
            .patch(ByteArray(0).toRequestBody(null))  // Empty request body
            .build()

        return@withContext execute<SpaceConnectResponse>(request)
    }

    override suspend fun leaveSpace(): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(KICK_PATH)
            .header(RQUID, UUID.randomUUID().toString())
            .header(AUTHORIZATION, getJwtToken())
            .get()
            .build()

        return@withContext execute<Boolean>(request)
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
            ApiResponse.Error( // FIXME: посмотреть как мапить ошибки ErrorResponse
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

    private fun getJwtToken() = "Bearer ${encProfileStorage.getApiToken()}"

}

/*
- 2xx + какие-то данные (json типа Т)
- 400 + какие-то данные (json типа ErrorResponse)
- 401, 403, 409 + какие-то данные (json типа AuthResponse)
- 401, 403 без данных (пустое тело)
- 500 + какие-то данные или без данных, если что-то сломалось
*/

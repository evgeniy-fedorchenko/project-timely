package com.efedorchenko.timely.service

import android.util.Log
import androidx.core.util.Supplier
import com.efedorchenko.timely.data.EncUserProfile
import com.efedorchenko.timely.model.AbstractData
import com.efedorchenko.timely.model.DataRangeRequest
import com.efedorchenko.timely.model.DataType
import com.efedorchenko.timely.model.UserDataModifyDto
import com.efedorchenko.timely.model.api.ApiErrorCode
import com.efedorchenko.timely.model.api.ApiResponse
import com.efedorchenko.timely.model.auth.AuthResponse
import com.efedorchenko.timely.model.auth.Credentials
import com.efedorchenko.timely.model.auth.RegisterRequest
import com.efedorchenko.timely.model.auth.SpaceConnectResponse
import com.efedorchenko.timely.model.member.AcceptMember
import com.efedorchenko.timely.model.member.AcceptMemberResult
import com.efedorchenko.timely.model.member.MembersResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
    private val encUserProfile: EncUserProfile
) : ApiService {

    companion object {
        /* Headers */
        private const val RQUID = "RqUID"
        private const val AUTHORIZATION = "Authorization"
        private val APPLICATION_JSON_MT = "application/json".toMediaType()

        /* Paths */
        private const val BASE_URL = "http://192.168.1.104:8080/api/v1"

        //        private const val BASE_URL = "http://172.20.10.2:8080/api/v1"
        private const val REG_PATH = "$BASE_URL/auth/reg"
        private const val LOGIN_PATH = "$BASE_URL/auth/login"
        private const val DATA_PATH = "$BASE_URL/data"
        private const val DATA_RANGE_PATTERN = "$BASE_URL/data/%s/range"
        private const val DATA_UPDATES_PATTERN = "$BASE_URL/data/%s/updates"
        private const val SPACE_PATH = "$BASE_URL/spaces"
        private const val KICK_PATH = "$BASE_URL/spaces/kick"

        /* Query parameters */
        private const val USER_ID_QPARAM_NAME = "targetUserId"
        private const val SINCE_QPARAM_NAME = "since"
        private const val JOIN_REQS_QPARAM_NAME = "withJoinRequests"
        private const val KEY_QPARAM_NAME = "key"
        private const val START_RANGE_QPARAM_NAME = "start"
        private const val END_RANGE_QPARAM_NAME = "end"
        private const val REQ_USER_QPARAM_NAME = "requestedUserId"

        private val EMPTY_BODY = ByteArray(0).toRequestBody(null)
    }

    //    for dev
    private val client = OkHttpClient.Builder().readTimeout(1, TimeUnit.HOURS).build()

//    for prod
//    private val client = OkHttpClient()

    override suspend fun login(credentials: Credentials): ApiResponse<AuthResponse> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(LOGIN_PATH)
            .post(Json.encodeToString(credentials).toRequestBody(APPLICATION_JSON_MT))
            .build()

        return@withContext execute<AuthResponse>(request)
    }

    override suspend fun register(registerRequest: RegisterRequest): ApiResponse<AuthResponse> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(REG_PATH)
                .post(Json.encodeToString(registerRequest).toRequestBody(APPLICATION_JSON_MT))
                .build()

            return@withContext execute<AuthResponse>(request)
        }

    override suspend fun save(data: AbstractData): ApiResponse<AbstractData> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(DATA_PATH)
            .post(Json.encodeToString(data).toRequestBody(APPLICATION_JSON_MT))
            .build()

        return@withContext execute<AbstractData>(request)
    }

    override suspend fun change(data: UserDataModifyDto): ApiResponse<Unit> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(DATA_PATH)
            .patch(Json.encodeToString(data).toRequestBody(APPLICATION_JSON_MT))
            .build()

        return@withContext execute<Unit>(request)
    }

    override suspend fun getMembers(
        withJoinRequests: Boolean, since: Instant?
    ): ApiResponse<MembersResult> = withContext(Dispatchers.IO) {

        val urlBuilder = SPACE_PATH.toHttpUrl().newBuilder()
            .addQueryParameter(JOIN_REQS_QPARAM_NAME, withJoinRequests.toString())
        since?.let { urlBuilder.addQueryParameter(SINCE_QPARAM_NAME, it.toString()) }

        val request = Request.Builder().url(urlBuilder.build()).get().build()
        return@withContext execute<MembersResult>(request)
    }

    override suspend fun getRange(
        dataRangeRequest: DataRangeRequest, dataType: DataType
    ): ApiResponse<List<AbstractData>> = withContext(Dispatchers.IO) {

        val urlBuilder = DATA_RANGE_PATTERN.format(dataType).toHttpUrl().newBuilder()
            .addQueryParameter(START_RANGE_QPARAM_NAME, dataRangeRequest.startInclusive.toString())
            .addQueryParameter(END_RANGE_QPARAM_NAME, dataRangeRequest.endInclusive.toString())
        dataRangeRequest.requestedUserId?.let { urlBuilder.addQueryParameter(REQ_USER_QPARAM_NAME, it) }

        val request = Request.Builder().url(urlBuilder.build()).get().build()
        return@withContext execute<List<AbstractData>>(request)
    }

    override suspend fun getUpdates(
        userId: String?, dataType: DataType, since: Instant?
    ): ApiResponse<List<AbstractData>> = withContext(Dispatchers.IO) {

        val urlBuilder = DATA_UPDATES_PATTERN.format(dataType).toHttpUrl().newBuilder()
        userId?.let { urlBuilder.addQueryParameter(USER_ID_QPARAM_NAME, it) }
        since?.let { urlBuilder.addQueryParameter(SINCE_QPARAM_NAME, it.toString()) }

        val request = Request.Builder().url(urlBuilder.build()).get().build()
        return@withContext execute<List<AbstractData>>(request)
    }

    override suspend fun requestConnectToSpace(key: String):
            ApiResponse<SpaceConnectResponse> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(SPACE_PATH.toHttpUrl().newBuilder().addQueryParameter(KEY_QPARAM_NAME, key).build())
            .patch(EMPTY_BODY)  // Empty request body
            .build()

        return@withContext execute<SpaceConnectResponse>(request)
    }

    override suspend fun detachFromSpace(userId: String?): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        var url = KICK_PATH
        userId?.let {
            url = url.toHttpUrl()
                .newBuilder()
                .addQueryParameter(USER_ID_QPARAM_NAME, it)
                .build()
                .toString()
        }

        val request = Request.Builder().url(url).patch(EMPTY_BODY).build()
        return@withContext execute<Boolean>(request)
    }

    override suspend fun acceptMember(
        requestBody: AcceptMember
    ): ApiResponse<AcceptMemberResult> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(SPACE_PATH)
            .post(Json.encodeToString(requestBody).toRequestBody(APPLICATION_JSON_MT))
            .build()

        return@withContext execute<AcceptMemberResult>(request)

    }

    private inline fun <reified T> execute(request: Request, withJwt: Boolean = true): ApiResponse<T> {
        val rqUid = UUID.randomUUID().toString()

        return try {
            val builder = request.newBuilder().addHeader(RQUID, rqUid)
            if (withJwt) {
                builder.addHeader(AUTHORIZATION, "Bearer ${encUserProfile.getApiToken()}")
            }
            val response = client.newCall(builder.build()).execute()
            val body = response.body?.string()

            when (response.code) {
                in 200..299 -> handleSuccess<T>(body)
                400, in 404..408, in 410..499 -> handleClientError(body, rqUid)
                401, 403, 409 -> handleAuthError<T>(body, rqUid)
                else -> ApiResponse.Error(rqUid, ApiErrorCode.SERVER)
            }

        } catch (e: Exception) {
            e.message?.let { Log.d("e", it) }
            ApiResponse.Error(rqUid, ApiErrorCode.SERVER)
        }
    }

    private inline fun <reified T> handleSuccess(body: String?): ApiResponse<T> {
        return if (body.isNullOrEmpty()) ApiResponse.Success()
        else ApiResponse.Success(Json.decodeFromString<T>(body))
    }

    private inline fun <reified T> handleClientError(body: String?, rqUid: String?): ApiResponse<T> {
        return if (body.isNullOrEmpty()) {
            ApiResponse.Error(rqUid, ApiErrorCode.VALIDATION)
        } else {
            ApiResponse.Error( // FIXME: посмотреть как мапить ошибки ErrorResponse
                rqUid = rqUid,
                apiErrorCode = ApiErrorCode.CLIENT,
                errorData = Json.decodeFromString<T>(body)
            )
        }
    }

    private inline fun <reified T> handleAuthError(body: String?, rqUid: String?): ApiResponse<T> {
        return if (body.isNullOrEmpty()) {
            ApiResponse.Error(rqUid, ApiErrorCode.AUTH)
        } else {
            ApiResponse.Error(
                rqUid = rqUid,
                apiErrorCode = ApiErrorCode.AUTH,
                errorData = Json.decodeFromString<T>(body)
            )
        }
    }

    private fun Request.Builder.addAuthHeader(tokenSupplier: Supplier<String?>) {
        this.addHeader("Authorization", "Bearer ${tokenSupplier.get()}")
    }
}

/*
- 2xx + какие-то данные (json типа Т)
- 400 + какие-то данные (json типа ErrorResponse)
- 401, 403, 409 + какие-то данные (json типа AuthResponse)
- 401, 403 без данных (пустое тело)
- 500 + какие-то данные или без данных, если что-то сломалось
*/

package com.efedorchenko.timely.service

import com.efedorchenko.timely.data.EncProfileStorage
import com.efedorchenko.timely.data.ProfileStorage
import com.efedorchenko.timely.model.api.ApiErrorCode.AUTH
import com.efedorchenko.timely.model.api.ApiErrorCode.CLIENT
import com.efedorchenko.timely.model.api.ApiErrorCode.SERVER
import com.efedorchenko.timely.model.api.ApiErrorCode.VALIDATION
import com.efedorchenko.timely.model.api.ApiResponse
import com.efedorchenko.timely.model.api.Resource
import com.efedorchenko.timely.model.auth.Credentials
import com.efedorchenko.timely.model.auth.RegisterRequest
import javax.inject.Inject

class AuthServiceImpl @Inject constructor(
    private val encProfileStorage: EncProfileStorage,
    private val profileStorage: ProfileStorage,
    private val apiService: ApiService
) : AuthService {

    override suspend fun tryLogin(credentials: Credentials): Resource<Unit> {
        return when (val response = apiService.login(credentials)) {
            is ApiResponse.Success -> {
                response.data?.let {
                    if (it.authData == null || it.userData == null) {
                        Resource.Error("Network error")
                    } else {
                        profileStorage.saveUserData(it.userData)
                        encProfileStorage.saveAuthData(it.authData)
                        Resource.Success(it.userData.spaceName != null)
                    }
                } ?: Resource.Error("Network error")
            }

            is ApiResponse.Error -> {
                val message = when (response.apiErrorCode) {
                    VALIDATION -> ToastHelper.INCORRECT_LOGIN_DATA
                    SERVER -> ToastHelper.NETWORK_ERROR
                    AUTH -> response.errorData?.errorCode?.description ?: ToastHelper.INCORRECT_LOGIN_DATA
                    CLIENT -> response.errorData?.errorCode?.description ?: "Client error"
                }
                Resource.Error(message)
            }
        }
    }

    override suspend fun tryRegister(registerRequest: RegisterRequest): Resource<Unit> {
        return when (val response = apiService.register(registerRequest)) {
            is ApiResponse.Success -> {
                response.data?.let {
                    if (it.authData == null || it.userData == null) {
                        Resource.Error("Network error")
                    } else {
                        profileStorage.saveUserData(it.userData)
                        encProfileStorage.saveAuthData(it.authData)
                        Resource.Success(it.userData.spaceName != null)
                    }
                } ?: Resource.Error("Network error")
            }
            is ApiResponse.Error -> {
                val message = when (response.apiErrorCode) {
                    VALIDATION -> ToastHelper.INVALID_DATA_ON_REG
                    SERVER -> ToastHelper.NETWORK_ERROR
                    AUTH -> response.errorData?.errorCode?.description ?: "Authentication error"
                    CLIENT -> response.errorData?.errorCode?.description ?: "Client error"
                }
                Resource.Error(message)
            }
        }
    }
}

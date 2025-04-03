package com.efedorchenko.timely.service

import com.efedorchenko.timely.data.EncUserProfile
import com.efedorchenko.timely.data.SpaceViewModel
import com.efedorchenko.timely.data.UserProfile
import com.efedorchenko.timely.model.api.ApiErrorCode.AUTH
import com.efedorchenko.timely.model.api.ApiErrorCode.CLIENT
import com.efedorchenko.timely.model.api.ApiErrorCode.SERVER
import com.efedorchenko.timely.model.api.ApiErrorCode.VALIDATION
import com.efedorchenko.timely.model.api.ApiResponse
import com.efedorchenko.timely.model.api.Resource
import com.efedorchenko.timely.model.auth.Credentials
import com.efedorchenko.timely.model.auth.RegisterRequest
import com.efedorchenko.timely.model.member.SpaceConnectResultType
import javax.inject.Inject

class AuthServiceImpl @Inject constructor(
    private val encUserProfile: EncUserProfile,
    private val userProfile: UserProfile,
    private val apiService: ApiService,
    private val spaceViewModel: SpaceViewModel
) : AuthService {

    companion object {
        private const val NE_TAG = "ASI Network error"
    }

    override suspend fun tryLogin(credentials: Credentials): Resource<Unit> {
        return when (val response = apiService.login(credentials)) {
            is ApiResponse.Success -> {
                response.data?.let {
                    if (it.authData == null || it.userData == null) {
                        Resource.Error("Network error")
                    } else {
                        userProfile.setUserData(it.userData)
                        encUserProfile.setAuthData(it.authData)
                        Resource.Success(it.authData, it.userData)
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
                        userProfile.setUserData(it.userData)
                        encUserProfile.setAuthData(it.authData)
                        Resource.Success(it.authData, it.userData)
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

    override suspend fun requestConnectToSpace(key: String): SpaceConnectResultType =
        when (val response = apiService.requestConnectToSpace(key)) {
            is ApiResponse.Success -> {
                response.data?.let {
                    it.spaceDto?.let { space -> userProfile.setSpace(space) }
                    userProfile.setSpaceStatus(it.newSpaceStatus)
                    spaceViewModel.emitStatusChanged(it.newSpaceStatus)
                    it.result
                } ?: run { SpaceConnectResultType.UNKNOWN_ERROR }
            }

            is ApiResponse.Error -> {
                response.logErr(NE_TAG, "Cannot connected user to space [$key]")
                SpaceConnectResultType.UNKNOWN_ERROR
            }
        }
}

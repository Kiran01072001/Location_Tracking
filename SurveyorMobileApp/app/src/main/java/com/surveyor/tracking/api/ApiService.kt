package com.surveyor.tracking.api

import com.surveyor.tracking.model.LoginRequest
import com.surveyor.tracking.model.LoginResponse
import com.surveyor.tracking.model.LocationData
import com.surveyor.tracking.model.LiveLocationMessage
import com.surveyor.tracking.model.Surveyor
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    @POST("surveyors/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>
    
    @GET("surveyors")
    suspend fun getAllSurveyors(): Response<List<Surveyor>>
    
    @POST("live/location")
    suspend fun updateLocation(
        @Body location: LiveLocationMessage
    ): Response<okhttp3.ResponseBody>
    
    @GET("location/{surveyorId}/track")
    suspend fun getLocationHistory(
        @Path("surveyorId") surveyorId: String
    ): Response<List<LocationData>>
    
    @GET("location/{surveyorId}/latest")
    suspend fun getLatestLocation(
        @Path("surveyorId") surveyorId: String
    ): Response<LocationData>
    
    @GET("location/{surveyorId}/latest")
    suspend fun getLatestLocation(
        @Path("surveyorId") surveyorId: String
    ): Response<LocationData>
}

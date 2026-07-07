package com.example.t_learnappmobile.data.auth

import android.content.Context
import android.net.ConnectivityManager
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import kotlin.jvm.Throws


class NetworkMonitorInterceptor constructor(
    private val context: Context
) : Interceptor {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @Throws(NoNetworkException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()
        if (isConnected()) {
            return chain.proceed(request)
        } else {
            throw NoNetworkException("Network Error")
        }
    }

    private fun isConnected() = connectivityManager.activeNetwork != null

}
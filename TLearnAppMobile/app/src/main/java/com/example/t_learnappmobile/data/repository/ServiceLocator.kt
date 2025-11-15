package com.example.t_learnappmobile.data.repository

import com.example.t_learnappmobile.domain.repository.WordRepository
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ServiceLocator {
    private val retrofitBuilder : Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8080/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: WordApi by lazy {
        retrofitBuilder.create(WordApi::class.java)
    }
    val storage: WordsStorage by lazy {
        WordsStorage()
    }

    val wordRepository: WordRepository by lazy {
        WordRepositoryImpl(api = api, storage = storage)
    }
}



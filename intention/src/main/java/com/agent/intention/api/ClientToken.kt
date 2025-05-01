package com.agent.intention.api

data class ClientApiGetTokenRes(
    val code: Int,
    val msg: String,
    var status: String,
    val data: String,
)
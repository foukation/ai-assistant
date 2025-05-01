package com.agent.intention.api

data class ClientTimeRes(
    val code: Int,
    val msg: String,
    val data: ClientTimeData,
)

data class ClientTimeData(
    val standard_time: String,
    val chinese_time: String,
    val weekDay: String,
    val time_zone: String,
)
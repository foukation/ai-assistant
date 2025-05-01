package com.agent.intention.api

data class TripContentRes(
    val code: Int,
    val msg: String,
    val data: TripListData,
)

data class TripListData(
    val count: Int,
    val pageIndex: Int,
    val pageSize: Int,
    val list: ArrayList<TripItemContent>?
)

data class TripItemContent(
    val id: Int,
    val title: String,
    val description: String,
    val command: String,
    val status: Int,
    val sub_status1: Int,
    val sub_status2: Int,
    val createdAt: String,
    val result: TripResultContent?,
    val all_result: TripAllResultContent?,
)

data class TripResultContent(
    val file_url: String,
)

data class  TripAllResultContent(
    val poi_search_result: PoiSearchResultContent,
    val status_summary_info: String,
)

data class PoiSearchResultContent(
    val top1_poi_recommend_reason: String,
    val title: String,
    val sub_title: String,
    val poi_list: ArrayList<PoiItem>
)

data class PoiItem(
    val name: String,
    val address: String,
    val opentime_week: String,
    val rating: String,
    val poi_photos: ArrayList<String>,
    val type: String,
    val recommend_reason: String,
)

data class TripDelRes(
    val code: Int,
    val msg: String,
)

data class TripCreateParams(
    val title: String,
    val description: String,
    val command: String,
)

data class TripCreateRes(
    val code: Int,
    val msg: String,
    val data: TripCreateData,
)

data class TripCreateData(
    val taskId: Int,
)


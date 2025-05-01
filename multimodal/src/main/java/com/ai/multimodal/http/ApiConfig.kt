package com.ai.multimodal.http

object ApiConfig {

    const val TIMEOUT: Long = 15000L

    /*const val IMAGE_RECOGNITION_BASE_URL = "http://36.134.128.162:11435"*/
    const val IMAGE_RECOGNITION_BASE_URL = "http://36.213.71.200:5692"
    const val QUESTION_ANSWER_BASE_URL = "http://36.213.71.163:11507"

    const val MULTIMODAL_API = "/multimodal"
    /*const val MULTIMODAL_API = "/med_extract_2"*/

    const val QUESTION_ANSWER_API = "/completions"

    const val MED_REMINDERS_API = "/med-reminders"

}
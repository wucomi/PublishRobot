package com.wucomi.publishrobot

data class PgyUploadResponse(
    val code: Int,
    val message: String,
    val data: PgyData?
)

data class PgyData(
    val buildShortcutUrl: String?
)
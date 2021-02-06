package com.wucomi.publishrobot

import com.android.build.gradle.api.ApplicationVariant
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.IOException
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


open class PublishTask : DefaultTask() {

    @Input
    lateinit var variant: ApplicationVariant

    @Input
    lateinit var extensions: PublishExtension

    @TaskAction
    fun publish() {
        val apkPath = variant.outputs.firstOrNull()?.outputFile?.absolutePath
        val pgyApiKey = extensions.pgyApiKey
        val dingTalkWebhook = extensions.dingTalkWebhook
        val dingTalkSecret = extensions.dingTalkSecret

        // 上传蒲公英
        val uploadResult = uploadToPgy(apkPath, pgyApiKey)
        // 推送到钉钉
        notifyDingTalk(dingTalkWebhook, dingTalkSecret, uploadResult)
    }

    private fun uploadToPgy(filePath: String?, apiKey: String?): String? {
        if (apiKey == null) {
            return null
        }
        requireNotNull(filePath) { "filePath 为空" }
        val file = File(filePath)
        if (!file.exists() || file.isDirectory) {
            throw IllegalArgumentException("$filePath 不是文件")
        }

        val client = OkHttpClient()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                RequestBody.create("application/octet-stream".toMediaTypeOrNull(), file)
            )
            .addFormDataPart("_api_key", apiKey)
            .build()

        val request = Request.Builder()
            .url("https://www.pgyer.com/apiv2/app/upload")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected code ${response.code}")
            }
            val responseBody = response.body?.string() ?: ""
            val pgyResponse = Gson().fromJson(responseBody, PgyUploadResponse::class.java)
            if (pgyResponse.code == 0) {
                println("The pgy upload is successful")
                return "https://www.pgyer.com/${pgyResponse.data?.buildShortcutUrl}"
            } else {
                println("The pgy upload is failed: $responseBody")
                return "https://www.pgyer.com/"
            }
        }
    }

    private fun notifyDingTalk(webhook: String?, secret: String?, downloadUrl: String?) {
        if (webhook == null) {
            return
        }

        val client = OkHttpClient()
        val payload = mapOf(
            "msgtype" to "actionCard",
            "actionCard" to mapOf(
                "text" to "Android打包完成，欢迎下载使用！",
                "singleTitle" to "了解详情",
                "singleURL" to downloadUrl
            )
        )

        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            Gson().toJson(payload)
        )

        // 如果有 secret，需要对请求进行签名
        var url = webhook
        if (secret != null) {
            val timestamp = System.currentTimeMillis().toString()
            val sign = getSign(secret)
            url = "$webhook&timestamp=$timestamp&sign=$sign"
        }
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected code ${response.code}")
            }
            val responseBody = response.body?.string() ?: ""
            val dingTalkResponse = Gson().fromJson(responseBody, DingTalkResponse::class.java)
            if (dingTalkResponse.errcode == 0) {
                println("The DingTalk message is sent successfully")
            } else {
                println("The DingTalk message is sent failed: $responseBody")
            }
        }
    }

    private fun getSign(secret: String): String {
        try {
            val timestamp = System.currentTimeMillis()
            val stringToSign = """
            $timestamp
            $secret
            """.trimIndent()
            val mac: Mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
            val signData: ByteArray = mac.doFinal(stringToSign.toByteArray(StandardCharsets.UTF_8))
            val sign =
                URLEncoder.encode(Base64.getEncoder().encodeToString(signData), StandardCharsets.UTF_8.toString())
            return sign
        } catch (e: Exception) {
            throw RuntimeException("DingTalk signature failed", e)
        }
    }
}
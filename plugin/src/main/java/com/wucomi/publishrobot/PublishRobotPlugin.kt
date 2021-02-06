package com.wucomi.publishrobot

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import org.gradle.api.Plugin
import org.gradle.api.Project

const val TAG = "PublishRobotPlugin: "

class PublishRobotPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // 创建PublishExtension扩展对象
        // 注意这里只是创建扩展对象, 只有当配置阶段执行完毕后配置对象才会被赋值
        val extensions = project.extensions.create("publishRobot", PublishExtension::class.java)
        // 创建皮肤打包Task, 在配置阶段结束后创建, 这样才能拿到扩展中的值
        project.afterEvaluate {
            val androidExtension = project.extensions.findByType(AppExtension::class.java)
            androidExtension?.applicationVariants?.all { variant ->
                // 为每个变体创建上传任务
                createPublishTask(project, extensions, variant)
            }
        }
    }
}

private fun createPublishTask(project: Project, extensions: PublishExtension, variant: ApplicationVariant) {
    val task = project.tasks.register(
        "publish${variant.name.replaceFirstChar { it.uppercaseChar() }}",
        PublishTask::class.java
    ) {
        it.group = "PublishRobot"
        it.description = "Publish for variant: ${variant.name}"
        it.variant = variant
        it.extensions = extensions
    }
    // 设置依赖关系，依赖打包任务
    task.configure {
        it.dependsOn(variant.assembleProvider)
    }
}
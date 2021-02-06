# 自动发布机器人

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

#### 介绍

自动发布机器人，自动上传到蒲公英和发送钉钉机器人消息

![screenshot_2025-02-25_14-08-11.png](https://s2.loli.net/2025/02/25/Nsm8eHfZxzGrE7K.png)

![8D350820-28AA-407f-AA4A-9AD4A8387FD2.png](https://s2.loli.net/2025/02/25/bRfZ7TSu9jxKJ5a.png)

#### 安装教程

1. 在项目的 `setting.gradle` 文件中，添加三方库的依赖项。

    ```groovy
    dependencyResolutionManagement {
        repositories {
            maven { url 'https://jitpack.io' }
        }
    }
    ```

2. 在项目module的 `build.gradle` 文件中，添加三方库的依赖项。

    ```groovy
    plugins {
        id 'com.github.wucomi.PublishRobot' version '1.0.0'
    }
    
    publishRobot {
        pgyApiKey pgyApiKey
        dingTalkSecret dingTalkSecret
       dingTalkWebhook "https://oapi.dingtalk.com/robot/send?access_token=$dingTalkToken"
    }
    ```

#### 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request

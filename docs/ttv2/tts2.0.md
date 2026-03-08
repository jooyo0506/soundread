# 火山引擎语音合成双向流式API对接指南（豆包语音合成模型2.0 ）

## 1. 概述

本文档针对**豆包语音合成模型2.0（seed-tts-2.0，字符版）**，提供基于Java的双向流式WebSocket调用指南。服务会自动整理文本、平衡延迟与合成效果，推荐将大模型流式输出的文本直接传入。

## 2. 接入准备

调用前需获取以下信息（火山引擎控制台获取）：

| 参数         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| APP ID       | 控制台获取的APP ID                                           |
| Access Token | 控制台获取的Access Token                                     |
| Resource ID  | 固定为 `seed-tts-2.0`（豆包语音合成模型2.0字符版）           |
| 音色ID       | 期望使用的音色，参考[大模型音色列表](https://www.volcengine.com/docs/6561/1257544) |

## 3. WebSocket连接与鉴权

- **服务地址**：`wss://openspeech.bytedance.com/api/v3/tts/bidirection`
- **建连头**（HTTP Upgrade请求头）：

| Header                                | 必填 | 示例                             |
| ------------------------------------- | ---- | -------------------------------- |
| X-Api-App-Key                         | 是   | `123456789`                      |
| X-Api-Access-Key                      | 是   | `your-access-token`              |
| X-Api-Resource-Id                     | 是   | `seed-tts-2.0`                   |
| X-Api-Connect-Id                      | 否   | `UUID`（建议生成唯一值用于追踪） |
| X-Control-Require-Usage-Tokens-Return | 否   | `*`（开启用量返回）              |

成功握手后，响应头会包含`X-Tt-Logid`，请记录以便排查。

## 4. WebSocket二进制协议

所有数据使用二进制帧传输，帧结构如下：

| 字节偏移   | 字段                      | 说明                                   |
| ---------- | ------------------------- | -------------------------------------- |
| 0（高4位） | Protocol version          | 固定`0b0001`（v1）                     |
| 0（低4位） | Header size               | 固定`0b0001`（4字节头）                |
| 1          | Message type + flags      | 见下文                                 |
| 2（高4位） | Serialization             | `0b0000`=raw，`0b0001`=JSON            |
| 2（低4位） | Compression               | `0b0000`=无压缩                        |
| 3          | Reserved                  | 固定`0b00000000`                       |
| [4~7]      | Event number              | 如果flags指示包含event，则为4字节int32 |
| [8~11]     | Connection/Session ID长度 | 可选                                   |
| [12~...]   | Connection/Session ID     | 实际ID字符串                           |
| ...        | Payload长度               | 4字节uint32                            |
| ...        | Payload                   | JSON字符串或音频二进制                 |

**Message type & flags**（常用）：

| Message type | 含义       | flags  | 是否含Event | 备注                             |
| ------------ | ---------- | ------ | ----------- | -------------------------------- |
| 0b0001       | 客户端请求 | 0b0100 | 是          | 用于StartSession、TaskRequest等  |
| 0b1001       | 服务端响应 | 0b0100 | 是          | 包含事件信息（如SessionStarted） |
| 0b1011       | 纯音频响应 | 0b0100 | 是          | 音频数据                         |
| 0b1111       | 错误信息   | 无     | 否          | payload为错误JSON                |

## 5. 交互流程

```
客户端                            服务端
  |-------- StartConnection ------->|
  |<----- ConnectionStarted --------|
  |-------- StartSession ----------->|  携带合成参数
  |<------ SessionStarted -----------|
  |-------- TaskRequest ------------->|  携带待合成文本
  |<------ 音频帧 + 字幕事件 ---------|  持续返回
  |-------- FinishSession ----------->|
  |<------ SessionFinished -----------|  可能含用量
  |-------- FinishConnection -------->|
  |<----- ConnectionFinished ---------|
```

- **同一连接**支持多次会话（多个StartSession/FinishSession），但不支持并发。
- **取消会话**：可发送`CancelSession`（事件101），服务端释放资源，需重新StartSession。

## 6. 请求参数详解（模型2.0重点）

StartSession事件（事件100）的payload为JSON，包含合成参数。

### 6.1 必填参数

```json
{
    "user": { "uid": "your-uid" },
    "event": 100,
    "req_params": {
        "text": "待合成文本",
        "speaker": "音色ID",
        "audio_params": {
            "format": "mp3",
            "sample_rate": 24000
        }
    }
}
```

### 6.2 音频参数（audio_params）

| 字段                | 类型   | 默认值 | 说明                                                         |
| ------------------- | ------ | ------ | ------------------------------------------------------------ |
| format              | string | mp3    | 可选mp3/ogg_opus/pcm（wav在流式场景下会重复头，建议pcm）     |
| sample_rate         | number | 24000  | 8000/16000/22050/24000/32000/44100/48000                     |
| bit_rate            | number | -      | 比特率，MP3/ogg建议主动设置（如64000）                       |
| emotion             | string | -      | 情感（部分音色支持）                                         |
| emotion_scale       | number | 4      | 情感强度1~5                                                  |
| speech_rate         | number | 0      | 语速[-50,100]（100为2倍速）                                  |
| loudness_rate       | number | 0      | 音量[-50,100]                                                |
| **enable_subtitle** | bool   | false  | **模型2.0专用**：开启字幕，通过TTSSubtitle事件返回原文时间戳 |

### 6.3 扩展参数（additions，模型2.0特色）

| 字段                    | 类型     | 说明                                                         |
| ----------------------- | -------- | ------------------------------------------------------------ |
| **context_texts**       | string[] | 辅助情感/语速调整。**本项目已由后端接管解析前端的 `[#全局语气]` 并自动填充此数组** |
| **section_id**          | string   | 引用其他会话的session_id提供上下文（最长30轮/10分钟）        |
| **use_tag_parser**      | bool     | 是否开启cot标签解析。**本系统后端强制设为true，自动将 `【局部表情】` 转译为 `<cot>` 标签以确保兼容** |
| silence_duration        | number   | 句尾增加静音时长（0~30000ms）                                |
| disable_markdown_filter | bool     | true则忽略Markdown语法（如`**`）                             |
| explicit_language       | string   | 明确语种，如"zh-cn"、"en"                                    |
| cache_config            | object   | 缓存配置（use_cache等）                                      |
| post_process.pitch      | int      | 音调调整[-12,12]                                             |

## 7. 响应处理

### 7.1 音频帧

- Message type = 0b1011
- payload为音频二进制数据

### 7.2 事件帧

Message type = 0b1001，payload为JSON，根据event字段处理：

| 事件码  | 名称             | 说明                                      |
| ------- | ---------------- | ----------------------------------------- |
| 150     | SessionStarted   | 会话开始成功                              |
| 152     | SessionFinished  | 会话结束，可能含用量`usage.text_words`    |
| 153     | SessionFailed    | 会话失败                                  |
| 350     | TTSSentenceStart | 一句合成开始（仅标记）                    |
| 351     | TTSSentenceEnd   | 一句合成结束（模型2.0不返回时间戳）       |
| **352** | **TTSSubtitle**  | **模型2.0专用**：字幕事件，包含原文时间戳 |

**TTSSubtitle示例**：
```json
{
    "text": "2019年1月8日，软件2.0版本发布。",
    "words": [
        {"word": "2019", "startTime": 0.585, "endTime": 0.615},
        {"word": "年", "startTime": 0.615, "endTime": 0.845},
        ...
    ]
}
```

### 7.3 错误帧

Message type = 0b1111，payload JSON含`status_code`和`message`。

## 8. 错误码

| 错误码   | 含义              |
| -------- | ----------------- |
| 20000000 | 成功              |
| 45000001 | 请求参数错误      |
| 45000000 | 客户端通用错误    |
| 55000000 | 服务端错误        |
| 55000001 | 服务端session错误 |

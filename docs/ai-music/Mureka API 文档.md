# Mureka API 文档

## 1. 平台简介
Mureka 是全球领先的 AI 音乐生成产品，拥有自主研发的音乐生成模型，面向全球近千万用户提供服务。除大众创作平台外，Mureka 提供 B 端 API 集成与内容解决方案，覆盖社交、语音聊天、游戏、教育、客服、播客、有声书、娱乐、媒体等行业，以及音乐软件开发商、创意工具提供商、应用开发者、AI 科技公司、游戏工作室等客户。

## 2. 核心服务
Mureka API 目前提供三大类服务：

| 服务类型             | 描述                                                         |
| -------------------- | ------------------------------------------------------------ |
| **标准音乐生成 API** | 覆盖歌曲生成、纯音乐生成、歌词生成、歌曲续写四大核心功能，满足从创意到完整作品的全流程需求。 |
| **模型微调**         | 支持企业基于 200 首具有一致特征的曲目微调专属模型，实现特定风格或独特特征的稳定音乐生成。 |
| **内容服务**         | 无需开发，直接获取成品音乐内容（如流媒体配乐、视频配乐），可基于热门歌曲风格快速生成近似内容，降低采购成本。 |

## 3. 模型特点
| 模型            | 特点                                                         |
| --------------- | ------------------------------------------------------------ |
| **Mureka V7.5** | 旋律与编曲质量优异，人声与乐器音色逼真；支持流式输出，实时试听，提升创作效率。 |
| **Mureka O1**   | 通过算法甄选最佳生成作品，情感表达细腻；支持多语言创作、情境化背景音乐生成及 AI 音乐编辑。 |

**支持语言**：歌曲生成支持以下 10 种语言：
- 中文
- 英文
- 日语
- 韩语
- 葡萄牙语
- 西班牙语
- 德语
- 法语
- 意大利语
- 俄语

> 提示：您可以在歌词和提示词中使用上述语言，模型将根据输入的语言生成相应人声和风格的音乐。vocal 功能增强后，`vocal_id` 和 `prompt` 可同时控制，实现更精准的多语言人声合成。

## 4. 优势
- **官方可靠**：全球首个官方 AI 音乐 API 平台，核心研发团队直接技术支持。
- **轻松集成**：提供清晰文档、示例代码、快速上手指南，数小时内完成接入。
- **稳定输出**：API 与数百万用户信赖的生产模型同步，迭代经过严格测试，输出稳定可扩展。

## 5. 快速入门

### 5.1 创建账户
请访问 [Mureka 平台](https://www.mureka.cn) 点击“登录”/“注册”按钮，按照指引创建账户。

### 5.2 获取 API 密钥
登录后，在平台导航栏进入 **“API Keys”** 页面，查看或生成您的 API 密钥。  
> ⚠️ **重要**：API 密钥是您的机密凭证，请勿与他人共享，也**不要**在客户端代码（浏览器、移动 App 等）中暴露。

### 5.3 认证
所有 API 请求均需在 HTTP 头中携带您的 API 密钥，格式如下：
```
Authorization: Bearer MUREKA_API_KEY
```
将 `MUREKA_API_KEY` 替换为您的实际密钥。

### 5.4 服务器地址
```
https://api.mureka.cn
```

### 5.5 发送第一个请求
以下示例使用 `curl` 调用歌曲生成接口。请将 `$MUREKA_API_KEY` 替换为您的密钥。

```bash
curl https://api.mureka.cn/v1/song/generate \
  -H "Authorization: Bearer $MUREKA_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "lyrics": "[Verse]\n在暴风雨的夜晚，我独自徘徊\n迷失在雨中，感觉像是被抛弃\n你的回忆，它们在我眼前闪烁\n希望有那么一刻，能找到一些幸福",
    "model": "auto",
    "prompt": "r&b, slow, passionate, male vocal"
  }'
```

**请求参数说明**：
- `lyrics` (string, 必填)：歌词文本，支持换行符（`\n`）。可使用支持的多语言。
- `model` (string, 可选)：指定模型版本，如 `"auto"`（自动选择）、`"mureka-v7.5"`、`"mureka-o1"` 等。不填或 `"auto"` 由系统选择最优模型。
- `prompt` (string, 可选)：描述音乐风格的提示词，例如流派、速度、情绪、人声类型等。

**成功响应示例**：
```json
{
  "id": "1436211",
  "created_at": 1677610602,
  "model": "mureka-6",
  "status": "preparing",
  "trace_id": "1e94aba5a2de4cc4bff54a2813c8d36c"
}
```
- `id`：生成任务 ID，可用于后续查询任务状态或获取结果。
- `created_at`：任务创建时间戳（Unix 秒）。
- `model`：实际使用的模型版本。
- `status`：任务状态，如 `preparing`（准备中）、`processing`（处理中）、`completed`（完成）、`failed`（失败）。
- `trace_id`：请求追踪 ID，用于技术支持时排查问题。

## 6. API 接口

### 6.1 文件上传与管理

#### 6.1.1 上传文件
上传一个可以在多个接口使用的文件。单个文件大小最多为 10 MB。

**请求**
- 方法：`POST`
- 路径：`/v1/files/upload`
- 认证：Bearer Token
- 请求体：`multipart/form-data`

| 参数名    | 类型   | 必填 | 描述                                     |
| --------- | ------ | ---- | ---------------------------------------- |
| `file`    | file   | 是   | 要上传的文件内容（二进制），不是文件名。 |
| `purpose` | string | 是   | 文件的预期用途，枚举值见下表。           |

**purpose 枚举值说明**：

| 用途           | 支持格式      | 时长要求                  | 说明                               |
| -------------- | ------------- | ------------------------- | ---------------------------------- |
| `reference`    | mp3, m4a      | [30,30]秒（超出会被裁剪） | 参考音频，用于风格参考等。         |
| `vocal`        | mp3, m4a      | [15,30]秒（超出会被裁剪） | 提取人声，用于人声克隆或参考。     |
| `melody`       | mp3, m4a, mid | [5,60]秒（超出会被裁剪）  | 提取哼唱旋律，建议上传midi文件。   |
| `instrumental` | mp3, m4a      | [30,30]秒（超出会被裁剪） | 纯乐器伴奏。                       |
| `voice`        | mp3, m4a      | [5,15]秒（超出会被裁剪）  | 简短人声样本。                     |
| `audio`        | mp3, m4a      | 无特定时长限制            | 通用音频文件，用于歌曲续写等功能。 |

**请求示例**
```bash
curl https://api.mureka.cn/v1/files/upload \
  -H "Authorization: Bearer $MUREKA_API_KEY" \
  -F purpose="reference" \
  -F file=@/path/to/your/audio.mp3
```

**响应示例**
```json
{
  "id": "file-123456",
  "bytes": 5242880,
  "created_at": 1677610602,
  "filename": "audio.mp3",
  "purpose": "reference"
}
```
- `id`：文件ID，可在后续API中引用。
- `bytes`：文件大小，单位字节。
- `created_at`：文件创建时间戳（Unix秒）。
- `filename`：原始文件名。
- `purpose`：上传时指定的用途。

---

#### 6.1.2 创建上传对象（用于大文件分片上传）
创建一个上传对象，后续就可以向其中追加数据。

**请求**
- 方法：`POST`
- 路径：`/v1/uploads/create`
- 认证：Bearer Token
- 请求体：`application/json`

| 参数名        | 类型    | 必填 | 描述                                                         |
| ------------- | ------- | ---- | ------------------------------------------------------------ |
| `upload_name` | string  | 是   | 为创建的上传命名，或为要上传的大文件命名。                   |
| `purpose`     | string  | 是   | 此次上传的预期用途。目前仅支持 `fine-tuning`。               |
| `bytes`       | integer | 否   | 上传的总大小。如果未提供，则在上传结束时不会检查上传的总大小。 |

**请求示例**
```bash
curl https://api.mureka.cn/v1/uploads/create \
  -H "Authorization: Bearer $MUREKA_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "upload_name": "my.mp3",
    "purpose": "fine-tuning"
  }'
```

**响应示例**
```json
{
  "id": "upload-123456",
  "upload_name": "my.mp3",
  "purpose": "fine-tuning",
  "bytes": 0,
  "created_at": 1677610602,
  "expires_at": 1677697002,
  "status": "pending",
  "parts": []
}
```
- `id`：上传对象ID。
- `status`：上传状态，`pending`（进行中）、`completed`（已完成）、`cancelled`（已取消）。
- `parts`：已完成的分块列表。

---

#### 6.1.3 追加数据（分片上传）
向上传对象追加数据。追加的数据代表上传对象的一部分或一个大文件的块。上传数据最大可到10MB。

**请求**
- 方法：`POST`
- 路径：`/v1/uploads/add`
- 认证：Bearer Token
- 请求体：`multipart/form-data`

| 参数名      | 类型   | 必填 | 描述                                                         |
| ----------- | ------ | ---- | ------------------------------------------------------------ |
| `file`      | file   | 是   | 要上传的文件内容（二进制）。对于 `fine-tuning` 用途，支持格式 mp3、m4a，音频时长在30秒到270秒之间。 |
| `upload_id` | string | 是   | 此次追加数据所属的上传对象ID。                               |

**请求示例**
```bash
curl https://api.mureka.cn/v1/uploads/add \
  -H "Authorization: Bearer $MUREKA_API_KEY" \
  -F upload_id="upload-123456" \
  -F file=@/path/to/part1.mp3
```

**响应示例**
```json
{
  "id": "part-789012",
  "upload_id": "upload-123456",
  "created_at": 1677610700
}
```
- `id`：此次追加数据的ID，用于后续完成上传时指定分块顺序。

---

#### 6.1.4 完成上传
完成上传。当创建一个带有指定字节数的上传对象时，它会检查所有部分的大小是否与指定的字节数匹配。

**请求**
- 方法：`POST`
- 路径：`/v1/uploads/complete`
- 认证：Bearer Token
- 请求体：`application/json`

| 参数名      | 类型          | 必填 | 描述                                                         |
| ----------- | ------------- | ---- | ------------------------------------------------------------ |
| `upload_id` | string        | 是   | 上传对象的ID。                                               |
| `part_ids`  | array[string] | 否   | 追加数据ID的有序列表。如果此参数为空，则表示使用 `uploads/add` 添加的所有分块，顺序按添加顺序排列。 |

**请求示例**
```bash
curl https://api.mureka.cn/v1/uploads/complete \
  -H "Authorization: Bearer $MUREKA_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "upload_id": "upload-123456",
    "part_ids": ["part-789012", "part-789013"]
  }'
```

**响应示例**
```json
{
  "id": "upload-123456",
  "upload_name": "my.mp3",
  "purpose": "fine-tuning",
  "bytes": 10485760,
  "created_at": 1677610602,
  "expires_at": 1677697002,
  "status": "completed",
  "parts": ["part-789012", "part-789013"]
}
```
- `status`：此时为 `completed`。
- `parts`：按最终顺序排列的分块ID列表。

---

### 6.2 歌词相关

#### 6.2.1 生成歌词
根据提示生成歌词。

**请求**
- 方法：`POST`
- 路径：`/v1/lyrics/generate`
- 认证：Bearer Token
- 请求体：`application/json`

| 参数名   | 类型   | 必填 | 描述               |
| -------- | ------ | ---- | ------------------ |
| `prompt` | string | 是   | 生成歌词的提示词。 |

**请求示例**
```bash
curl https://api.mureka.cn/v1/lyrics/generate \
  -H "Authorization: Bearer $MUREKA_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "一首关于夏日海滩的浪漫歌曲"
  }'
```

**响应示例**
```json
{
  "title": "夏日海滩",
  "lyrics": "[Verse]\n金色的阳光洒在沙滩上\n海浪轻轻拍打着海岸\n我们手牵手漫步在夕阳下\n这一刻永远留在心间"
}
```
- `title`：生成的歌曲标题。
- `lyrics`：生成的歌词文本。

---

#### 6.2.2 扩展歌词
在现有歌词的基础上，续写下一句歌词。

**请求**
- 方法：`POST`
- 路径：`/v1/lyrics/extend`
- 认证：Bearer Token
- 请求体：`application/json`

| 参数名   | 类型   | 必填 | 描述           |
| -------- | ------ | ---- | -------------- |
| `lyrics` | string | 是   | 要续写的歌词。 |

**请求示例**
```bash
curl https://api.mureka.cn/v1/lyrics/extend \
  -H "Authorization: Bearer $MUREKA_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "lyrics": "[主歌]\n在暴风雨的夜晚，我独自徘徊\n被雨淋湿，感觉像是被抛弃\n你的回忆，在我眼前闪现\n希望这一刻，能找到一丝幸福"
  }'
```

**响应示例**
```json
{
  "lyrics": "可风雨中你的身影渐渐模糊"
}
```
- `lyrics`：续写的下一行歌词。

---

### 6.3 歌曲生成与处理

#### 6.3.1 生成歌曲
根据用户输入生成歌曲。异步任务，需通过查询接口获取结果。

**请求**
- 方法：`POST`
- 路径：`/v1/song/generate`
- 认证：Bearer Token
- 请求体：`application/json`

| 参数名         | 类型    | 必填 | 描述                                                         |
| -------------- | ------- | ---- | ------------------------------------------------------------ |
| `lyrics`       | string  | 是   | 生成音乐的歌词，最大3000个字符。                             |
| `model`        | string  | 否   | 要使用的模型。可选值：`auto`（默认，选择常规模型最新版）、`mureka-7.5`、`mureka-7.6`、`mureka-o2`、`mureka-8`。也可使用微调模型名称。 |
| `n`            | integer | 否   | 每次请求生成的歌曲数量，最大值为3，缺省为2。按数量计费。     |
| `prompt`       | string  | 否   | 通过输入提示词控制音乐生成，最大1024个字符。支持与 `vocal_id` 组合。 |
| `reference_id` | string  | 否   | 通过参考音乐控制音乐生成，由文件上传接口生成（purpose: `reference`）。支持与 `vocal_id` 组合。 |
| `vocal_id`     | string  | 否   | 通过音色控制音乐生成，由文件上传接口生成（purpose: `vocal`）。支持与 `reference_id` 或 `prompt` 组合。 |
| `melody_id`    | string  | 否   | 通过旋律控制音乐生成，由文件上传接口生成（purpose: `melody`）。不支持与其他控制选项组合。 |
| `stream`       | boolean | 否   | 如果设为 `true`，则生成任务会有 `streaming` 阶段，可获取 `stream_url` 边生成边收听。当模型为 `mureka-o1` 时不支持。 |

**请求示例**
```bash
curl https://api.mureka.cn/v1/song/generate \
  -H "Authorization: Bearer $MUREKA_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "lyrics": "[主歌]\n在暴风雨的夜晚，我独自徘徊\n被雨淋湿，感觉像是被抛弃\n你的回忆，在我眼前闪现\n希望这一刻，能找到一丝幸福",
    "model": "auto",
    "prompt": "r&b, slow, passionate, male vocal"
  }'
```

**响应示例**
```json
{
  "id": "task-123456",
  "created_at": 1677610602,
  "model": "mureka-7.5",
  "status": "preparing",
  "trace_id": "1e94aba5a2de4cc4bff54a2813c8d36c"
}
```
- `id`：任务ID，用于查询任务状态。

---

#### 6.3.2 查询歌曲生成任务
查询歌曲生成任务的信息。

**请求**
- 方法：`GET`
- 路径：`/v1/song/query/{task_id}`
- 认证：Bearer Token

**路径参数**
| 参数名    | 类型   | 必填 | 描述                     |
| --------- | ------ | ---- | ------------------------ |
| `task_id` | string | 是   | 歌曲生成任务的 task_id。 |

**请求示例**
```bash
curl https://api.mureka.cn/v1/song/query/task-123456 \
  -H "Authorization: Bearer $MUREKA_API_KEY"
```

**响应示例**
```json
{
  "id": "task-123456",
  "created_at": 1677610602,
  "finished_at": 1677610702,
  "model": "mureka-7.5",
  "status": "succeeded",
  "failed_reason": null,
  "watermarked": true,
  "choices": [
    {
      "index": 0,
      "url": "https://cdn.mureka.cn/output/123456_0.mp3",
      "wav_url": "https://cdn.mureka.cn/output/123456_0.wav",
      "stream_url": "https://cdn.mureka.cn/stream/123456_0.m3u8",
      "expires_at": 1677697002,
      "lyrics": "[主歌]\n在暴风雨的夜晚，我独自徘徊\n被雨淋湿，感觉像是被抛弃\n你的回忆，在我眼前闪现\n希望这一刻，能找到一丝幸福",
      "duration": 180000
    }
  ]
}
```
- `status`：任务状态，枚举值：`preparing`、`queued`、`running`、`streaming`、`reviewing`、`succeeded`、`failed`、`timeouted`、`cancelled`。
- `choices`：生成结果列表，每个元素包含音频URL（`url` 为 mp3，`wav_url` 为 wav）、流式URL（`stream_url`）、过期时间、歌词和时长（毫秒）。

---

#### 6.3.3 续写歌曲
根据输入的歌词续写歌曲。

**请求**
- 方法：`POST`
- 路径：`/v1/song/extend`
- 认证：Bearer Token
- 请求体：`application/json`

| 参数名            | 类型    | 必填   | 描述                                                         |
| ----------------- | ------- | ------ | ------------------------------------------------------------ |
| `song_id`         | string  | 二选一 | 要续写的歌曲id，由 `song/generate` API生成，只支持1个月内的生成歌曲。 |
| `upload_audio_id` | string  | 二选一 | 要续写的歌曲上传id，由文件上传接口生成（purpose: `audio`）。 |
| `lyrics`          | string  | 是     | 续写的歌词。                                                 |
| `extend_at`       | integer | 是     | 续写开始时间，单位毫秒。如果大于歌曲的时长，则取歌曲的时长。取值范围 [8000, 420000]。 |

**请求示例**
```bash
curl https://api.mureka.cn/v1/song/extend \
  -H "Authorization: Bearer $MUREKA_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "upload_audio_id": "file-43543541",
    "lyrics": "[主歌]\n在暴风雨的夜晚，我独自徘徊\n被雨淋湿，感觉像是被抛弃\n你的回忆，在我眼前闪现\n希望这一刻，能找到一丝幸福",
    "extend_at": 12234
  }'
```

**响应示例**
```json
{
  "id": "task-123457",
  "created_at": 1677610602,
  "model": "mureka-7.5",
  "status": "preparing",
  "trace_id": "1e94aba5a2de4cc4bff54a2813c8d36d"
}
```
- 返回任务ID，后续通过 `/v1/song/query/{task_id}` 查询结果。

---

#### 6.3.4 识别歌曲
将输入的歌曲转换为带时间戳信息的歌词。

**请求**
- 方法：`POST`
- 路径：`/v1/song/recognize`
- 认证：Bearer Token
- 请求体：`application/json`

| 参数名            | 类型   | 必填 | 描述                                                         |
| ----------------- | ------ | ---- | ------------------------------------------------------------ |
| `upload_audio_id` | string | 是   | 要识别的歌曲上传id，由文件上传接口生成（purpose: `audio`）。 |

**请求示例**
```bash
curl https://api.mureka.cn/v1/song/recognize \
  -H "Authorization: Bearer $MUREKA_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "upload_audio_id": "file-43543541"
  }'
```

**响应示例**
```json
{
  "duration": 240000,
  "lyrics_sections": [
    {
      "start": 0,
      "end": 10000,
      "text": "在暴风雨的夜晚，我独自徘徊"
    },
    {
      "start": 10000,
      "end": 20000,
      "text": "被雨淋湿，感觉像是被抛弃"
    }
  ]
}
```
- `duration`：歌曲总时长（毫秒）。
- `lyrics_sections`：带时间戳的歌词段落，每个段落包含起始时间、结束时间和文本。

---

#### 6.3.5 理解歌曲
对输入的歌曲作理解描述。

**请求**
- 方法：`POST`
- 路径：`/v1/song/describe`
- 认证：Bearer Token
- 请求体：`application/json`

| 参数名 | 类型   | 必填 | 描述                                                         |
| ------ | ------ | ---- | ------------------------------------------------------------ |
| `url`  | string | 是   | 需要处理的歌曲URL。支持格式：mp3、m4a。也支持base64格式的URL，最大数据大小为10MB（如 `data:audio/mp3;base64,AAAAGGZ...`）。 |

**请求示例**
```bash
# 标准URL
curl https://api.mureka.cn/v1/song/describe \
  -H "Authorization: Bearer $MUREKA_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://cdn.mureka.cn/1.mp3"
  }'

# base64格式（Linux）
echo -n '{"url": "data:audio/mp3;base64,'"$(base64 -w 0 test.mp3)"'"}' | \
  curl https://api.mureka.cn/v1/song/describe \
    -H "Authorization: Bearer $MUREKA_API_KEY" \
    -H "Content-Type: application/json" \
    -d @-
```

**响应示例**
```json
{
  "instrument": ["钢琴", "吉他", "鼓"],
  "genres": ["流行", "民谣"],
  "tags": ["抒情", "温暖", "治愈"],
  "description": "这是一首以钢琴和吉他为主伴奏的流行民谣歌曲，节奏舒缓，旋律温暖，歌词表达了对过去的怀念和对未来的希望。"
}
```
- `instrument`：使用的乐器列表。
- `genres`：曲风列表。
- `tags`：标签列表。
- `description`：总体描述文本。

---

#### 6.3.6 分轨歌曲
对输入的歌曲进行分轨，返回包含所有分轨的ZIP文件。

**请求**
- 方法：`POST`
- 路径：`/v1/song/stem`
- 认证：Bearer Token
- 请求体：`application/json`

| 参数名 | 类型   | 必填 | 描述                                                         |
| ------ | ------ | ---- | ------------------------------------------------------------ |
| `url`  | string | 是   | 需要处理的歌曲URL。支持格式：mp3、m4a。也支持base64格式的URL，最大数据大小为10MB。 |

**请求示例**
```bash
curl https://api.mureka.cn/v1/song/stem \
  -H "Authorization: Bearer $MUREKA_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://cdn.mureka.cn/1.mp3"
  }'
```

**响应示例**
```json
{
  "zip_url": "https://cdn.mureka.cn/stems/123456.zip",
  "expires_at": 1677697002
}
```
- `zip_url`：包含所有分轨的ZIP文件URL。
- `expires_at`：URL过期时间戳。

---

### 6.4 纯音乐生成

#### 6.4.1 生成纯音乐
根据用户输入生成纯音乐。异步任务，需通过查询接口获取结果。

**请求**
- 方法：`POST`
- 路径：`/v1/instrumental/generate`
- 认证：Bearer Token
- 请求体：`application/json`

| 参数名            | 类型    | 必填 | 描述                                                         |
| ----------------- | ------- | ---- | ------------------------------------------------------------ |
| `model`           | string  | 否   | 要使用的模型。可选值：`auto`（默认）、`mureka-7.5`、`mureka-7.6`。 |
| `n`               | integer | 否   | 每次请求生成的纯音乐数量，最大值为3，缺省为2。按数量计费。   |
| `prompt`          | string  | 否   | 通过输入提示词控制纯音乐的生成，最大1024个字符。不支持与其他控制选项组合。 |
| `instrumental_id` | string  | 否   | 通过参考音乐控制纯音乐的生成，由文件上传接口生成（purpose: `instrumental`）。不支持与其他控制选项组合。 |
| `stream`          | boolean | 否   | 如果设为 `true`，则生成任务会有 `streaming` 阶段，可获取 `stream_url` 边生成边收听。 |

**请求示例**
```bash
curl https://api.mureka.cn/v1/instrumental/generate \
  -H "Authorization: Bearer $MUREKA_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "auto",
    "prompt": "r&b, slow, passionate"
  }'
```

**响应示例**
```json
{
  "id": "task-123458",
  "created_at": 1677610602,
  "model": "mureka-7.5",
  "status": "preparing",
  "trace_id": "1e94aba5a2de4cc4bff54a2813c8d36e"
}
```
- `id`：任务ID，用于查询任务状态。

---

#### 6.4.2 查询纯音乐生成任务
查询纯音乐生成任务的信息。

**请求**
- 方法：`GET`
- 路径：`/v1/instrumental/query/{task_id}`
- 认证：Bearer Token

**路径参数**
| 参数名    | 类型   | 必填 | 描述                       |
| --------- | ------ | ---- | -------------------------- |
| `task_id` | string | 是   | 纯音乐生成任务的 task_id。 |

**请求示例**
```bash
curl https://api.mureka.cn/v1/instrumental/query/task-123458 \
  -H "Authorization: Bearer $MUREKA_API_KEY"
```

**响应示例**
```json
{
  "id": "task-123458",
  "created_at": 1677610602,
  "finished_at": 1677610702,
  "model": "mureka-7.5",
  "status": "succeeded",
  "failed_reason": null,
  "watermarked": true,
  "choices": [
    {
      "index": 0,
      "url": "https://cdn.mureka.cn/instrumental/123458_0.mp3",
      "wav_url": "https://cdn.mureka.cn/instrumental/123458_0.wav",
      "stream_url": "https://cdn.mureka.cn/stream/123458_0.m3u8",
      "expires_at": 1677697002,
      "duration": 180000
    }
  ]
}
```
- 响应结构与歌曲查询类似，但无歌词字段。

---

### 6.5 模型微调

#### 6.5.1 创建微调任务
创建一个微调任务，使用给定的数据集创建一个微调模型。

**请求**
- 方法：`POST`
- 路径：`/v1/finetuning/create`
- 认证：Bearer Token
- 请求体：`application/json`

| 参数名      | 类型   | 必填 | 描述                                                         |
| ----------- | ------ | ---- | ------------------------------------------------------------ |
| `upload_id` | string | 是   | 上传对象的ID（状态必须为 `completed`）。一次有效的微调需要上传100-200首风格一致的歌，每首歌时长2-4分钟。训练时长与歌曲总时长相关，200首4分钟的歌曲约需训练3小时。 |
| `suffix`    | string | 是   | 微调模型名称后缀，最多32个字符，仅允许使用小写字母、数字和连字符。例如后缀为 `my-model`，则产生的模型名称为 `lora:mureka-6:4354198:my-model`。 |

**请求示例**
```bash
curl https://api.mureka.cn/v1/finetuning/create \
  -H "Authorization: Bearer $MUREKA_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "upload_id": "upload-123456",
    "suffix": "my-model"
  }'
```

**响应示例**
```json
{
  "id": "finetune-123456",
  "upload_id": "upload-123456",
  "model": "mureka-6",
  "created_at": 1677610602,
  "finished_at": 0,
  "status": "queued",
  "failed_reason": null,
  "fine_tuned_model": null
}
```
- `id`：微调任务ID。
- `status`：任务状态，枚举值：`preparing`、`queued`、`running`、`succeeded`、`failed`、`timeouted`、`cancelled`。
- `fine_tuned_model`：完成后将填充微调模型名称。

---

#### 6.5.2 查询微调任务
查询微调任务的信息。

**请求**
- 方法：`GET`
- 路径：`/v1/finetuning/query/{task_id}`
- 认证：Bearer Token

**路径参数**
| 参数名    | 类型   | 必填 | 描述                 |
| --------- | ------ | ---- | -------------------- |
| `task_id` | string | 是   | 微调任务的 task_id。 |

**请求示例**
```bash
curl https://api.mureka.cn/v1/finetuning/query/finetune-123456 \
  -H "Authorization: Bearer $MUREKA_API_KEY"
```

**响应示例**
```json
{
  "id": "finetune-123456",
  "upload_id": "upload-123456",
  "model": "mureka-6",
  "created_at": 1677610602,
  "finished_at": 1677621402,
  "status": "succeeded",
  "failed_reason": null,
  "fine_tuned_model": "lora:mureka-6:4354198:my-model"
}
```
- `fine_tuned_model`：微调完成后的模型名称，可在歌曲生成接口的 `model` 字段中使用。

---

### 6.6 语音合成

#### 6.6.1 创建语音（TTS）
根据输入文本生成语音。

**请求**
- 方法：`POST`
- 路径：`/v1/tts/generate`
- 认证：Bearer Token
- 请求体：`application/json`

| 参数名     | 类型   | 必填   | 描述                                                         |
| ---------- | ------ | ------ | ------------------------------------------------------------ |
| `text`     | string | 是     | 要生成音频的文本，最大长度为500个字符。                      |
| `voice`    | string | 二选一 | 预定义说话人，可选值：`Ethan`、`Victoria`、`Jake`、`Luna`、`Emma`。 |
| `voice_id` | string | 二选一 | 通过参考语音控制生成，由文件上传接口生成（purpose: `voice`）。 |

**请求示例**
```bash
curl https://api.mureka.cn/v1/tts/generate \
  -H "Authorization: Bearer $MUREKA_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "text": "嗨，我叫艾玛。",
    "voice": "Emma"
  }'
```

**响应示例**
```json
{
  "url": "https://cdn.mureka.cn/tts/123456.mp3",
  "expires_at": 1677697002
}
```
- `url`：生成的音频文件URL。
- `expires_at`：URL过期时间戳。

---

#### 6.6.2 创建播客（多角色TTS）
将双人对话脚本转换为自然的播客风格音频。

**请求**
- 方法：`POST`
- 路径：`/v1/tts/podcast`
- 认证：Bearer Token
- 请求体：`application/json`

| 参数名          | 类型  | 必填 | 描述                                                         |
| --------------- | ----- | ---- | ------------------------------------------------------------ |
| `conversations` | array | 是   | 对话数组，数组最大长度为10。每个元素包含 `text` 和 `voice` 字段。 |

**请求示例**
```bash
curl https://api.mureka.cn/v1/tts/podcast \
  -H "Authorization: Bearer $MUREKA_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "conversations": [
      {"text": "嗨，我叫露娜。", "voice": "Luna"},
      {"text": "嗨，我叫杰克。", "voice": "Jake"}
    ]
  }'
```

**响应示例**
```json
{
  "url": "https://cdn.mureka.cn/podcast/123456.mp3",
  "expires_at": 1677697002
}
```
- `url`：生成的播客音频文件URL。
- `expires_at`：URL过期时间戳。

---

### 6.7 账户与账单

#### 6.7.1 查询账单
查询账户的账单信息。

**请求**
- 方法：`GET`
- 路径：`/v1/account/billing`
- 认证：Bearer Token

**请求示例**
```bash
curl https://api.mureka.cn/v1/account/billing \
  -H "Authorization: Bearer $MUREKA_API_KEY"
```

**响应示例**
```json
{
  "account_id": 10001,
  "balance": 50000,
  "total_recharge": 100000,
  "total_spending": 50000,
  "concurrent_request_limit": 5
}
```
- `account_id`：账户ID。
- `balance`：账户余额，单位分。
- `total_recharge`：总充值额，单位分。
- `total_spending`：总消费额，单位分。
- `concurrent_request_limit`：账户的最大并发请求次数。

---

## 7. 错误代码

本指南提供了您在使用 API 时可能会遇到的错误代码的概述。所有错误响应均包含 `trace_id` 字段，便于追踪问题。

### 7.1 错误响应格式
当请求失败时，API 返回 400~600 之间的 HTTP 状态码，并附带如下 JSON 结构：
```json
{
  "error": {
    "message": "错误描述"
  },
  "trace_id": "1e94aba5a2de4cc4bff54a2813c8d36c"
}
```

### 7.2 错误码列表

| 状态码 | 概述                                                         | 原因与解决方案                                               |
| ------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| 400    | Invalid Request                                              | **原因**：请求参数不正确。<br>**解决方案**：请参考对应接口的文档，检查请求参数的格式、类型和必填项。 |
| 401    | Invalid Authentication                                       | **原因**：认证无效。<br>**解决方案**：请确保使用了正确的 API 密钥，并在 `Authorization` 头中正确携带（格式：`Bearer YOUR_API_KEY`）。 |
| 403    | Forbidden                                                    | **原因**：您正在从不支持的地区访问 API。<br>**解决方案**：请确保您的访问来源位于 Mureka API 支持的地区列表内。 |
| 429    | Rate limit reached for requests                              | **原因**：请求频率超过限制。<br>**解决方案**：请降低请求速度，查看价格方案中关于并发请求的限制，或联系技术支持提升配额。 |
| 429    | You exceeded your current quota, please check your billing details | **原因**：账户余额或配额已用尽。<br>**解决方案**：请前往控制台充值或升级套餐。 |
| 451    | Unavailable For Legal Reasons                                | **原因**：请求参数未通过安全审核（如包含违规内容）。<br>**解决方案**：请修改请求参数，确保符合平台内容政策。 |
| 500    | The server had an error while processing your request        | **原因**：服务器内部错误。<br>**解决方案**：稍后重试。若问题持续，请联系技术支持并提供 `trace_id`。 |
| 503    | The engine is currently overloaded, please try again later   | **原因**：服务负载过高，暂时无法处理请求。<br>**解决方案**：请稍后重试。建议实现指数退避重试策略。 |

### 7.3 最佳实践
- **重试策略**：对于 `5xx` 错误（如 500、503），建议采用指数退避算法重试（如首次等待 1 秒，再次等待 2 秒、4 秒……）。
- **记录 trace_id**：所有错误响应都包含 `trace_id`，在联系技术支持时提供该 ID 可以加速问题定位。
- **监控配额**：定期检查账户余额和 API 调用配额，避免因欠费导致服务中断。

## 8. 更新日志

本更新日志记录 Mureka API 的重要变更，帮助开发者了解最新功能与改进。

| 日期       | 分类           | 详情                                                         |
| ---------- | -------------- | ------------------------------------------------------------ |
| 2026.2.5   | 模型更新       | 生成纯音乐，`model` 字段增加 `mureka-7.6`                    |
| 2026.1.5   | 模型更新       | `vocal` 功能增强，`vocal_id` 和 `prompt` 可以同时控制，支持 10 语种：中、英、日、韩、葡、西、德、意、法、俄 |
| 2025.12.9  | 接口和模型更新 | - 生成歌曲和纯音乐新增一个字段：`wav_url`<br>- 新增 `mureka-7.6`、`mureka-o2` 模型，提升生成音乐的效果 |
| 2025.10.30 | 接口更新       | 生成的歌曲尾部添加 5s 水印音频                               |
| 2025.10.27 | 接口更新       | 歌曲生成在流模式下，`stream url` 的有效期在歌曲生成结束后，额外增加 5 分钟 |
| 2025.9.25  | 接口和模型更新 | - 新增 1 个接口：歌曲理解<br>- 新增 `mureka-7.5` 模型，替换 `mureka-7`，提升生成音乐的效果 |
| 2025.9.1   | 接口更新       | 歌曲和纯音乐生成接口，支持通过输入参数 `n` 来控制生成的数量  |
| 2025.7.29  | 接口和模型更新 | - 生成歌曲和纯音乐支持流式输出<br>- 新增两个接口：歌曲识别和歌曲续写<br>- 新增 `mureka-7` 模型，全面提升生成音乐的效果<br>- 新增 `mureka-o1` 模型，首款音乐推理模型 |
| 2025.6.10  | 接口更新       | 歌曲生成接口返回的歌词结构，`breakdown` 改为 `break`         |
| 2025.6.4   | 接口更新       | 上传音频支持格式增加 `m4a`，`melody` 额外支持 `midi` 格式    |
| 2025.6.3   | 模型更新       | `mureka-6` 模型优化版上线                                    |
| 2025.4.18  | 接口更新       | 歌曲生成接口，控制参数 `reference_id` 和 `vocal_id` 可以同时输入 |

> **说明**：更新日志按日期降序排列（最近更新在前）。如您在使用中遇到任何问题，请参考错误代码章节或联系技术支持。

---

文档整理完毕。如需调整格式或补充内容，请随时告知。
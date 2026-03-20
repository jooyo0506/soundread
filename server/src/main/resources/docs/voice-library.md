# 声读 SoundRead — 音色资源库

> **版本**: v3.0 (2026-02-27)  
> **用途**: 记录系统支持的全部发音人音色 ID，供开发联调和产品运营参考。

---

## TTS 1.0 音色 (火山引擎 · 同步 HTTP)

> 对应 SDK：`Tts1Client.java`，ID 格式 `BVxxx_streaming`

### 中文场景

| 音色名称 | 音色ID | 场景 | 性别 |
|:---------|:-------|:-----|:-----|
| 通用男声 | BV002_streaming | 通用 | 男 |
| 通用女声 | BV001_streaming | 通用 | 女 |
| 灿灿 | BV700_streaming | 通用 | 女 |
| 阳光男声 | BV056_streaming | 视频配音 | 男 |
| 活泼女声 | BV005_streaming | 视频配音 | 女 |
| 擎苍 | BV701_streaming | 有声阅读 | 男 |
| 儒雅青年 | BV102_streaming | 有声阅读 | 男 |
| 甜宠少御 | BV113_streaming | 有声阅读 | 女 |
| 古风少御 | BV115_streaming | 有声阅读 | 女 |
| 亲切女声 | BV007_streaming | 客服 | 女 |
| 温柔小哥 | BV033_streaming | 教育 | 男 |
| 知性姐姐-双语 | BV034_streaming | 教育 | 女 |

### 特色方言

| 音色名称 | 音色ID | 方言 |
|:---------|:-------|:-----|
| 东北老铁 | BV021_streaming | 东北话 |
| 重庆小伙 | BV019_streaming | 重庆话 |

### 外语

| 音色名称 | 音色ID | 语种 |
|:---------|:-------|:-----|
| 日语男声 | BV524_streaming | 日语 |
| 气质女生 | BV522_streaming | 日语 |
| 活力女声-Ariana | BV503_streaming | 英语 |
| 活力男声-Jackson | BV504_streaming | 英语 |

### 特色

| 音色名称 | 音色ID | 场景 |
|:---------|:-------|:-----|
| 奶气萌娃 | BV051_streaming | 儿童 |

---

## TTS 2.0 音色 (火山 Seed-TTS 2.0 · WebSocket)

> 对应 SDK：`Tts2Client.java` (双向 WebSocket)，ID 格式各异

### 中文场景

| 音色名称 | 音色ID | 场景 |
|:---------|:-------|:-----|
| vivi 2.0 | zh_female_vv_uranus_bigtts | 通用 |
| 小何 | zh_female_xiaohe_uranus_bigtts | 通用 |
| 云舟 | zh_male_m191_uranus_bigtts | 通用 |
| 小天 | zh_male_taocheng_uranus_bigtts | 通用 |
| 大壹 | zh_male_dayi_saturn_bigtts | 视频配音 |
| 黑猫侦探社咪仔 | zh_female_mizai_saturn_bigtts | 视频配音 |
| 鸡汤女 | zh_female_jitangnv_saturn_bigtts | 视频配音 |
| 魅力女友 | zh_female_meilinvyou_saturn_bigtts | 视频配音 |
| 流畅女声 | zh_female_santongyongns_saturn_bigtts | 视频配音 |
| 儒雅逸辰 | zh_male_ruyayichen_saturn_bigtts | 视频配音 |
| 知性灿灿 | saturn_zh_female_cancan_tob | 角色扮演 |
| 可爱女生 | saturn_zh_female_keainvsheng_tob | 角色扮演 |
| 调皮公主 | saturn_zh_female_tiaopigongzhu_tob | 角色扮演 |
| 爽朗少年 | saturn_zh_male_shuanglangshaonian_tob | 角色扮演 |
| 天才同桌 | saturn_zh_male_tiancaitongzhuo_tob | 角色扮演 |
| 儿童绘本 | zh_female_xueayi_saturn_bigtts | 有声阅读 |

### 英语

| 音色名称 | 音色ID | 场景 |
|:---------|:-------|:-----|
| Tim | en_male_tim_uranus_bigtts | 通用 |
| Dacey | en_female_dacey_uranus_bigtts | 通用 |
| Stokie | en_female_stokie_uranus_bigtts | 通用 |

---

> **维护说明**：新增音色需同步更新 `sys_voice` 数据库表和本文档。音色的 `supported_engines` 字段决定其在哪些引擎下可用。

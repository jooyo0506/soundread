/**
 * 全局音频播放器 Store
 *
 * 管理全局唯一的 Audio 实例，提供播放、暂停、进度拖拽、关闭等操作。
 * 任何页面都可通过 `playerStore.play(track)` 触发底部播放条。
 *
 * ★ 支持流式播放(HLS)：自动处理 Infinity 时长、断流重连
 * ★ 后台轮询：离开音乐页依然能接收生成完成通知
 *
 * @module stores/player
 */
import { defineStore } from 'pinia'
import { musicApi } from '../api/music'

/** 流式播放时的预估时长（秒），真实时长未知时使用 */
const ESTIMATED_STREAM_DURATION = 180

/** 断流重试间隔(ms) */
const RETRY_DELAY = 3000
/** 最大重试次数 */
const MAX_RETRIES = 5

export const usePlayerStore = defineStore('player', {
    state: () => ({
        /** 全局 Audio 实例（懒初始化） */
        audio: null,
        /** 是否正在播放 */
        isPlaying: false,
        /** 当前播放进度（秒） */
        currentTime: 0,
        /** 当前曲目总时长（秒） */
        duration: 0,
        /** 当前播放曲目信息 { title, author, cover, url, lyrics? } */
        currentTrack: null,
        /** 是否显示底部播放条 */
        showPlayer: false,
        /** 待播放队列（多片段 TTS 连续播放） */
        playQueue: [],

        // ── 流式播放状态 ──
        /** 是否是流式播放中 */
        isStreaming: false,
        /** 预估时长（流式用） */
        estimatedDuration: ESTIMATED_STREAM_DURATION,
        /** 断流重试次数 */
        _retryCount: 0,
        /** 重试定时器 */
        _retryTimer: null,

        // ── 后台任务轮询 ──
        /** 正在轮询的任务 ID */
        pollingTaskId: null,
        /** 轮询定时器 */
        _pollTimer: null
    }),

    getters: {
        /**
         * 安全时长：处理流式播放时 duration 为 Infinity/NaN 的情况
         */
        safeDuration(state) {
            if (!state.duration || !isFinite(state.duration) || isNaN(state.duration)) {
                return state.estimatedDuration
            }
            return state.duration
        }
    },

    actions: {
        /**
         * 懒初始化 Audio 实例并绑定事件监听
         * 仅在首次调用时创建，后续复用同一实例
         */
        init() {
            if (this.audio) { return }

            this.audio = new Audio()

            this.audio.addEventListener('timeupdate', () => {
                this.currentTime = this.audio.currentTime
            })

            this.audio.addEventListener('loadedmetadata', () => {
                const dur = this.audio.duration
                if (isFinite(dur) && !isNaN(dur) && dur > 0) {
                    this.duration = dur
                    this.isStreaming = false
                } else {
                    // HLS 流式播放 → duration 为 Infinity
                    this.duration = 0
                    this.isStreaming = true
                }
            })

            // ★ 持续更新 duration（流式播放中 duration 可能随时间更新）
            this.audio.addEventListener('durationchange', () => {
                const dur = this.audio.duration
                if (isFinite(dur) && !isNaN(dur) && dur > 0) {
                    this.duration = dur
                    this.isStreaming = false
                }
            })

            this.audio.addEventListener('ended', () => {
                // 队列中还有下一段 → 自动播放
                if (this.playQueue.length > 0) {
                    const nextUrl = this.playQueue.shift()
                    this.audio.src = nextUrl
                    if (this.currentTrack) this.currentTrack.url = nextUrl
                    this.audio.play().catch(e => console.error('Queue play failed:', e))
                } else {
                    this.isPlaying = false
                    this.isStreaming = false
                }
            })

            // ★ 断流处理：自动重试
            this.audio.addEventListener('error', (e) => {
                console.error('Audio playback error', e)
                if (this.isStreaming && this._retryCount < MAX_RETRIES) {
                    console.log(`[Player] 流断开，${RETRY_DELAY / 1000}秒后重试 (${this._retryCount + 1}/${MAX_RETRIES})`)
                    this._retryCount++
                    this._retryTimer = setTimeout(() => {
                        if (this.audio && this.currentTrack?.url) {
                            const currentPos = this.currentTime
                            this.audio.src = this.currentTrack.url
                            this.audio.currentTime = currentPos
                            this.audio.play().then(() => {
                                this.isPlaying = true
                            }).catch(() => { })
                        }
                    }, RETRY_DELAY)
                } else {
                    this.isPlaying = false
                }
            })

            // ★ 播放恢复后重置重试计数
            this.audio.addEventListener('playing', () => {
                this._retryCount = 0
            })

            // ★ 处理缓冲等待（流式经常发生）
            this.audio.addEventListener('waiting', () => {
                // 不改 isPlaying 状态，保持 UI 一致
                console.log('[Player] 缓冲中...')
            })

            this.audio.addEventListener('canplay', () => {
                console.log('[Player] 缓冲完成，可播放')
            })
        },

        /**
         * 播放指定曲目（自动初始化 + 切换音源）
         * @param {string|{ title: string, author: string, cover: string, url: string, queue?: string[] }} trackOrUrl
         */
        play(trackOrUrl) {
            this.init()
            this.showPlayer = true

            // 兼容：如果传入的是字符串 URL，自动包装为 track 对象
            const track = typeof trackOrUrl === 'string'
                ? { title: '未命名音频', url: trackOrUrl }
                : trackOrUrl

            // 设置队列（多片段 TTS 连续播放）
            this.playQueue = track.queue ? [...track.queue] : []

            // 清除重试状态
            this._retryCount = 0
            if (this._retryTimer) {
                clearTimeout(this._retryTimer)
                this._retryTimer = null
            }

            // 切换音源前先暂停，防止 AbortError
            if (this.audio.src && !this.audio.paused) {
                this.audio.pause()
            }

            if (this.currentTrack?.url !== track.url || this.playQueue.length > 0) {
                this.currentTrack = { ...track }
                delete this.currentTrack.queue
                this.audio.src = track.url
                // 重置时长（新曲目）
                this.duration = 0
                this.isStreaming = false
            }

            this.audio.play().then(() => {
                this.isPlaying = true
            }).catch(e => {
                console.error('Audio play failed:', e)
                this.isPlaying = false
            })
        },

        /** 切换播放 / 暂停状态 */
        togglePlay() {
            if (!this.audio || !this.currentTrack) { return }

            if (this.audio.paused) {
                this.audio.play().then(() => { this.isPlaying = true })
            } else {
                this.audio.pause()
                this.isPlaying = false
            }
        },

        /**
         * 跳转到指定时间点
         * @param {number} time - 目标时间（秒）
         */
        seek(time) {
            if (this.audio && isFinite(time)) {
                try {
                    this.audio.currentTime = time
                } catch (e) {
                    // 流式播放中 seek 可能失败
                    console.warn('[Player] Seek 失败（流式播放中不支持）', e)
                }
            }
        },

        /** 关闭播放器并释放状态 */
        close() {
            if (this.audio) {
                this.audio.pause()
            }
            this.isPlaying = false
            this.showPlayer = false
            this.currentTrack = null
            this.playQueue = []
            this.isStreaming = false
            this._retryCount = 0
            if (this._retryTimer) {
                clearTimeout(this._retryTimer)
                this._retryTimer = null
            }
        },

        /**
         * 向播放队列追加 URL（用于边合成边播放）
         * 如果当前没有在播放且队列为空，则立即播放
         * @param {string} url - 音频 URL
         */
        enqueue(url) {
            if (!url) return
            this.init()
            if (!this.isPlaying && this.playQueue.length === 0) {
                // 当前空闲，立即播放
                this.audio.src = url
                if (this.currentTrack) this.currentTrack.url = url
                this.audio.play().then(() => { this.isPlaying = true })
                    .catch(e => console.error('Enqueue play failed:', e))
            } else {
                // 正在播放，推入队列等待自动衔接
                this.playQueue.push(url)
            }
        },

        // ═════════════════════════════════════════
        // ★ 后台任务轮询（全局级，离开页面不中断）
        // ═════════════════════════════════════════

        /**
         * 启动后台轮询一个音乐生成任务
         * @param {string|number} taskId - 任务 ID
         * @param {Function} onComplete - 任务完成回调 (task) => void
         */
        startTaskPolling(taskId, onComplete) {
            this.stopTaskPolling()
            this.pollingTaskId = taskId

            this._pollTimer = setInterval(async () => {
                if (!this.pollingTaskId) {
                    this.stopTaskPolling()
                    return
                }
                try {
                    const task = await musicApi.getTask(this.pollingTaskId)

                    // ★ streaming → 自动播放流
                    if (task.status === 'streaming' && task.streamUrl) {
                        if (!this.isPlaying || this.currentTrack?.url !== task.streamUrl) {
                            this.play({
                                title: task.title || 'AI 音乐 (生成中...)',
                                author: '🎵 AI Music · 实时生成',
                                url: task.streamUrl,
                                lyrics: task.taskType === 'song' ? task.lyrics : null
                            })
                        }
                    }

                    // ★ succeeded → 切换到最终 URL + 通知
                    if (task.status === 'succeeded') {
                        if (task.resultUrl) {
                            // 解析歌词时间戳
                            let lyricTimings = null
                            if (task.lyricTimings) {
                                try {
                                    lyricTimings = typeof task.lyricTimings === 'string'
                                        ? JSON.parse(task.lyricTimings)
                                        : task.lyricTimings
                                } catch (e) { /* 降级 */ }
                            }
                            this.play({
                                title: task.title || 'AI 音乐',
                                author: 'AI Music',
                                url: task.resultUrl,
                                lyrics: task.taskType === 'song' ? task.lyrics : null,
                                lyricTimings
                            })
                        }
                        this.stopTaskPolling()
                        if (onComplete) onComplete(task)
                    }

                    if (task.status === 'failed') {
                        this.stopTaskPolling()
                        if (onComplete) onComplete(task)
                    }
                } catch (e) {
                    console.error('[Player] 后台轮询异常:', e)
                }
            }, 3000)
        },

        /** 停止后台轮询 */
        stopTaskPolling() {
            if (this._pollTimer) {
                clearInterval(this._pollTimer)
                this._pollTimer = null
            }
            this.pollingTaskId = null
        }
    }
})

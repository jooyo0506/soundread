/**
 * WebSocket 连接管理 Composable
 *
 * 封装 WebSocket 连接的生命周期管理，包括：
 * - 自动注入认证令牌（通过 query 参数）
 * - 断线指数退避重连（最多 5 次，间隔 2s → 4s → 8s → ...）
 * - 组件卸载时自动清理连接
 *
 * @module composables/useWebSocket
 * @example
 * const { connect, send, close, isConnected } = useWebSocket('/ws/tts', {
 *   autoReconnect: true,
 *   onMessage: (event) => handleMessage(event.data)
 * })
 * connect()
 */
import { ref, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'

/** 默认最大重连次数 */
const DEFAULT_MAX_RECONNECTS = 5

/** 重连基础延迟（毫秒），配合指数退避 */
const RECONNECT_BASE_DELAY_MS = 1000

/** 重连最大延迟上限（毫秒） */
const RECONNECT_MAX_DELAY_MS = 10000

/**
 * WebSocket 连接管理 Composable
 *
 * @param {string} endpoint - WebSocket 路径（如 '/ws/tts'）
 * @param {Object} options - 配置选项
 * @param {boolean} [options.autoReconnect=false] - 是否自动重连
 * @param {number} [options.maxReconnects=5] - 最大重连次数
 * @param {Function} [options.onOpen] - 连接成功回调
 * @param {Function} [options.onMessage] - 收到消息回调
 * @param {Function} [options.onError] - 连接错误回调
 * @param {Function} [options.onClose] - 连接关闭回调
 * @returns {{ isConnected: Ref<boolean>, connect: Function, send: Function, close: Function }}
 */
export function useWebSocket(endpoint, options = {}) {
    const ws = ref(null)
    const isConnected = ref(false)
    const authStore = useAuthStore()

    // 优先取环境变量（线上 www.joyoai.xyz 前端 → 后端 joyoai.xyz 跨域WS）
    // 本地开发 fallback 到 window.location.host（由 vite proxy 转发）
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsBase = import.meta.env.VITE_WS_BASE_URL
    const wsUrl = wsBase
        ? `${wsBase}${endpoint}`
        : `${protocol}//${window.location.host}${endpoint}`

    let reconnectTimer = null
    let reconnectAttempts = 0
    const maxReconnectAttempts = options.maxReconnects || DEFAULT_MAX_RECONNECTS

    /**
     * 建立 WebSocket 连接
     * 通过 query 参数传递认证令牌（WebSocket 不支持自定义 Header）
     */
    const connect = () => {
        if (ws.value) { return }

        const token = authStore.token ? authStore.token.replace('Bearer ', '') : ''
        const sep = wsUrl.includes('?') ? '&' : '?'
        const urlWithAuth = `${wsUrl}${sep}satoken=${token}`

        ws.value = new WebSocket(urlWithAuth)

        ws.value.onopen = (event) => {
            isConnected.value = true
            reconnectAttempts = 0
            options.onOpen?.(event)
        }

        ws.value.onmessage = (event) => {
            options.onMessage?.(event)
        }

        ws.value.onerror = (error) => {
            console.error('[WebSocket] 连接错误:', error)
            options.onError?.(error)
        }

        ws.value.onclose = (event) => {
            isConnected.value = false
            ws.value = null
            options.onClose?.(event)

            // 断线自动重连：指数退避策略
            if (options.autoReconnect && reconnectAttempts < maxReconnectAttempts) {
                reconnectAttempts++
                const delay = Math.min(
                    RECONNECT_BASE_DELAY_MS * Math.pow(2, reconnectAttempts),
                    RECONNECT_MAX_DELAY_MS
                )
                reconnectTimer = setTimeout(() => {
                    console.debug(`[WebSocket] 正在尝试第 ${reconnectAttempts} 次重连...`)
                    connect()
                }, delay)
            }
        }
    }

    /**
     * 发送数据到 WebSocket 服务端
     * @param {string|ArrayBuffer|Blob|Object} data - 待发送数据，对象自动 JSON 序列化
     */
    const send = (data) => {
        if (ws.value && isConnected.value) {
            const isRawType = typeof data === 'string' || data instanceof ArrayBuffer || data instanceof Blob
            ws.value.send(isRawType ? data : JSON.stringify(data))
        } else {
            console.warn('[WebSocket] 连接未就绪，无法发送数据')
        }
    }

    /** 主动关闭连接并取消重连计时器 */
    const close = () => {
        if (reconnectTimer) { clearTimeout(reconnectTimer) }
        if (ws.value) {
            ws.value.close()
            ws.value = null
        }
    }

    // 组件卸载时自动清理
    onUnmounted(() => {
        close()
    })

    return {
        isConnected,
        connect,
        send,
        close
    }
}

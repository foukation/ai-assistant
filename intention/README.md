# Intention SDK

## 项目简介

Intention SDK 是一个用于 Android 平台的意图识别框架，提供了多种识别策略和灵活的处理机制。

### 主要特性
- 多策略支持：默认策略、仅云端、本地优先、并行处理
- 多处理器：云端大模型、云端 BERT、本地 BERT
- 灵活配置：支持超时设置、重试机制、自定义策略
- 多种调用：支持同步、异步、协程三种调用方式
- 错误处理：完整的异常处理和错误回调机制

### 技术栈
- Kotlin 1.8+
- OkHttp 4.x
- Android API 21+
- Coroutines 1.6+

## 快速开始

### 集成步骤
1. 添加依赖
```gradle
dependencies {
    implementation 'com.agent:intention:1.0.0'
}
```

2. 添加网络权限
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

3. 初始化 SDK
```kotlin
IntentionApi.init {
    setDefaultStrategy(StrategyType.DEFAULT)
    setTimeout(30000L)
    setRetryCount(1)
}
```

### 基本使用

1. 异步调用
```kotlin
IntentionApi.newRequest()
    .input("导航去鸟巢")
    .onSuccess { response -> 
        // 处理响应
    }
    .onError { error ->
        // 处理错误
    }
    .execute()
```

2. 同步调用
```kotlin
val result = IntentionApi.newRequest()
    .input("导航去鸟巢")
    .executeSync()
```

3. 协程调用
```kotlin
val result = IntentionApi.newRequest()
    .input("导航去鸟巢")
    .executeAsync()
```

## 详细文档

### API 说明

#### IntentionApi
- `init`: SDK 初始化
- `newRequest`: 创建请求构建器

#### IntentionRequestBuilder
- `input`: 设置输入文本
- `strategy`: 设置策略类型
- `timeout`: 设置超时时间
- `retryCount`: 设置重试次数
- `execute`: 异步执行
- `executeSync`: 同步执行
- `executeAsync`: 协程执行

### 配置说明

#### 超时设置
- 默认超时：30000ms
- 可在请求时单独设置
- 支持全局配置

#### 重试机制
- 默认重试次数：1
- 支持请求级别设置
- 支持全局配置

#### 策略选择
- DEFAULT: 默认策略，按优先级尝试所有处理器
- CLOUD_ONLY: 仅使用云端处理器
- LOCAL_FIRST: 优先使用本地处理器
- PARALLEL: 并行使用所有处理器

### 自定义策略示例
```kotlin
class CustomStrategy : BaseCustomStrategy() {
    override val strategyType = StrategyType.CUSTOM
    
    override fun execute(
        request: IntentionRequest,
        onSuccess: (ActionResponse?) -> Unit,
        onError: (String) -> Unit
    ) {
        // 实现自定义逻辑
    }
}
```

### 输入输出格式

#### 输入格式
输入为纯文本字符串，例如：
```kotlin
val input = "导航去鸟巢"
```

#### 输出格式
输出为 `IntentionResult` 对象，包含以下字段：
```kotlin
data class IntentionResult(
    val success: Boolean,        // 是否成功
    val response: ActionResponse?, // 意图识别结果
    val error: String? = null,    // 错误信息
    val processorType: ProcessorType? = null  // 处理器类型
)
```

其中 `ActionResponse` 包含意图识别的具体内容：
```kotlin
data class ActionResponse(
    val intents: Array<IntentContent>  // 识别出的意图数组
)

data class IntentContent(
    val intent: String,     // 意图类型
    val slots: SlotsContent // 槽位信息
)

data class SlotsContent(
    val content: String?,      // 原始内容
    val product: String?,      // 产品
    val item: String?,         // 物品
    val recipient: String?,    // 接收者
    val location: String?,     // 位置
    val messageContent: String?, // 消息内容
    val destination: String?,   // 目的地
    val drinkName: String?,    // 饮品名称
    val shop: String?,         // 商店
    val receiver: String?,     // 接收人
    val store: String?,        // 店铺
    val times: String?,        // 次数/时间
    val extraSlots: Map<String, String> = emptyMap()  // 扩展字段，用于存储未知的槽位
)
```

示例输出：
```kotlin
// 成功示例
IntentionResult(
    success = true,
    response = ActionResponse(
        intents = arrayOf(
            IntentContent(
                intent = "NAVIGATION",
                slots = SlotsContent(
                    content = "导航去鸟巢",
                    destination = "鸟巢",
                    // 其他基础槽位为空
                    extraSlots = mapOf(
                        "transport_type" to "driving",  // 额外的槽位信息
                        "arrival_time" to "20分钟"
                    )
                )
            )
        )
    ),
    error = null,
    processorType = ProcessorType.CLOUD_LARGE_MODEL
)

// 失败示例
IntentionResult(
    success = false,
    response = null,
    error = "网络连接失败",
    processorType = null
)
```

## 架构说明

### 项目结构
```
intention/
├── api/                    # 公开 API 接口
│   ├── IntentionApi       # SDK 主入口
│   ├── IntentionConfig    # 配置接口
│   └── IntentionData      # 数据定义
├── core/                   # 核心实现
│   ├── IntentionManager   # 核心管理类
│   └── IntentionConfig    # 配置实现
├── strategy/              # 策略实现
│   ├── IntentionStrategy  # 策略接口
│   └── impl/             # 具体策略
├── processor/            # 处理器实现
│   ├── IntentionProcessor # 处理器接口
│   └── impl/             # 具体处理器
└── model/               # 数据模型
```

### 核心流程
1. 请求处理流程
   - 创建请求
   - 选择策略
   - 执行处理器
   - 返回结果

2. 策略选择流程
   - 检查自定义策略
   - 使用默认策略
   - 执行策略逻辑

3. 处理器调用流程
   - 准备处理器
   - 执行处理
   - 处理结果
   - 错误处理

### 设计决策

1. 策略模式
   - 灵活切换处理策略：支持在运行时根据需求切换不同的处理策略（默认、云端、本地优先、并行）
   - 支持自定义扩展：允许开发者通过继承 BaseCustomStrategy 实现自定义策略
   - 运行时动态选择：可以通过配置或请求级别动态指定使用的策略

2. 处理器链
   - 责任链模式：多个处理器（云端大模型、云端BERT、本地BERT）按照策略定义的顺序依次处理
   - 支持降级处理：当优先级高的处理器失败时，自动尝试下一个处理器
   - 并行处理支持：可以同时调用多个处理器，采用最快返回的有效结果

3. 配置管理
   - Builder 模式：使用 Builder 模式进行 SDK 初始化和配置管理
   - 分层配置：支持全局配置（SDK 初始化时）和请求级配置（每次请求时）
   - 运行时可修改：支持在运行时动态修改超时、重试次数等配置

## 开发指南

### 环境要求
- Android Studio Arctic Fox 或更高版本
- JDK 11 或更高版本
- Android SDK API 21+
- Kotlin 插件支持

### 测试说明
1. 单元测试
   - 策略测试
   - 处理器测试
   - 配置测试

2. 集成测试
   - 完整流程测试
   - 网络请求测试
   - 错误处理测试

3. Mock 测试
   - 网络请求 Mock
   - 处理器 Mock
   - 策略 Mock

### 调试方法
1. 日志输出
   - 使用 TAG: "IntentionSDK"
   - 详细的错误信息
   - 性能指标记录

2. 错误处理
   - 网络错误
   - 超时处理
   - 降级处理

3. 性能监控
   - 请求耗时
   - 内存使用
   - CPU 占用

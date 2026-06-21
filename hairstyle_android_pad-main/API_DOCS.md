# 发型/发色变换 API 文档

> 本文档适用于 Android WebView 前端（JavaScript）调用原生换发型/换发色功能。

---

## 目录

- [概述](#概述)
- [基础 API（底层方法）](#基础-api底层方法)
- [高级 API（一键封装）](#高级-api一键封装)
- [完整调用流程图](#完整调用流程图)
- [错误码说明](#错误码说明)
- [前端最佳实践](#前端最佳实践)
- [数据结构](#数据结构)

---

## 概述

App 通过 `AndroidBridge` 对象向 WebView 前端暴露 JavaScript 接口。前端通过 `window.AndroidBridge` 调用原生功能。

所有异步方法返回 **Promise**，支持 `.then()` / `.catch()` 链式调用。

### 快速开始

```javascript
// 1. 确认在 Android 环境中
if (AndroidBridge.isAndroid()) {
    // 2. 调用高级封装
    AndroidBridge.changeHairstyle(userPhotoBase64, hairstyleBase64)
        .then(function(result) {
            console.log('换发型完成', result.result_urls);
        })
        .catch(function(err) {
            console.error('失败', err);
        });
}
```

---

## 基础 API（底层方法）

底层 API 提供原子操作，适合需要自定义流程控制的场景。

### 拍照

```javascript
AndroidBridge.takePhoto()
```

**返回：** `Promise<{ success: boolean, base64: string }>`

| 字段 | 类型 | 说明 |
|---|---|---|
| success | boolean | 是否成功 |
| base64 | string | 照片 JPEG Base64 字符串（不含 data:image 前缀） |

**示例：**
```javascript
AndroidBridge.takePhoto().then(function(result) {
    var base64 = result.base64;
    // 显示预览
    document.getElementById('preview').src = 'data:image/jpeg;base64,' + base64;
});
```

---

### 从相册选择图片

```javascript
AndroidBridge.pickImage()
```

**返回：** `Promise<{ success: boolean, base64: string }>`

参数与返回值同 `takePhoto()`。

---

### 创建会话

```javascript
AndroidBridge.createSession()
```

**返回：** `Promise<{ success: boolean, session_id: string }>`

| 字段 | 类型 | 说明 |
|---|---|---|
| success | boolean | 是否成功 |
| session_id | string | 会话唯一标识，后续所有操作需要使用此 ID |

---

### 上传图片

```javascript
AndroidBridge.uploadImage(sessionId, imageType, base64Data)
```

| 参数 | 类型 | 说明 |
|---|---|---|
| sessionId | string | 会话 ID |
| imageType | string | 图片类型：`"user"`（用户照片）或 `"hairstyle"`（发型/发色参考图） |
| base64Data | string | 图片 Base64 数据 |

**返回：** `Promise<{ success: boolean, url: string }>`

---

### 启动 AI 处理

```javascript
AndroidBridge.startProcess(sessionId, processType)
```

| 参数 | 类型 | 说明 |
|---|---|---|
| sessionId | string | 会话 ID |
| processType | string | 处理类型：`"hairstyle"`（换发型）、`"haircolor"`（换发色）、`"3d"`（3D 转视频） |

**返回：** `Promise<{ success: boolean }>`

> ⚠️ 此方法仅启动异步处理任务，需配合 `getSessionStatus()` 轮询结果。

---

### 获取会话状态

```javascript
AndroidBridge.getSessionStatus(sessionId)
```

| 参数 | 类型 | 说明 |
|---|---|---|
| sessionId | string | 会话 ID |

**返回：** `Promise<{ success: boolean, status: string, result_urls?: string[], ... }>`

**status 取值：**

| 值 | 说明 |
|---|---|
| `"created"` | 已创建 |
| `"processing"` | 处理中 |
| `"completed"` | 处理完成 |
| `"failed"` | 处理失败 |

---

### 获取发型库列表

```javascript
AndroidBridge.getHairstyleList(gender)
```

| 参数 | 类型 | 说明 |
|---|---|---|
| gender | string | `"女"` 或 `"男"` |

**返回：** `Promise<{ success: boolean, hairstyles: Hairstyle[] }>`

**Hairstyle 对象：**

| 字段 | 类型 | 说明 |
|---|---|---|
| category | string | 分类（如"韩式"、"甜酷风"等） |
| name | string | 发型名称 |
| description | string | 发型描述 |
| fileName | string | 文件名 |
| path | string | 本地路径（`file:///android_asset/...`） |

---

### 获取发型图片

```javascript
AndroidBridge.getHairstyleImage(path)
```

| 参数 | 类型 | 说明 |
|---|---|---|
| path | string | 发型图片路径（来自 `getHairstyleList` 返回的 `path` 字段） |

**返回：** `Promise<{ success: boolean, base64: string }>`

---

### 获取发色库数据

```javascript
AndroidBridge.getHairColorsData()
```

**返回：** `Promise<Array>` — 发色 JSON 数据数组，按色系分组。

---

### 获取 Assets 图片

```javascript
AndroidBridge.getAssetImageBase64(assetPath)
```

| 参数 | 类型 | 说明 |
|---|---|---|
| assetPath | string | assets 中的相对路径，如 `"hair_colors/温感色系_蜂蜜茶色.png"` |

**返回：** `Promise<{ success: boolean, base64: string }>`

---

### 生成二维码

```javascript
AndroidBridge.generateUploadQRCode(imageType)
// 或为已有会话生成：
AndroidBridge.generateQRCodeForSession(sessionId, imageType)
```

| 参数 | 类型 | 说明 |
|---|---|---|
| imageType | string | `"user"` 或 `"hairstyle"` |
| sessionId | string | 已有会话 ID |

**返回：** `Promise<{ success: boolean, session_id: string, upload_url: string, qrcode_base64: string }>`

---

### 取消任务

```javascript
AndroidBridge.cancelSession(sessionId)
```

**返回：** `Promise<{ success: boolean, message: string }>`

---

### 工具方法

```javascript
AndroidBridge.showToast(message)    // 显示 Toast 提示
AndroidBridge.isAndroid()           // 判断是否在 Android WebView 中运行
AndroidBridge.checkPermission(name) // 检查权限（"camera"）
AndroidBridge.requestPermission(name) // 请求权限
AndroidBridge.fetchImageAsBase64(url) // 从 URL 获取图片转 Base64
AndroidBridge.saveImage(base64, fileName) // 保存图片到相册
AndroidBridge.shareImage(base64)     // 分享图片
```

---

## 高级 API（一键封装）

高级 API 将 **创建会话 → 上传图片 → AI 处理 → 轮询结果** 整个流程封装为一个方法调用，前端只需等待结果即可。

### 一键换发型

```javascript
AndroidBridge.changeHairstyle(userBase64, hairstyleBase64)
```

#### 参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| userBase64 | string | 是 | 用户照片 JPEG Base64（不含 `data:image` 前缀） |
| hairstyleBase64 | string | 是 | 目标发型参考图 Base64 |

#### 返回 Promise

```javascript
{
    status: "completed",              // 最终状态
    session_id: "abc123",             // 会话 ID
    result_urls: [
        "https://.../result_0.jpg",   // 生成结果图片 URL
        "https://.../result_1.jpg"
    ],
    user_image_url: "https://.../user.jpg",      // 用户原始照片 URL
    hairstyle_image_url: "https://.../hair.jpg"  // 发型参考图 URL
}
```

#### 进度事件监听

在处理过程中，会通过 jQuery 事件触发进度更新：

```javascript
$(document).on('hairstyle:progress', function(e, data) {
    // data 内容：
    // {
    //     status: "uploading" | "processing",
    //     progress: 5~95,         // 0-100 的进度值
    //     message: "正在上传用户照片..."
    // }
    
    $('#progressBar').css('width', data.progress + '%');
    $('#statusText').text(data.message);
});

// 完成事件
$(document).on('hairstyle:complete', function(e, data) {
    // 显示结果图片
    data.result_urls.forEach(function(url) {
        $('#results').append('<img src="' + url + '">');
    });
});

// 错误事件
$(document).on('hairstyle:error', function(e, data) {
    alert('换发型失败: ' + data.message);
});
```

#### 完整示例

```html
<script>
$(document).on('hairstyle:progress', function(e, data) {
    $('#progress').text(data.progress + '%');
    $('#msg').text(data.message);
});

function onUserPhotoTaken(base64) {
    window.userPhotoBase64 = base64;
}

function onHairstyleSelected(base64) {
    AndroidBridge.changeHairstyle(window.userPhotoBase64, base64)
        .then(function(result) {
            // 显示结果
            result.result_urls.forEach(function(url) {
                $('#gallery').append('<img src="' + url + '">');
            });
        })
        .catch(function(err) {
            alert('失败: ' + err.message);
        });
}
</script>
```

---

### 一键换发色

```javascript
AndroidBridge.changeHairColor(userBase64, colorBase64)
```

#### 参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| userBase64 | string | 是 | 用户照片 JPEG Base64 |
| colorBase64 | string | 是 | 目标发色参考图 Base64（可从发色库获取） |

#### 返回 Promise + 进度事件

与 `changeHairstyle` 结构相同，但事件前缀为 `haircolor`：

```javascript
$(document).on('haircolor:progress', function(e, data) { /* 处理进度 */ });
$(document).on('haircolor:complete', function(e, data) { /* 显示结果 */ });
$(document).on('haircolor:error', function(e, data) { /* 错误处理 */ });
```

#### 完整示例（结合发色库）

```javascript
// 1. 获取发色库数据
AndroidBridge.getHairColorsData().then(function(colors) {
    // 渲染发色列表
    colors.forEach(function(category) {
        category.colors.forEach(function(color) {
            // 点击发色时执行换发色
            $('#color-' + color.id).on('click', function() {
                AndroidBridge.getAssetImageBase64(
                    'hair_colors/' + category.name + '_' + color.name + '.png'
                ).then(function(result) {
                    return AndroidBridge.changeHairColor(
                        window.userPhotoBase64,
                        result.base64
                    );
                }).then(function(result) {
                    // 显示换发色结果
                    $('#result').attr('src', result.result_urls[0]);
                });
            });
        });
    });
});
```

---

### 进度事件数据格式

在 `uploading` 和 `processing` 阶段触发：

| 字段 | 类型 | 说明 |
|---|---|---|
| status | string | 当前阶段：`"uploading"` 或 `"processing"` |
| progress | number | 进度百分比（0-100） |
| message | string | 中文描述信息 |

**各阶段进度对照：**

| 进度 | 阶段 | 说明 |
|---|---|---|
| 5% | uploading | 正在创建会话 |
| 20% | uploading | 正在上传用户照片 |
| 40% | uploading | 正在上传发型/发色参考图 |
| 60~95% | processing | AI 分析处理中 |
| 100% | completed | 完成 |

---

## 完整调用流程图

```
┌─────────────────────────────────────────────────────┐
│                   前端 WebView                        │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌─────────────────────────────────────────┐        │
│  │        高级 API (推荐)                   │        │
│  │                                         │        │
│  │  AndroidBridge.changeHairstyle()        │        │
│  │  AndroidBridge.changeHairColor()        │        │
│  │         │                                     │
│  │         ├─ 进度事件: hairstyle:progress        │
│  │         ├─ 完成事件: hairstyle:complete         │
│  │         └─ 错误事件: hairstyle:error            │
│  └─────────────────────────────────────────┘        │
│                                                     │
│  ┌─────────────────────────────────────────┐        │
│  │        基础 API (自定义流程)              │        │
│  │                                         │        │
│  │  ① createSession()                      │        │
│  │  ② uploadImage("user")                  │        │
│  │  ③ uploadImage("hairstyle")             │        │
│  │  ④ startProcess("hairstyle")            │        │
│  │  ⑤ getSessionStatus() 轮询 (3s间隔)     │        │
│  └─────────────────────────────────────────┘        │
│                                                     │
└──────────────────────┬──────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│                原生 Android 层                        │
├─────────────────────────────────────────────────────┤
│                                                     │
│  AndroidBridge.kt (JavaScriptInterface)              │
│         │                                           │
│         ├─ createSession() → POST /api/create-session │
│         ├─ uploadImage()   → POST /api/upload/...    │
│         ├─ startProcess()  → POST /api/process/...   │
│         └─ getSessionStatus() → GET /api/session/... │
│                                                     │
└──────────────────────┬──────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│               后端 API 服务器                         │
│         (web-production-bingli.up.railway.app)       │
├─────────────────────────────────────────────────────┤
│                                                     │
│  POST /api/create-session       创建会话              │
│  POST /api/upload/{id}/{type}   上传图片              │
│  POST /api/process/{id}         启动换发型处理         │
│  POST /api/process-color/{id}   启动换发色处理         │
│  GET  /api/session/{id}         查询会话状态           │
│  POST /api/cancel-session/{id}  取消任务              │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

## 生命周期状态图

```
  ┌──────────┐
  │ CREATED  │ ← createSession()
  └────┬─────┘
       │
  ┌────▼─────┐     uploadImage()
  │ UPLOADING│ ──────────────────►
  └────┬─────┘
       │
  ┌────▼────────┐  startProcess()
  │ PROCESSING  │ ────────────────►
  └────┬────────┘
       │
       ├──► ┌───────────┐
       │    │ COMPLETED │ ← 显示结果图片
       │    └───────────┘
       │
       └──► ┌────────┐
            │ FAILED │ ← 提示错误
            └────────┘
```

---

## 错误码说明

| 错误信息 | 可能原因 | 处理建议 |
|---|---|---|
| `创建会话失败` | 网络不可用 / 服务器异常 | 检查网络连接后重试 |
| `上传用户照片失败` | 图片过大 / 网络中断 | 压缩图片后重试 |
| `上传发型参考图失败` | 图片格式不支持 / 网络中断 | 使用 JPEG 或 PNG 图片 |
| `AI 处理失败` | 服务器处理异常 | 稍后重试 |
| `处理超时` | 服务器响应超过 3 分钟 | 重新发起请求 |
| `任务已取消` | 用户主动取消 | 无 |

---

## 前端最佳实践

### 1. 进度条 UI

```javascript
// 监听进度
$(document).on('hairstyle:progress', function(e, data) {
    $('#progressBar').css('width', data.progress + '%');
    $('#progressText').text(data.message);
    
    // 切换步骤图标
    if (data.progress >= 20) $('#step1').addClass('done');
    if (data.progress >= 40) $('#step2').addClass('done');
    if (data.progress >= 60) $('#step3').addClass('done');
});
```

### 2. 错误重试

```javascript
function retryChangeHairstyle(userBase64, hairBase64, maxRetries) {
    maxRetries = maxRetries || 2;
    
    function attempt(retryCount) {
        return AndroidBridge.changeHairstyle(userBase64, hairBase64)
            .catch(function(err) {
                if (retryCount < maxRetries) {
                    return attempt(retryCount + 1);
                }
                throw err;
            });
    }
    
    return attempt(0);
}
```

### 3. 并发保护

```javascript
// 防止用户重复点击
var isProcessing = false;

function onClickChangeHairstyle() {
    if (isProcessing) return;
    isProcessing = true;
    
    AndroidBridge.changeHairstyle(userBase64, hairBase64)
        .then(function(result) {
            // 显示结果
        })
        .catch(function(err) {
            alert(err.message);
        })
        .finally(function() {
            isProcessing = false;
        });
}
```

---

## 数据结构

### HairstyleTemplate（发型模板）

```javascript
{
    id: "female_0",                // 唯一标识
    name: "复古齐肩卷发",           // 发型名称
    description: "长度在锁骨处，发尾S形卷度...",  // 发型描述
    imageUrl: "file:///...",       // 图片路径
    category: "日式",              // 分类
    gender: "女"                   // 适用性别
}
```

### HairColorTemplate（发色模板）

```javascript
{
    id: "color_0",                 // 唯一标识
    name: "蜂蜜茶色",              // 发色名称
    category: "温感色系",          // 色系分类
    imageUrl: "hair_colors/...",   // 色样图片路径
    procedure: ""                  // 标准流程（预留）
}
```

### SessionStatusResponse（会话状态）

```javascript
{
    session_id: "abc123",
    has_user_image: true,
    has_hairstyle_image: true,
    user_image_url: "https://...",
    hairstyle_image_url: "https://...",
    status: "processing" | "completed" | "failed",
    ready_to_process: true,
    task_id: "task_xxx",
    result_urls: ["https://.../result_0.jpg"]
}
```

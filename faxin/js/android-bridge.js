/**
 * Android Bridge - 封装与Android原生代码的交互
 * 提供Promise风格的异步接口
 */
window.AndroidBridge = {

    // 检查是否在Android WebView中运行
    isAndroid: function() {
        return typeof Android !== 'undefined';
    },

    // 生成唯一回调名称
    _generateCallbackName: function(prefix) {
        return prefix + '_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    },

    // 注册回调并返回Promise
    _callWithCallback: function(methodName, args, callbackPrefix) {
        var self = this;
        return new Promise(function(resolve, reject) {
            if (!self.isAndroid()) {
                reject(new Error('Not running in Android WebView'));
                return;
            }

            var callbackName = self._generateCallbackName(callbackPrefix);

            // 注册全局回调函数
            window[callbackName] = function(result) {
                // 清理回调
                delete window[callbackName];

                try {
                    var data = typeof result === 'string' ? JSON.parse(result) : result;
                    if (data.success) {
                        resolve(data);
                    } else {
                        reject(new Error(data.error || 'Unknown error'));
                    }
                } catch (e) {
                    reject(e);
                }
            };

            // 调用Android方法
            try {
                args.push(callbackName);
                Android[methodName].apply(Android, args);
            } catch (e) {
                delete window[callbackName];
                reject(e);
            }
        });
    },

    /**
     * 拍照
     * @returns {Promise<{success: boolean, base64: string}>}
     */
    takePhoto: function() {
        return this._callWithCallback('takePhoto', [], 'takePhotoCallback');
    },

    /**
     * 从相册选择图片
     * @returns {Promise<{success: boolean, base64: string}>}
     */
    pickImage: function() {
        return this._callWithCallback('pickImage', [], 'pickImageCallback');
    },

    /**
     * 上传图片到服务器
     * @param {string} sessionId - 会话ID
     * @param {string} imageType - 图片类型: 'user' 或 'hairstyle'
     * @param {string} base64Data - 图片Base64数据
     * @returns {Promise<{success: boolean, url: string}>}
     */
    uploadImage: function(sessionId, imageType, base64Data) {
        return this._callWithCallback('uploadImage', [sessionId, imageType, base64Data], 'uploadImageCallback');
    },

    /**
     * 创建新会话
     * @returns {Promise<{success: boolean, session_id: string}>}
     */
    createSession: function() {
        return this._callWithCallback('createSession', [], 'createSessionCallback');
    },

    /**
     * 获取会话状态
     * @param {string} sessionId - 会话ID
     * @returns {Promise<{success: boolean, status: string, results: Array}>}
     */
    getSessionStatus: function(sessionId) {
        return this._callWithCallback('getSessionStatus', [sessionId], 'getSessionStatusCallback');
    },

    /**
     * 开始AI处理
     * @param {string} sessionId - 会话ID
     * @param {string} processType - 处理类型: 'hairstyle' 或 'haircolor'
     * @returns {Promise<{success: boolean}>}
     */
    startProcess: function(sessionId, processType) {
        return this._callWithCallback('startProcess', [sessionId, processType], 'startProcessCallback');
    },

    /**
     * 获取发型库列表
     * @param {string} gender - 性别: '男' 或 '女'
     * @returns {Promise<{success: boolean, hairstyles: Array}>}
     */
    getHairstyleList: function(gender) {
        return this._callWithCallback('getHairstyleList', [gender || '女'], 'getHairstyleListCallback');
    },

    /**
     * 获取发型图片Base64
     * @param {string} path - 发型图片路径
     * @returns {Promise<{success: boolean, base64: string}>}
     */
    getHairstyleImage: function(path) {
        return this._callWithCallback('getHairstyleImage', [path], 'getHairstyleImageCallback');
    },

    /**
     * 显示Toast消息
     * @param {string} message - 消息内容
     */
    showToast: function(message) {
        if (this.isAndroid()) {
            Android.showToast(message);
        } else {
            alert(message);
        }
    },

    /**
     * 检查权限
     * @param {string} permission - 权限名称: 'camera' 或 'storage'
     * @returns {Promise<{success: boolean, granted: boolean}>}
     */
    checkPermission: function(permission) {
        return this._callWithCallback('checkPermission', [permission], 'checkPermissionCallback');
    },

    /**
     * 请求权限
     * @param {string} permission - 权限名称: 'camera' 或 'storage'
     * @returns {Promise<{success: boolean, granted: boolean}>}
     */
    requestPermission: function(permission) {
        return this._callWithCallback('requestPermission', [permission], 'requestPermissionCallback');
    },

    /**
     * 保存图片到相册
     * @param {string} base64Data - 图片Base64数据
     * @param {string} fileName - 文件名
     * @returns {Promise<{success: boolean}>}
     */
    saveImage: function(base64Data, fileName) {
        return this._callWithCallback('saveImage', [base64Data, fileName], 'saveImageCallback');
    },

    /**
     * 分享图片
     * @param {string} base64Data - 图片Base64数据
     * @returns {Promise<{success: boolean}>}
     */
    shareImage: function(base64Data) {
        return this._callWithCallback('shareImage', [base64Data], 'shareImageCallback');
    },

    /**
     * 生成上传二维码
     * @param {string} imageType - 图片类型: 'user' 或 'hairstyle'
     * @returns {Promise<{success: boolean, session_id: string, upload_url: string, qrcode_base64: string}>}
     */
    generateUploadQRCode: function(imageType) {
        return this._callWithCallback('generateUploadQRCode', [imageType], 'generateUploadQRCodeCallback');
    },

    /**
     * 从URL获取图片并转换为Base64
     * @param {string} imageUrl - 图片URL
     * @returns {Promise<{success: boolean, base64: string}>}
     */
    fetchImageAsBase64: function(imageUrl) {
        return this._callWithCallback('fetchImageAsBase64', [imageUrl], 'fetchImageCallback');
    },

    /**
     * 为已有会话生成上传二维码（不创建新会话）
     * @param {string} sessionId - 已有的会话ID
     * @param {string} imageType - 图片类型: 'user' 或 'hairstyle'
     * @returns {Promise<{success: boolean, session_id: string, upload_url: string, qrcode_base64: string}>}
     */
    generateQRCodeForSession: function(sessionId, imageType) {
        return this._callWithCallback('generateQRCodeForSession', [sessionId, imageType], 'generateQRCodeForSessionCallback');
    },

    /**
     * 获取Assets目录下图片的Base64
     * @param {string} assetPath - Assets中的相对路径，如 'hair_colors/温感色系_蜂蜜茶色.png'
     * @returns {Promise<{success: boolean, base64: string}>}
     */
    getAssetImageBase64: function(assetPath) {
        return this._callWithCallback('getAssetImageBase64', [assetPath], 'getAssetImageCallback');
    },

    /**
     * 获取发色库数据
     * @returns {Promise<Array>} 发色数据数组
     */
    getHairColorsData: function() {
        var self = this;
        return new Promise(function(resolve, reject) {
            if (!self.isAndroid()) {
                reject(new Error('Not running in Android WebView'));
                return;
            }

            var callbackName = self._generateCallbackName('getHairColorsDataCallback');

            // 注册全局回调函数 - 这个回调直接接收JSON数组
            window[callbackName] = function(result) {
                delete window[callbackName];
                try {
                    // result 可能是数组或包含error的对象
                    if (Array.isArray(result)) {
                        resolve(result);
                    } else if (result && result.error) {
                        reject(new Error(result.error));
                    } else {
                        resolve(result);
                    }
                } catch (e) {
                    reject(e);
                }
            };

            try {
                Android.getHairColorsData(callbackName);
            } catch (e) {
                delete window[callbackName];
                reject(e);
            }
        });
    },

    /**
     * 取消会话任务
     * @param {string} sessionId - 会话ID
     * @returns {Promise<{success: boolean, message: string}>}
     */
    cancelSession: function(sessionId) {
        return this._callWithCallback('cancelSession', [sessionId], 'cancelSessionCallback');
    },

    // ========================================================================
    //  高级封装 —— 前端一键调用，自动完成完整流程
    // ========================================================================

    /**
     * 内部方法：处理多次回调（进度更新）的高级 API
     * 回调函数不会被自动删除，直到 status 为 'completed' 或 'failed'
     */
    _callWithProgress: function(methodName, args, eventPrefix) {
        var self = this;
        return new Promise(function(resolve, reject) {
            if (!self.isAndroid()) {
                reject(new Error('Not running in Android WebView'));
                return;
            }

            var callbackName = methodName + '_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);

            // 注册持久回调 —— 原生端会多次调用此函数发送进度更新
            window[callbackName] = function(result) {
                var data = typeof result === 'string' ? JSON.parse(result) : result;

                // 触发进度事件，前端可监听 $(document).on('hairstyle:progress', fn)
                if (data.status === 'uploading' || data.status === 'processing') {
                    $(document).trigger(eventPrefix + ':progress', [data]);
                    return; // 持续等待，不删除回调
                }

                // 最终状态（completed / failed）—— 清理回调并 resolve/reject
                delete window[callbackName];

                if (data.status === 'completed') {
                    $(document).trigger(eventPrefix + ':complete', [data]);
                    resolve(data);
                } else if (data.status === 'failed') {
                    $(document).trigger(eventPrefix + ':error', [data]);
                    reject(new Error(data.error || data.message || '处理失败'));
                }
            };

            try {
                Android[methodName].apply(Android, args.concat([callbackName]));
            } catch (e) {
                delete window[callbackName];
                reject(e);
            }
        });
    },

    /**
     * 一键换发型
     *
     * 自动完成：创建会话 → 上传照片 → 上传发型参考图 → AI 处理 → 返回结果
     * 通过事件监听进度：$(document).on('hairstyle:progress', fn)
     *
     * @param {string} userBase64      - 用户照片 Base64
     * @param {string} hairstyleBase64 - 发型参考图 Base64
     * @returns {Promise<{
     *   status: string,
     *   session_id: string,
     *   result_urls: string[],
     *   user_image_url: string,
     *   hairstyle_image_url: string
     * }>}
     *
     * @example
     * // 监听进度
     * $(document).on('hairstyle:progress', function(e, data) {
     *   console.log(data.progress + '%: ' + data.message);
     * });
     * // 调用
     * AndroidBridge.changeHairstyle(userBase64, hairstyleBase64)
     *   .then(function(result) {
     *     console.log('完成!', result.result_urls);
     *   })
     *   .catch(function(err) {
     *     console.error('失败', err);
     *   });
     */
    changeHairstyle: function(userBase64, hairstyleBase64) {
        return this._callWithProgress('changeHairstyle', [userBase64, hairstyleBase64], 'hairstyle');
    },

    /**
     * 一键换发色
     *
     * 自动完成：创建会话 → 上传照片 → 上传发色参考图 → AI 处理 → 返回结果
     * 通过事件监听进度：$(document).on('haircolor:progress', fn)
     *
     * @param {string} userBase64  - 用户照片 Base64
     * @param {string} colorBase64 - 目标发色参考图 Base64
     * @returns {Promise<{
     *   status: string,
     *   session_id: string,
     *   result_urls: string[],
     *   user_image_url: string,
     *   hairstyle_image_url: string
     * }>}
     *
     * @example
     * $(document).on('haircolor:progress', function(e, data) {
     *   $('#progress').text(data.progress + '%');
     *   $('#message').text(data.message);
     * });
     * AndroidBridge.changeHairColor(userBase64, colorBase64)
     *   .then(function(result) {
     *     $('#result img').attr('src', result.result_urls[0]);
     *   });
     */
    changeHairColor: function(userBase64, colorBase64) {
        return this._callWithProgress('changeHairColor', [userBase64, colorBase64], 'haircolor');
    }
};

// 为非Android环境提供降级支持
if (!window.AndroidBridge.isAndroid()) {
    console.log('AndroidBridge: Running in non-Android environment, some features may not work');
}

/**
 * 发型APP - 业务逻辑JS
 */

(function() {
    'use strict';

    // 全局状态
    window.AppState = {
        sessionId: null,
        userPhotoBase64: null,
        hairstylePhotoBase64: null,
        selectedHairstyle: null,
        currentGender: '女',
        hairstyles: []
    };

    // 从sessionStorage恢复状态
    function restoreState() {
        try {
            var savedSessionId = sessionStorage.getItem('hairstyle_session_id');
            if (savedSessionId) {
                window.AppState.sessionId = savedSessionId;
                console.log('Restored session ID:', savedSessionId);
            }
        } catch (e) {
            console.error('Failed to restore state:', e);
        }
    }

    // 保存sessionId到sessionStorage
    function saveSessionId(sessionId) {
        try {
            sessionStorage.setItem('hairstyle_session_id', sessionId);
            window.AppState.sessionId = sessionId;
            console.log('Saved session ID:', sessionId);
        } catch (e) {
            console.error('Failed to save session ID:', e);
        }
    }

    // 立即恢复状态
    restoreState();

    // rem适配 - 统一 REM 计算
    // 兼容所有屏幕：让 7.5rem 始终等于屏幕宽度，内容自动填满
    function setRem() {
        var html = document.documentElement;
        var clientWidth = html.clientWidth;

        // 获取实际屏幕宽度（优先使用 window.innerWidth，更准确）
        var actualWidth = window.innerWidth || html.clientWidth;

        // 让 7.5rem = 屏幕宽度，这样 .layout (width: 7.5rem) 就会填满屏幕
        var targetRem = actualWidth / 7.5;
        html.style.fontSize = targetRem + 'px';

        // 设置 viewport 为屏幕实际宽度
        var viewport = document.querySelector('meta[name="viewport"]');
        if (viewport) {
            viewport.setAttribute('content', 'width=' + actualWidth + ', initial-scale=1.0, minimum-scale=1.0, maximum-scale=1.0, user-scalable=no');
        }

        console.log('setRem: actualWidth=' + actualWidth + ', fontSize=' + targetRem + 'px');
    }

    // 初始化rem - 在页面加载完成后执行
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', setRem);
    } else {
        setRem();
    }
    window.addEventListener('resize', setRem);

    // DOM Ready
    $(function() {
        initApp();
    });

    // 应用初始化
    function initApp() {
        // 如果在Android环境中，添加android-bridge.js
        if (typeof AndroidBridge !== 'undefined' && AndroidBridge.isAndroid()) {
            console.log('Running in Android WebView');
            initAndroidFeatures();
        } else {
            console.log('Running in browser');
        }

        // 初始化页面特定功能
        initPageFeatures();
    }

    // 初始化Android特有功能
    function initAndroidFeatures() {
        // 绑定拍照按钮（替代HTML文件选择）
        bindCameraButton();
    }

    // 绑定相机按钮
    function bindCameraButton() {
        // page1.html 和 page2.html 中的相机图标已改为跳转到发型库/发色库，不需要绑定相机事件
        var pathname = window.location.pathname;
        if (pathname.indexOf('page1.html') > -1 || pathname.indexOf('page2.html') > -1) {
            return; // 不绑定相机按钮
        }

        // home.html 中的相机图标
        $('.hpic .bflex .icon:first-child').off('click').on('click', function(e) {
            e.preventDefault();
            e.stopPropagation();

            // 隐藏原有的file input
            $(this).find('input[type="file"]').hide();

            // 直接调用拍照
            takePhoto();
        });
    }

    // 显示拍照/选择相册选项
    function showPhotoOptions() {
        if (typeof AndroidBridge !== 'undefined' && AndroidBridge.isAndroid()) {
            // 直接显示选择对话框
            var isUserPhoto = window.location.href.indexOf('home.html') > -1;
            var title = isUserPhoto ? '上传您的照片' : '选择参考发型';

            // 创建选择弹窗
            var html = '<div class="photo-options-mask">' +
                '<div class="photo-options-dialog">' +
                '<h3>' + title + '</h3>' +
                '<div class="photo-options-btns">' +
                '<a href="javascript:;" class="photo-option-btn" data-action="camera">拍照</a>' +
                '<a href="javascript:;" class="photo-option-btn" data-action="gallery">从相册选择</a>' +
                '<a href="javascript:;" class="photo-option-btn cancel" data-action="cancel">取消</a>' +
                '</div></div></div>';

            $('body').append(html);

            // 绑定按钮事件
            $('.photo-option-btn').on('click', function() {
                var action = $(this).data('action');
                $('.photo-options-mask').remove();

                if (action === 'camera') {
                    takePhoto();
                } else if (action === 'gallery') {
                    pickImage();
                }
            });
        }
    }

    // 拍照
    function takePhoto() {
        if (!AndroidBridge.isAndroid()) {
            alert('拍照功能仅在APP中可用');
            return;
        }

        AndroidBridge.takePhoto()
            .then(function(result) {
                handlePhotoResult(result.base64);
            })
            .catch(function(error) {
                AndroidBridge.showToast('拍照失败: ' + error);
            });
    }

    // 从相册选择
    function pickImage() {
        if (!AndroidBridge.isAndroid()) {
            alert('选择图片功能仅在APP中可用');
            return;
        }

        AndroidBridge.pickImage()
            .then(function(result) {
                handlePhotoResult(result.base64);
            })
            .catch(function(error) {
                AndroidBridge.showToast('选择图片失败: ' + error);
            });
    }

    // 处理照片结果
    function handlePhotoResult(base64) {
        var isUserPhoto = window.location.href.indexOf('home.html') > -1;

        // 显示图片预览
        var imgSrc = 'data:image/jpeg;base64,' + base64;
        $('#pics').attr('src', imgSrc);

        // 保存到全局状态
        if (isUserPhoto) {
            AppState.userPhotoBase64 = base64;

            // 上传用户照片到服务器
            uploadUserPhotoToServer(base64);
        } else {
            AppState.hairstylePhotoBase64 = base64;
        }
    }

    // 上传用户照片到服务器
    function uploadUserPhotoToServer(base64) {
        if (!AndroidBridge.isAndroid()) return;

        // 检查是否已有session，如果没有则创建
        var existingSessionId = AppState.sessionId;
        if (!existingSessionId) {
            try {
                existingSessionId = sessionStorage.getItem('hairstyle_session_id');
            } catch (e) {}
        }

        if (existingSessionId) {
            // 已有session，直接上传
            doUploadUserPhoto(existingSessionId, base64);
        } else {
            // 创建新session后上传
            AndroidBridge.createSession()
                .then(function(result) {
                    var sessionId = result.session_id;
                    AppState.sessionId = sessionId;
                    try {
                        sessionStorage.setItem('hairstyle_session_id', sessionId);
                    } catch (e) {}
                    doUploadUserPhoto(sessionId, base64);
                })
                .catch(function(error) {
                    AndroidBridge.showToast('创建会话失败: ' + error);
                });
        }
    }

    // 执行上传用户照片
    function doUploadUserPhoto(sessionId, base64) {
        AndroidBridge.uploadImage(sessionId, 'user', base64)
            .then(function(result) {
                AndroidBridge.showToast('照片上传成功！');
                // 显示下一步按钮
                $('.wshow').hide();
                $('.whide').show();
            })
            .catch(function(error) {
                AndroidBridge.showToast('照片上传失败: ' + error);
            });
    }

    // 初始化页面特定功能
    function initPageFeatures() {
        var pathname = window.location.pathname;

        if (pathname.indexOf('faxlist.html') > -1) {
            initHairstyleListPage();
        } else if (pathname.indexOf('home.html') > -1) {
            initHomePage();
        } else if (pathname.indexOf('page1.html') > -1) {
            initPage1();
        }
    }

    // 初始化首页（上传照片页）
    function initHomePage() {
        // 创建会话
        if (AndroidBridge.isAndroid()) {
            createSession();
        }
    }

    // 创建会话
    function createSession() {
        AndroidBridge.createSession()
            .then(function(result) {
                saveSessionId(result.session_id);
                console.log('Session created:', result.session_id);
            })
            .catch(function(error) {
                console.error('Create session failed:', error);
            });
    }

    // 初始化page1（参考发型页）
    function initPage1() {
        // 绑定下一步按钮
        $('.flexbtn .btn').on('click', function(e) {
            // 可以在这里添加验证逻辑
        });
    }

    // 初始化发型库列表页
    function initHairstyleListPage() {
        // 加载发型库
        if (AndroidBridge.isAndroid()) {
            loadHairstyleList(AppState.currentGender);
        }

        // 性别切换
        $('.flextxt .rtc a.sex').off('click').on('click', function() {
            $(this).toggleClass('on');
            AppState.currentGender = $(this).hasClass('on') ? '男' : '女';
            loadHairstyleList(AppState.currentGender);
        });

        // 分类切换
        $('.htab li').off('click').on('click', function() {
            $(this).addClass('on').siblings('li').removeClass('on');
            var category = $(this).find('a').text().replace(/\s+/g, '');
            filterHairstyles(category);
        });

        // 发型选择
        $(document).off('click', '.piclist li').on('click', '.piclist li', function() {
            var $item = $(this);
            var hairstyle = $item.data('hairstyle');
            if (hairstyle) {
                AppState.selectedHairstyle = hairstyle;
                showHairstyleConfirm(hairstyle);
            }
        });

        // 确认/取消弹窗
        $('.flexbtn .rbt1').off('click').on('click', function() {
            $('.mask').hide();
            AppState.selectedHairstyle = null;
        });

        $('.flexbtn .rbt2').off('click').on('click', function() {
            if (AppState.selectedHairstyle) {
                // 上传发型图片后返回参考发型页面
                uploadSelectedHairstyle();
            }
        });
    }

    // 上传选中的发型图片
    function uploadSelectedHairstyle() {
        var sessionId = AppState.sessionId;
        if (!sessionId) {
            try {
                sessionId = sessionStorage.getItem('hairstyle_session_id');
            } catch (e) {}
        }

        if (!sessionId) {
            AndroidBridge.showToast('会话未创建，请返回首页重试');
            return;
        }

        if (!AppState.selectedHairstyle || !AppState.selectedHairstyle.path) {
            AndroidBridge.showToast('请先选择发型');
            return;
        }

        AndroidBridge.showToast('正在上传发型...');

        // 获取发型图片的Base64
        AndroidBridge.getHairstyleImage(AppState.selectedHairstyle.path)
            .then(function(result) {
                if (result.success && result.base64) {
                    // 保存发型图片base64到sessionStorage，供page1.html显示
                    try {
                        sessionStorage.setItem('selected_hairstyle_base64', result.base64);
                        sessionStorage.setItem('selected_hairstyle_name', AppState.selectedHairstyle.name || '');
                    } catch (err) {}
                    // 上传发型图片
                    return AndroidBridge.uploadImage(sessionId, 'hairstyle', result.base64);
                } else {
                    throw new Error('获取发型图片失败');
                }
            })
            .then(function() {
                // 上传成功，返回参考发型页面
                AndroidBridge.showToast('发型选择成功！');
                window.location.href = 'page1.html';
            })
            .catch(function(error) {
                AndroidBridge.showToast('上传发型失败: ' + error);
            });
    }

    // 加载发型库列表
    function loadHairstyleList(gender) {
        if (!AndroidBridge.isAndroid()) {
            console.log('Hairstyle list loading only works in Android');
            return;
        }

        AndroidBridge.getHairstyleList(gender)
            .then(function(result) {
                AppState.hairstyles = result.hairstyles || [];
                renderHairstyleList(AppState.hairstyles);
            })
            .catch(function(error) {
                console.error('Load hairstyle list failed:', error);
                AndroidBridge.showToast('加载发型库失败');
            });
    }

    // 渲染发型库列表
    function renderHairstyleList(hairstyles) {
        var html = '';
        hairstyles.forEach(function(item) {
            html += '<li data-hairstyle=\'' + JSON.stringify(item).replace(/'/g, "\\'") + '\'>' +
                '<a href="javascript:;">' +
                '<div class="icon"><img src="' + item.path + '"></div>' +
                '<div class="flex">' +
                '<h6>' + item.name + '</h6>' +
                '<i>' + (item.category || '') + '</i>' +
                '</div></a></li>';
        });

        $('.piclist ul').html(html);
    }

    // 过滤发型
    function filterHairstyles(category) {
        if (category === '全部') {
            renderHairstyleList(AppState.hairstyles);
        } else {
            var filtered = AppState.hairstyles.filter(function(item) {
                return item.category === category;
            });
            renderHairstyleList(filtered);
        }
    }

    // 显示发型确认弹窗
    function showHairstyleConfirm(hairstyle) {
        // 更新弹窗内容
        $('.mask .mpic img').attr('src', hairstyle.path);
        $('.mask .mpic i').text(hairstyle.name);
        $('.mask').fadeIn();
    }

    // 开始AI处理
    function startProcess() {
        if (!AppState.sessionId) {
            AndroidBridge.showToast('会话未创建，请返回首页重试');
            return;
        }

        if (!AppState.userPhotoBase64) {
            AndroidBridge.showToast('请先上传您的照片');
            return;
        }

        AndroidBridge.showToast('正在处理中...');

        // 先上传用户照片
        AndroidBridge.uploadImage(AppState.sessionId, 'user', AppState.userPhotoBase64)
            .then(function() {
                // 再上传发型参考图
                if (AppState.selectedHairstyle) {
                    return AndroidBridge.getHairstyleImage(AppState.selectedHairstyle.path)
                        .then(function(result) {
                            return AndroidBridge.uploadImage(AppState.sessionId, 'hairstyle', result.base64);
                        });
                }
                return Promise.resolve();
            })
            .then(function() {
                // 开始处理
                return AndroidBridge.startProcess(AppState.sessionId, 'hairstyle');
            })
            .then(function() {
                AndroidBridge.showToast('处理已开始，请稍候...');
                // 开始轮询状态
                pollProcessStatus();
            })
            .catch(function(error) {
                AndroidBridge.showToast('处理失败: ' + error);
            });
    }

    // 轮询处理状态
    function pollProcessStatus() {
        if (!AppState.sessionId) return;

        var pollInterval = setInterval(function() {
            AndroidBridge.getSessionStatus(AppState.sessionId)
                .then(function(result) {
                    if (result.status === 'completed') {
                        clearInterval(pollInterval);
                        // 跳转到结果页面或显示结果
                        showResults(result.results);
                    } else if (result.status === 'failed') {
                        clearInterval(pollInterval);
                        AndroidBridge.showToast('处理失败，请重试');
                    }
                })
                .catch(function() {
                    // 继续轮询
                });
        }, 3000);
    }

    // 显示结果
    function showResults(results) {
        // TODO: 跳转到结果页面或显示结果弹窗
        AndroidBridge.showToast('处理完成！');
        console.log('Results:', results);
    }

    // 导出全局函数供内联JS调用
    window.AppFunctions = {
        takePhoto: takePhoto,
        pickImage: pickImage,
        createSession: createSession,
        loadHairstyleList: loadHairstyleList,
        startProcess: startProcess
    };

})();

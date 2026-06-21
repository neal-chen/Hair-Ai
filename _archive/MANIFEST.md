# 项目归档清单

归档时间: 2026-06-17 10:38
归档文件: `hairstyle_project_archive_20260617_103809.tar.gz` (191MB)

## 项目文件清单

```
.
_archive
faxin
faxin/_archive
faxin/_archive/fascl.html
faxin/_archive/faslist.html
faxin/_archive/faxcl.html
faxin/_archive/faxlist.html
faxin/_archive/home.html
faxin/_archive/jquery-3.5.1.min.js
faxin/_archive/page1.html
faxin/_archive/page2.html
faxin/_archive/slicy.css
faxin/_archive/swiper.min.css
faxin/_archive/swiper.min.js
faxin/css
faxin/css/style.css
faxin/images
faxin/index.html
faxin/js
faxin/js/carousel.js
faxin/js/colorwheel.js
faxin/js/pages.js
faxin/js/router.js
faxin/js/state.js
hairstyle_android_pad-main
hairstyle_android_pad-main/API_DOCS.md
hairstyle_android_pad-main/app
hairstyle_android_pad-main/app/build.gradle
hairstyle_android_pad-main/app/src
hairstyle_android_pad-main/app/src/main
hairstyle_android_pad-main/app/src/main/AndroidManifest.xml
hairstyle_android_pad-main/app/src/main/assets
hairstyle_android_pad-main/app/src/main/java
hairstyle_android_pad-main/app/src/main/res
hairstyle_android_pad-main/build.gradle
hairstyle_android_pad-main/DATABASE_ARCHITECTURE.md
hairstyle_android_pad-main/DATABASE_TABLES.md
hairstyle_android_pad-main/gradle
hairstyle_android_pad-main/gradle.properties
hairstyle_android_pad-main/gradlew
hairstyle_android_pad-main/gradlew.bat
hairstyle_android_pad-main/settings.gradle
hairstyle_processor_v2.py
```

## 项目概览

### Android 项目 (hairstyle_android_pad-main)
- Kotlin 源码: $(find hairstyle_android_pad-main -name '*.kt' | wc -l) 个文件
- XML 布局: $(find hairstyle_android_pad-main -name '*.xml' | wc -l) 个文件
- Web 前端: $(find hairstyle_android_pad-main/app/src/main/assets/web -name '*.html' -o -name '*.js' -o -name '*.css' | wc -l) 个文件
- 发色图片: 134 张 (assets/hair_colors/*.png)
- 发色数据: hair_colors_data.json (含完整染发流程)
- 硬编码发型: 102 款女发 + 24 款男发 (HairstyleLibraryManager.kt)

### Web 前端 (faxin)
- HTML 页面: index.html 等
- JS 模块: carousel.js, colorwheel.js, pages.js, router.js, state.js
- 归档页面: _archive/ (旧版页面备份)

### Python 脚本
- hairstyle_processor_v2.py (2473 行, 发型图像处理)

### 已归档文档
- hairstyle_android_pad-main/API_DOCS.md
- hairstyle_android_pad-main/DATABASE_ARCHITECTURE.md
- hairstyle_android_pad-main/DATABASE_TABLES.md

## 关键文件索引

| 文件 | 说明 | 行数 |
|---|---|---|
| ./hairstyle_android_pad-main/app/src/main/assets/web/js/android-bridge.js | Android JS Bridge | 380 |
| ./hairstyle_android_pad-main/app/src/main/assets/web/js/script.js | WebView 业务逻辑 | 515 |
| ./hairstyle_android_pad-main/app/src/main/java/com/hairstyle/generator/bridge/AndroidBridge.kt | JS↔Native 桥接层 | 907 |
| ./hairstyle_android_pad-main/app/src/main/java/com/hairstyle/generator/data/api/HairstyleApiService.kt | API 接口定义 | 107 |
| ./hairstyle_android_pad-main/app/src/main/java/com/hairstyle/generator/data/api/NetworkConfig.kt | 网络配置 | 140 |
| ./hairstyle_android_pad-main/app/src/main/java/com/hairstyle/generator/data/repository/HairstyleRepository.kt | 数据仓库 | 387 |
| ./hairstyle_android_pad-main/app/src/main/java/com/hairstyle/generator/data/storage/HairColorLibraryManager.kt | 发色库管理 | 110 |
| ./hairstyle_android_pad-main/app/src/main/java/com/hairstyle/generator/data/storage/HairstyleLibraryManager.kt | 发型库管理(硬编码) | 266 |
| ./hairstyle_android_pad-main/app/src/main/java/com/hairstyle/generator/ui/PhotoUploadActivity.kt | 上传页面 | 715 |
| ./hairstyle_android_pad-main/app/src/main/java/com/hairstyle/generator/ui/ResultsActivity.kt | 结果页面 | 574 |

---
归档于 2026-06-17

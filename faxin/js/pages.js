/**
 * SPA Page Templates — 所有页面 HTML 模板 + 初始化逻辑
 * 无 ES module 依赖，兼容 file:// 直接打开
 */
;(function() {
  var state = HairApp.state
  var $ = HairApp.$
  var $$ = HairApp.$$
  var fadeIn = HairApp.fadeIn
  var fadeOut = HairApp.fadeOut
  var readFile = HairApp.readFileAsDataURL
  var createColorWheel = HairApp.createColorWheel
  var createStatusCarousel = HairApp.createStatusCarousel
  var createImageCarousel = HairApp.createImageCarousel

  // ─── 页面模板 ─────────────────────────────────

  var pages = {

    login: function() {
      return '<div class="login">' +
        '<img class="bg" src="images/login.jpg">' +
        '<a class="hbtn" onclick="router.go(\'#/home\')">' +
          '<img src="images/hbtn1.png">' +
        '</a>' +
      '</div>'
    },

    home: function() {
      var hasPhoto = !!state.photo
      return '' +
      '<div class="section1">' +
        '<div class="htit">' +
          '<div class="icon">' +
            '<a href="javascript:;"><img src="images/hprev.png"></a>' +
            '<a href="javascript:;"><img src="images/hnext.png"></a>' +
          '</div>' +
          '<div class="hrt"><img src="images/hrt.png"></div>' +
        '</div>' +
        '<div class="hmain">' +
          '<h6>上传照片</h6>' +
          '<h4 class="' + (hasPhoto ? 'whide' : 'wshow') + '">请上传一张您的清晰正脸照</h4>' +
          '<h4 class="' + (hasPhoto ? 'wshow' : 'whide') + '">请选择开始换发型或开始换发色</h4>' +
          '<div class="hpic">' +
            '<img id="pics" src="' + (state.photo || 'images/hpic.png') + '">' +
            '<div class="bflex">' +
              '<div class="icon">' +
                '<img src="images/hxj.png">' +
                '<input type="file" class="file" id="photoUpload" accept="image/*" capture="camera" multiple="false">' +
              '</div>' +
              '<div class="icon weixtc"><img src="images/hwx.png"></div>' +
            '</div>' +
          '</div>' +
          '<div class="' + (hasPhoto ? 'wshow' : 'whide') + '">' +
            '<div class="flexbtn">' +
              '<a onclick="router.go(\'#/page1\')">换发型</a>' +
              '<a onclick="router.go(\'#/page2\')">换发色</a>' +
            '</div>' +
          '</div>' +
        '</div>' +
        '<div class="mask mask-wechat">' +
          '<div class="flexcent">' +
            '<div class="wtanc">' +
              '<img src="images/weix.png">' +
              '<h5>扫码上传您的照片</h5>' +
            '</div>' +
            '<div class="close"><img src="images/close.png"></div>' +
          '</div>' +
        '</div>' +
      '</div>'
    },

    hairstyleRef: function() {
      return pageRefTemplate({
        title: '参考发型',
        subtitle: '为您的新造型选择一个灵感',
        image: state.hairstyleRef || 'images/hpic2.png',
        uploadIcon: 'images/hfx.png',
        nextRoute: '#/faxlist',
        headerImg: 'images/hrt2.png'
      })
    },

    colorRef: function() {
      return pageRefTemplate({
        title: '参考发色',
        subtitle: '为您的新发色选择一个灵感',
        image: state.colorRef || 'images/hpic3.png',
        uploadIcon: 'images/hfs.png',
        nextRoute: '#/faslist',
        headerImg: 'images/hrt3.png'
      })
    },

    hairstyleGallery: function() {
      return galleryTemplate({
        title: '发型库',
        searchPlaceholder: '搜索发型',
        tabs: ['全部', '日式', '欧美', '氛围感', '甜酷风', '韩式'],
        items: generateGalleryItems('hpic6.png', 'hpic7.png', 24),
        confirmRoute: '#/faxcl'
      })
    },

    colorGallery: function() {
      return galleryTemplate({
        title: '参考发色',
        searchPlaceholder: '搜索发色',
        tabs: ['全部', '温感色系', '元气橙系', '清冷色系', '流光金系'],
        items: generateGalleryItems('hpic4.png', 'hpic5.png', 24),
        hasColorWheel: true,
        confirmRoute: '#/fascl'
      })
    },

    hairstyleResult: function() {
      return '' +
      '<div class="section1">' +
        '<div class="htit">' +
          '<div class="icon">' +
            '<a href="javascript:;"><img src="images/hprev.png"></a>' +
            '<a href="javascript:;"><img src="images/hnext.png"></a>' +
          '</div>' +
          '<div class="hrt"><a onclick="router.go(\'#/home\')"><img src="images/homebtn.png"></a></div>' +
        '</div>' +
        '<div class="hmain">' +
          '<h6 class="wshow">正在处理</h6>' +
          '<h6 class="whide2">完成</h6>' +
          '<div class="huanse wshow"><ul><li><img src="images/hpic.png"></li><li><img src="images/hpic3.png"></li></ul></div>' +
          '<h2 class="wshow">AI智能换发型中...</h2>' +
          '<div class="swiperlist wshow" data-carousel="status"><ul>' +
            '<li><i>\uD83D\DD0D用户肌色分析完成</i></li>' +
            '<li><i>✂️正在分析发型风格...</i></li>' +
            '<li><i>🎨正在智能合成新发型...</i></li>' +
            '<li></li><li></li><li></li>' +
          '</ul></div>' +
          '<div class="qxbtn wshow">取消生成</div>' +
          '<div class="whide1">' +
            '<h2>换发型完成！</h2>' +
            '<div class="faxpic"><ul>' +
              '<li><a href="javascript:;" data-result="0"><img src="images/hpic6.png"></a></li>' +
              '<li><a href="javascript:;" data-result="1"><img src="images/hpic7.png"></a></li>' +
              '<li><a href="javascript:;" data-result="2"><img src="images/hpic7.png"></a></li>' +
              '<li><a href="javascript:;" data-result="3"><img src="images/hpic6.png"></a></li>' +
            '</ul></div>' +
            '<h6 class="txtcent">点击选择心仪的发型</h6>' +
          '</div>' +
          '<div class="whide2">' +
            '<h2>换发型失败！</h2>' +
            '<div class="faxpic"><ul>' +
              new Array(4).fill(0).map(function() {
                return '<li><a href="javascript:;"><img src="images/hpic6.png"><div class="mk"><img src="images/hicon4.png"></div></a></li>'
              }).join('') +
            '</ul></div>' +
            '<div class="flexbtn"><a href="javascript:;" id="retryBtn">重新生成</a></div>' +
          '</div>' +
        '</div>' +
        // Detail mask
        '<div class="mask mask-detail">' +
          '<div class="wshowpic"><div class="flexcent">' +
            '<div class="mpic">' +
              '<div class="carousel" data-carousel="image">' +
                '<div class="carousel-wrapper">' +
                  '<div class="carousel-slide"><img src="images/hpic3.png"></div>' +
                  '<div class="carousel-slide"><img src="images/hpic3.png"></div>' +
                  '<div class="carousel-slide"><img src="images/hpic3.png"></div>' +
                '</div>' +
                '<div class="carousel-prev"></div><div class="carousel-next"></div>' +
              '</div>' +
              '<span>建议烫燫，实际效果会因自身因素变化，请与发型师沟通确认。</span>' +
            '</div>' +
            '<div class="flexbtn flexbtn3">' +
              '<a href="javascript:;" class="rbt1">注册并保存</a>' +
              '<a href="javascript:;" class="rbt2" data-action="show-color-wheel">换发色</a>' +
              '<a href="javascript:;" class="rbt3" data-action="show-3d">3d展示</a>' +
            '</div>' +
          '</div></div>' +
          '<div class="whidese"><div class="flexcent">' +
            '<div class="mpic"><div class="wbga" data-color-wheel></div></div>' +
            '<div class="flexbtn flexbtn2">' +
              '<a href="javascript:;" class="rbt4">取消</a>' +
              '<a href="javascript:;" class="rbt5">确认</a>' +
            '</div>' +
          '</div></div>' +
          '<div class="wshowcg"><div class="flexcent">' +
            '<div class="mpic"><img src="images/hpic3.png"><i>复古齐肩卷发</i></div>' +
            '<div class="flexbtn">' +
              '<a href="javascript:;" class="rbt6">取消</a>' +
              '<a href="javascript:;">确认</a>' +
            '</div>' +
          '</div></div>' +
          '<div class="wshowsand"><div class="flexcent">' +
            '<div class="mpic"><img src="images/hpic3.png"></div>' +
            '<div class="flexbtn"><a href="javascript:;" class="rbt7">关闭</a></div>' +
          '</div></div>' +
        '</div>' +
      '</div>'
    },

    colorResult: function() {
      return '' +
      '<div class="section1">' +
        '<div class="htit">' +
          '<div class="icon">' +
            '<a href="javascript:;"><img src="images/hprev.png"></a>' +
            '<a href="javascript:;"><img src="images/hnext.png"></a>' +
          '</div>' +
          '<div class="hrt"><a onclick="router.go(\'#/home\')"><img src="images/homebtn.png"></a></div>' +
        '</div>' +
        '<div class="hmain">' +
          '<h6 class="wshow">正在处理</h6>' +
          '<h6 class="whide2">完成</h6>' +
          '<div class="huanse wshow"><ul><li><img src="images/hpic.png"></li><li><img src="images/hpic3.png"></li></ul></div>' +
          '<h2 class="wshow">AI智能换发色中...</h2>' +
          '<div class="swiperlist wshow" data-carousel="status"><ul>' +
            '<li><i>🔍用户肌色分析完成</i></li>' +
            '<li><i>✂️正在分析发型风格...</i></li>' +
            '<li><i>🎨正在智能合成新发型...</i></li>' +
            '<li></li><li></li><li></li>' +
          '</ul></div>' +
          '<div class="qxbtn wshow">取消生成</div>' +
          '<div class="whide1">' +
            '<h2>换发色完成！</h2>' +
            '<div class="activeimg on">' +
              '<img src="images/hpic3.png">' +
              '<i>建议烫燫，实际效果会因自身因素变化，请与发型师沟通确认。</i>' +
            '</div>' +
            '<div class="flexbtn">' +
              '<a href="javascript:;" class="rbt1">注册并保存</a>' +
              '<a href="javascript:;" class="rbt2" data-action="show-3d">3d展示</a>' +
            '</div>' +
          '</div>' +
          '<div class="whide2">' +
            '<h2>换发色失败！</h2>' +
            '<div class="activeimg">' +
              '<img src="images/hpic3.png">' +
              '<div class="mk"><img src="images/hicon4.png"></div>' +
            '</div>' +
            '<div class="flexbtn"><a href="javascript:;" id="retryBtn">重新生成</a></div>' +
          '</div>' +
          '<div class="mask mask-3d">' +
            '<div class="flexcent">' +
              '<div class="mpic"><img src="images/hpic3.png"></div>' +
              '<div class="flexbtn"><a href="javascript:;" class="rbt3">关闭</a></div>' +
            '</div>' +
          '</div>' +
        '</div>' +
      '</div>'
    }
  }

  // ─── 共享模板 ─────────────────────────────────

  function pageRefTemplate(o) {
    return '' +
    '<div class="section1">' +
      '<div class="htit">' +
        '<div class="icon">' +
          '<a href="javascript:;"><img src="images/hprev.png"></a>' +
          '<a href="javascript:;"><img src="images/hnext.png"></a>' +
        '</div>' +
        '<div class="hrt"><img src="' + o.headerImg + '"></div>' +
      '</div>' +
      '<div class="hmain">' +
        '<h6>' + o.title + '</h6>' +
        '<h4>' + o.subtitle + '</h4>' +
        '<div class="hpic">' +
          '<img id="pics" src="' + o.image + '">' +
          '<div class="bflex">' +
            '<div class="icon">' +
              '<img src="' + o.uploadIcon + '">' +
              '<input type="file" class="file" id="photoUpload" accept="image/*" capture="camera" multiple="false">' +
            '</div>' +
            '<div class="icon weixtc"><img src="images/hwx.png"></div>' +
          '</div>' +
        '</div>' +
        '<div class="flexbtn"><a class="btn" onclick="router.go(\'' + o.nextRoute + '\')">下一步</a></div>' +
      '</div>' +
      '<div class="mask mask-wechat">' +
        '<div class="flexcent">' +
          '<div class="wtanc"><img src="images/weix.png"><h5>扫码上传您的照片</h5></div>' +
          '<div class="close"><img src="images/close.png"></div>' +
        '</div>' +
      '</div>' +
    '</div>'
  }

  function galleryTemplate(o) {
    var tabsHtml = o.tabs.map(function(t, i) {
      return '<li class="' + (i === 0 ? 'on' : '') + '"><a href="javascript:;">' + t + '<i></i></a></li>'
    }).join('')

    var maskHtml

    if (o.hasColorWheel) {
      maskHtml = '' +
      '<div class="wshow"><div class="flexcent">' +
        '<div class="mpic"><div class="wbga" data-color-wheel></div></div>' +
        '<div class="flexbtn flexbtn2">' +
          '<a href="javascript:;" class="cancel-btn">取消</a>' +
          '<a href="javascript:;" class="confirm-color-btn">确认</a>' +
        '</div>' +
      '</div></div>' +
      '<div class="whide"><div class="flexcent">' +
        '<div class="mpic"><img src="images/hpic3.png"><i>复古齐肩卷发</i></div>' +
        '<div class="flexbtn">' +
          '<a href="javascript:;" class="back-color-btn">取消</a>' +
          '<a href="javascript:;" class="go-result-btn" data-route="' + o.confirmRoute + '">确认</a>' +
        '</div>' +
      '</div></div>'
    } else {
      maskHtml = '' +
      '<div class="wshow"><div class="flexcent">' +
        '<div class="mpic"><img src="images/hpic3.png"><i>复古齐肩卷发</i></div>' +
        '<div class="flexbtn">' +
          '<a href="javascript:;" class="cancel-btn">取消</a>' +
          '<a href="javascript:;" class="go-result-btn" data-route="' + o.confirmRoute + '">确认</a>' +
        '</div>' +
      '</div></div>'
    }

    return '' +
    '<div class="section1">' +
      '<div class="htit">' +
        '<div class="icon">' +
          '<a href="javascript:;"><img src="images/hprev.png"></a>' +
          '<a href="javascript:;"><img src="images/hnext.png"></a>' +
        '</div>' +
        '<div class="hrt"><form><div class="search">' +
          '<img src="images/hicon1.png">' +
          '<input type="text" class="txt" placeholder="' + o.searchPlaceholder + '">' +
        '</div></form></div>' +
      '</div>' +
      '<div class="hmain">' +
        '<div class="flextxt">' +
          '<h6>' + o.title + '</h6>' +
          '<div class="rtc">' +
            '<a href="javascript:;"><img src="images/hicon2.png"></a>' +
            '<a href="javascript:;" class="sex" id="genderToggle">' +
              '<img src="images/sex.png"><img src="images/sex2.png">' +
            '</a>' +
          '</div>' +
        '</div>' +
        '<div class="htab"><ul>' + tabsHtml + '</ul></div>' +
        '<div class="piclist"><ul>' + o.items + '</ul></div>' +
      '</div>' +
      '<div class="mask mask-select">' + maskHtml + '</div>' +
    '</div>'
  }

  function generateGalleryItems(img1, img2, count) {
    var html = ''
    for (var i = 0; i < count; i++) {
      var src = i % 2 === 0 ? img1 : img2
      html += '<li><a href="javascript:;"><div class="icon"><img src="images/' + src + '"></div>' +
        '<div class="flex"><h6>复古齐肩卷发</h6><i>日式</i></div></a></li>'
    }
    return html
  }

  // 挂载到全局
  HairApp.pages = pages

  // ─── 页面初始化 ───────────────────────────────

  document.addEventListener('route-change', function(e) {
    var path = e.detail.path
    switch (path) {
      case '#/home':      initHome(); break
      case '#/page1':     initRefPage(); break
      case '#/page2':     initRefPage(); break
      case '#/faxlist':   initGallery(false); break
      case '#/faslist':   initGallery(true); break
      case '#/faxcl':     initHairstyleResult(); break
      case '#/fascl':     initColorResult(); break
    }
  })

  // ─── 首页 ─────────────────────────────────────

  function initHome() {
    var upload = document.getElementById('photoUpload')
    if (upload) {
      upload.addEventListener('change', function() {
        if (this.files && this.files.length > 0) {
          readFile(this.files[0]).then(function(dataUrl) {
            state.photo = dataUrl
            window.router.resolve()
          })
        }
      })
    }

    bindWechat('.weixtc', '.mask-wechat')
  }

  // ─── 参考页 ────────────────────────────────────

  function initRefPage() {
    var upload = document.getElementById('photoUpload')
    if (upload) {
      upload.addEventListener('change', function() {
        if (this.files && this.files.length > 0) {
          readFile(this.files[0]).then(function(dataUrl) {
            var pics = document.getElementById('pics')
            if (pics) pics.src = dataUrl
            if (location.hash === '#/page1') {
              state.hairstyleRef = dataUrl
            } else {
              state.colorRef = dataUrl
            }
          })
        }
      })
    }

    bindWechat('.weixtc', '.mask-wechat')
  }

  // ─── 图库页 ────────────────────────────────────

  function initGallery(hasColorWheel) {
    // 性别切换
    var genderBtn = document.getElementById('genderToggle')
    if (genderBtn) {
      genderBtn.addEventListener('click', function() {
        genderBtn.classList.toggle('on')
        state.gender = genderBtn.classList.contains('on')
      })
    }

    // 标签页切换
    var tabs = document.querySelectorAll('.htab li')
    tabs.forEach(function(tab) {
      tab.addEventListener('click', function() {
        tabs.forEach(function(t) { t.classList.remove('on') })
        tab.classList.add('on')
      })
    })

    // 点击项目 -> 弹出选择框
    var items = document.querySelectorAll('.piclist li')
    var mask = document.querySelector('.mask-select')
    items.forEach(function(item) {
      item.addEventListener('click', function() {
        if (mask) fadeIn(mask)
      })
    })

    if (!mask) return

    // 取消按钮
    mask.querySelectorAll('.cancel-btn, .rbt1').forEach(function(btn) {
      btn.addEventListener('click', function() { fadeOut(mask) })
    })

    // 确认 -> 跳转
    mask.querySelectorAll('.go-result-btn').forEach(function(btn) {
      btn.addEventListener('click', function() {
        fadeOut(mask)
        var route = btn.getAttribute('data-route')
        if (route) window.router.go(route)
      })
    })

    if (hasColorWheel) {
      var wheelContainer = mask.querySelector('[data-color-wheel]')
      var wheel
      if (wheelContainer) {
        wheel = createColorWheel(wheelContainer, {
          initialColor: state.selectedColor,
          onChange: function(color) { state.selectedColor = color }
        })
      }

      var confirmColor = mask.querySelector('.confirm-color-btn')
      var backColor = mask.querySelector('.back-color-btn')
      var step1 = mask.querySelector('.wshow')
      var step2 = mask.querySelector('.whide')

      if (confirmColor && step1 && step2) {
        confirmColor.addEventListener('click', function() {
          step1.style.display = 'none'
          step2.style.display = ''
        })
      }
      if (backColor && step1 && step2) {
        backColor.addEventListener('click', function() {
          step2.style.display = 'none'
          step1.style.display = ''
        })
      }
    }
  }

  // ─── 发型结果 ──────────────────────────────────

  function initHairstyleResult() {
    var timer = setTimeout(function() {
      document.querySelectorAll('.wshow').forEach(function(el) { el.style.display = 'none' })
      var whide1 = document.querySelector('.whide1')
      if (whide1) whide1.style.display = ''
    }, 4000)

    var statusEl = document.querySelector('[data-carousel="status"]')
    if (statusEl) createStatusCarousel(statusEl)

    var qxbtn = document.querySelector('.qxbtn')
    if (qxbtn) {
      qxbtn.addEventListener('click', function() {
        clearTimeout(timer)
        document.querySelectorAll('.wshow').forEach(function(el) { el.style.display = 'none' })
        var whide2 = document.querySelector('.whide2')
        if (whide2) whide2.style.display = ''
      })
    }

    var retry = document.getElementById('retryBtn')
    if (retry) {
      retry.addEventListener('click', function() { window.router.resolve() })
    }

    // 结果缩略图 -> 详情弹窗
    var resultLinks = document.querySelectorAll('.faxpic ul li a')
    var detailMask = document.querySelector('.mask-detail')
    resultLinks.forEach(function(link) {
      link.addEventListener('click', function() {
        if (detailMask) fadeIn(detailMask)
      })
    })

    if (detailMask) {
      // 关闭详情
      detailMask.querySelectorAll('.rbt1').forEach(function(btn) {
        btn.addEventListener('click', function() { fadeOut(detailMask) })
      })

      // 换发色
      var showWheel = detailMask.querySelector('[data-action="show-color-wheel"]')
      var whidese = detailMask.querySelector('.whidese')
      var wshowpic = detailMask.querySelector('.wshowpic')
      if (showWheel && whidese && wshowpic) {
        showWheel.addEventListener('click', function() {
          wshowpic.style.display = 'none'
          whidese.style.display = ''
          var wbga = whidese.querySelector('[data-color-wheel]')
          if (wbga && !wbga.getAttribute('data-init')) {
            wbga.setAttribute('data-init', '1')
            var wheel = createColorWheel(wbga, {
              initialColor: state.selectedColor,
              onChange: function(c) { state.selectedColor = c }
            })
            var confirmBtn = whidese.querySelector('.rbt5')
            var showcg = detailMask.querySelector('.wshowcg')
            if (confirmBtn && showcg) {
              confirmBtn.addEventListener('click', function() {
                whidese.style.display = 'none'
                showcg.style.display = ''
              })
            }
            var backBtn = whidese.querySelector('.rbt4')
            if (backBtn) {
              backBtn.addEventListener('click', function() {
                whidese.style.display = 'none'
                wshowpic.style.display = ''
              })
            }
            var backCg = detailMask.querySelector('.rbt6')
            if (backCg) {
              backCg.addEventListener('click', function() {
                var cg = detailMask.querySelector('.wshowcg')
                if (cg) cg.style.display = 'none'
                whidese.style.display = ''
              })
            }
          }
        })
      }

      // 3D展示
      var show3d = detailMask.querySelector('[data-action="show-3d"]')
      var wshowsand = detailMask.querySelector('.wshowsand')
      if (show3d && wshowsand && wshowpic) {
        show3d.addEventListener('click', function() {
          wshowpic.style.display = 'none'
          wshowsand.style.display = ''
        })
      }
      var closeSand = detailMask.querySelector('.rbt7')
      if (closeSand && wshowsand && wshowpic) {
        closeSand.addEventListener('click', function() {
          wshowsand.style.display = 'none'
          wshowpic.style.display = ''
        })
      }

      // 图片轮播
      var imgCarousel = detailMask.querySelector('[data-carousel="image"]')
      if (imgCarousel) createImageCarousel(imgCarousel, { delay: 3000 })
    }
  }

  // ─── 发色结果 ──────────────────────────────────

  function initColorResult() {
    var timer = setTimeout(function() {
      document.querySelectorAll('.wshow').forEach(function(el) { el.style.display = 'none' })
      var whide1 = document.querySelector('.whide1')
      if (whide1) whide1.style.display = ''
    }, 4000)

    var statusEl = document.querySelector('[data-carousel="status"]')
    if (statusEl) createStatusCarousel(statusEl)

    var qxbtn = document.querySelector('.qxbtn')
    if (qxbtn) {
      qxbtn.addEventListener('click', function() {
        clearTimeout(timer)
        document.querySelectorAll('.wshow').forEach(function(el) { el.style.display = 'none' })
        var whide2 = document.querySelector('.whide2')
        if (whide2) whide2.style.display = ''
      })
    }

    var retry = document.getElementById('retryBtn')
    if (retry) {
      retry.addEventListener('click', function() { window.router.resolve() })
    }

    // 3D展示
    var show3d = document.querySelectorAll('[data-action="show-3d"]')
    var mask3d = document.querySelector('.mask-3d')
    show3d.forEach(function(btn) {
      btn.addEventListener('click', function() { if (mask3d) fadeIn(mask3d) })
    })
    var close3d = document.querySelector('.mask-3d .rbt3')
    if (close3d) {
      close3d.addEventListener('click', function() { if (mask3d) fadeOut(mask3d) })
    }

    // 保存（mock）
    document.querySelectorAll('.rbt1').forEach(function(btn) {
      btn.addEventListener('click', function() { alert('注册并保存功能即将上线') })
    })
  }

  // ─── 微信弹窗绑定 ──────────────────────────────

  function bindWechat(triggerSel, maskSel) {
    var trigger = document.querySelector(triggerSel)
    var mask = document.querySelector(maskSel)
    if (trigger && mask) {
      trigger.addEventListener('click', function() { fadeIn(mask) })
      var close = mask.querySelector('.close')
      if (close) {
        close.addEventListener('click', function() { fadeOut(mask) })
      }
    }
  }

})()

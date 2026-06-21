/**
 * Global application state + DOM helpers
 */
;(function() {
  // 初始化全局命名空间（必须在第一行）
  window.HairApp = {}

  const appState = {
    /** @type {string|null} 用户上传的照片 base64 */
    photo: null,
    /** @type {string|null} 发型参考图 */
    hairstyleRef: null,
    /** @type {string|null} 发色参考图 */
    colorRef: null,
    /** @type {string|null} 选中发型 */
    selectedHairstyle: null,
    /** @type {string} 选中颜色 hex */
    selectedColor: '#FD9B6C',
    /** @type {boolean} 性别 false=女 true=男 */
    gender: false,
    /** @type {Array} 结果图片 */
    results: [],
  }

  // DOM 工具函数
  const $ = (sel, ctx) => (ctx || document).querySelector(sel)
  const $$ = (sel, ctx) => Array.from((ctx || document).querySelectorAll(sel))

  function fadeIn(el) {
    if (typeof el === 'string') el = $(el)
    if (!el) return
    el.style.display = ''
    el.style.opacity = '0'
    requestAnimationFrame(() => { el.style.opacity = '1' })
  }

  function fadeOut(el) {
    if (typeof el === 'string') el = $(el)
    if (!el) return
    el.style.opacity = '0'
    setTimeout(() => { el.style.display = 'none' }, 200)
  }

  function readFileAsDataURL(file) {
    return new Promise(function(resolve, reject) {
      var reader = new FileReader()
      reader.onload = function(e) { resolve(e.target.result) }
      reader.onerror = reject
      reader.readAsDataURL(file)
    })
  }

  HairApp.state = appState
  HairApp.$ = $
  HairApp.$$ = $$
  HairApp.fadeIn = fadeIn
  HairApp.fadeOut = fadeOut
  HairApp.readFileAsDataURL = readFileAsDataURL
})()

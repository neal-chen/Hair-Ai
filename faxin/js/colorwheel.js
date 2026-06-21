/**
 * Color Wheel Picker — Canvas HSV 色环，支持鼠标+触控
 */
;(function() {
  var CANVAS_SIZE = 280
  var CENTER = CANVAS_SIZE / 2

  var PRESET_COLORS = [
    '#FD9B6C', '#C7037E', '#FF1312', '#FF6C0A', '#FF9502',
    '#FEC90F', '#FFFF10', '#27B51A', '#94CB14'
  ]

  function createColorWheel(container, opts) {
    opts = opts || {}
    var currentColor = opts.initialColor || '#FD9B6C'

    // 构建 DOM
    container.classList.add('color-wheel-container')
    container.innerHTML =
      '<div class="cw-canvas-wrap">' +
        '<canvas width="' + CANVAS_SIZE + '" height="' + CANVAS_SIZE + '"></canvas>' +
      '</div>' +
      '<div class="cw-preview-wrap">' +
        '<div class="cw-preview" style="background:' + currentColor + '"></div>' +
      '</div>' +
      '<div class="cw-presets">' +
        PRESET_COLORS.map(function(c) {
          return '<div class="cw-swatch' + (c === currentColor ? ' active' : '') +
                 '" data-color="' + c + '" style="background:' + c + '"></div>'
        }).join('') +
      '</div>'

    var canvas = container.querySelector('canvas')
    var ctx = canvas.getContext('2d')
    var preview = container.querySelector('.cw-preview')
    var swatches = container.querySelectorAll('.cw-swatch')

    // 绘制色环
    function drawWheel() {
      var imageData = ctx.createImageData(CANVAS_SIZE, CANVAS_SIZE)
      var data = imageData.data

      for (var y = 0; y < CANVAS_SIZE; y++) {
        for (var x = 0; x < CANVAS_SIZE; x++) {
          var dx = x - CENTER
          var dy = y - CENTER
          var r = Math.sqrt(dx * dx + dy * dy)
          if (r > CENTER) continue

          var hue = (Math.atan2(dy, dx) * 180 / Math.PI + 360) % 360
          var lightness = (r / CENTER) * 255
          var h = hue / 60
          var i = Math.floor(h)
          var f = h - i
          var q = lightness * (1 - f)
          var t = lightness * f
          var rVal, gVal, bVal

          switch (i % 6) {
            case 0: rVal = lightness; gVal = t; bVal = 0; break
            case 1: rVal = q; gVal = lightness; bVal = 0; break
            case 2: rVal = 0; gVal = lightness; bVal = t; break
            case 3: rVal = 0; gVal = q; bVal = lightness; break
            case 4: rVal = t; gVal = 0; bVal = lightness; break
            case 5: rVal = lightness; gVal = 0; bVal = q; break
          }

          var idx = (y * CANVAS_SIZE + x) * 4
          data[idx]     = Math.round(rVal)
          data[idx + 1] = Math.round(gVal)
          data[idx + 2] = Math.round(bVal)
          data[idx + 3] = 255
        }
      }
      ctx.putImageData(imageData, 0, 0)
    }
    drawWheel()

    // 选取颜色
    function pickColor(clientX, clientY) {
      var rect = canvas.getBoundingClientRect()
      var x = Math.round((clientX - rect.left) * (CANVAS_SIZE / rect.width))
      var y = Math.round((clientY - rect.top) * (CANVAS_SIZE / rect.height))
      if (x < 0 || x >= CANVAS_SIZE || y < 0 || y >= CANVAS_SIZE) return
      var pixel = ctx.getImageData(x, y, 1, 1).data
      var hex = '#' +
        pixel[0].toString(16).padStart(2, '0') +
        pixel[1].toString(16).padStart(2, '0') +
        pixel[2].toString(16).padStart(2, '0')
      setColor(hex, true)
    }

    // 拖拽选取
    function onPointerDown(e) {
      var coords = pointerCoords(e)
      pickColor(coords[0], coords[1])

      function onMove(ev) {
        var c = pointerCoords(ev)
        pickColor(c[0], c[1])
        if (ev.cancelable) ev.preventDefault()
      }
      function onUp() {
        document.removeEventListener('mousemove', onMove)
        document.removeEventListener('mouseup', onUp)
        document.removeEventListener('touchmove', onMove)
        document.removeEventListener('touchend', onUp)
      }
      document.addEventListener('mousemove', onMove, { passive: false })
      document.addEventListener('mouseup', onUp)
      document.addEventListener('touchmove', onMove, { passive: false })
      document.addEventListener('touchend', onUp)
    }

    canvas.addEventListener('mousedown', onPointerDown)
    canvas.addEventListener('touchstart', onPointerDown, { passive: false })

    // 预设色块点击
    swatches.forEach(function(el) {
      el.addEventListener('click', function() { setColor(el.dataset.color, true) })
      el.addEventListener('touchend', function(e) {
        setColor(el.dataset.color, true)
        e.preventDefault()
      })
    })

    // 设置颜色
    function setColor(hex, triggerChange) {
      hex = hex.toLowerCase()
      if (currentColor === hex) return
      currentColor = hex
      preview.style.background = hex
      swatches.forEach(function(el) {
        el.classList.toggle('active', el.dataset.color === hex)
      })
      if (triggerChange && opts.onChange) opts.onChange(hex)
    }

    function getColor() { return currentColor }

    if (currentColor) setColor(currentColor, false)

    return { setColor: setColor, getColor: getColor }
  }

  function pointerCoords(e) {
    if (e.touches && e.touches.length > 0) {
      return [e.touches[0].clientX, e.touches[0].clientY]
    }
    if (e.changedTouches && e.changedTouches.length > 0) {
      return [e.changedTouches[0].clientX, e.changedTouches[0].clientY]
    }
    return [e.clientX, e.clientY]
  }

  HairApp.createColorWheel = createColorWheel
})()

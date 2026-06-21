/**
 * 轻量轮播 — 替换 Swiper.js
 * - 垂直状态列表轮播（状态处理进度）
 * - 水平图片轮播（结果预览）
 */
;(function() {

  // 垂直状态轮播（处理中状态列表自动滚动）
  function createStatusCarousel(container) {
    var list = container && container.querySelector('ul')
    if (!list) return
    var items = list.children
    if (items.length < 2) return

    var index = 0
    var itemHeight = items[0] ? items[0].offsetHeight : 40
    var interval

    // 确保所有项高度一致
    if (items[0]) {
      itemHeight = items[0].offsetHeight
    }

    function scrollTo(i) {
      index = (i + items.length) % items.length
      list.style.transform = 'translateY(-' + (index * itemHeight) + 'px)'
      list.style.transition = 'transform 0.4s ease'
    }

    function start() {
      stop()
      interval = setInterval(function() {
        if (index < items.length - 1) {
          scrollTo(index + 1)
        } else {
          clearInterval(interval)
        }
      }, 1200)
    }

    function stop() {
      if (interval) { clearInterval(interval); interval = null }
    }

    list.style.transition = 'transform 0.4s ease'
    start()

    return { start: start, stop: stop, destroy: stop }
  }

  // 水平图片轮播（结果大图左右翻页）
  function createImageCarousel(container, opts) {
    opts = opts || {}
    var wrapper = container && container.querySelector('.carousel-wrapper')
    var slides = container ? container.querySelectorAll('.carousel-slide') : []
    var prevBtn = container && container.querySelector('.carousel-prev')
    var nextBtn = container && container.querySelector('.carousel-next')

    if (!wrapper || slides.length < 2) return

    var index = 0
    var interval

    function goTo(i) {
      index = ((i % slides.length) + slides.length) % slides.length
      wrapper.style.transform = 'translateX(-' + (index * 100) + '%)'
      wrapper.style.transition = 'transform 0.3s ease'
    }

    function nextSlide() { goTo(index + 1) }
    function prevSlide() { goTo(index - 1) }

    if (prevBtn) prevBtn.addEventListener('click', function() { stop(); prevSlide() })
    if (nextBtn) nextBtn.addEventListener('click', function() { stop(); nextSlide() })

    function start() {
      stop()
      if (opts.delay !== false) {
        interval = setInterval(nextSlide, opts.delay || 3000)
      }
    }

    function stop() {
      if (interval) { clearInterval(interval); interval = null }
    }

    start()

    return { goTo: goTo, next: nextSlide, prev: prevSlide, start: start, stop: stop, destroy: stop }
  }

  HairApp.createStatusCarousel = createStatusCarousel
  HairApp.createImageCarousel = createImageCarousel
})()

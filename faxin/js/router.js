/**
 * Hash Router — 兼容 file:// 协议，零依赖
 */
;(function() {
  const router = {
    routes: [],
    current: null,

    register(routes) {
      this.routes = routes
    },

    init() {
      window.addEventListener('hashchange', () => this.resolve())
      if (!location.hash) location.hash = '#/'
      this.resolve()
      window.router = this
    },

    go(path) {
      location.hash = path
    },

    back() {
      history.back()
    },

    resolve() {
      const hash = location.hash || '#/'
      const route = this.routes.find(r => r.path === hash)
      if (route) {
        this.current = hash
        const main = document.getElementById('main')
        if (!main) return
        main.innerHTML = route.render()
        main.scrollTop = 0
        document.dispatchEvent(
          new CustomEvent('route-change', { detail: { path: hash } })
        )
      }
    }
  }

  HairApp.router = router
  window.router = router
})()

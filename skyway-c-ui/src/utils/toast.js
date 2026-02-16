let container = null

function getContainer() {
  if (!container) {
    container = document.createElement('div')
    container.className = 'c-toast-container'
    document.body.appendChild(container)
  }
  return container
}

function showToast(type, message) {
  const el = document.createElement('div')
  el.className = `c-toast c-toast-${type}`
  el.textContent = message
  const c = getContainer()
  c.appendChild(el)
  requestAnimationFrame(() => el.classList.add('show'))
  const t = setTimeout(() => {
    el.classList.remove('show')
    setTimeout(() => el.remove(), 200)
  }, 2500)
  return () => clearTimeout(t)
}

export default {
  success: (msg) => showToast('success', msg),
  error: (msg) => showToast('error', msg),
  warning: (msg) => showToast('warning', msg)
}

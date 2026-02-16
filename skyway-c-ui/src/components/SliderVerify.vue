<template>
  <div class="slider-wrap" ref="wrapRef">
    <div
      class="slider-track"
      :class="{ verified }"
      @click="!verified && reset()"
    >
      <span class="slider-label">{{ verified ? '验证成功' : '向右滑动完成验证' }}</span>
      <div
        class="slider-thumb"
        :class="{ verified }"
        :style="{ transform: `translateX(${thumbPx}px)` }"
        @mousedown.prevent="onPointerDown($event, 'mouse')"
        @touchstart.prevent="onPointerDown($event, 'touch')"
      >
        <svg v-if="!verified" class="thumb-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M5 12h14M12 5l7 7-7 7"/>
        </svg>
        <svg v-else class="thumb-icon thumb-check" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="20 6 9 17 4 12"/>
        </svg>
      </div>
    </div>
    <button v-if="verified" type="button" class="slider-refresh" aria-label="重新验证" @click.stop="reset">
      <svg class="refresh-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M23 4v6h-6M1 20v-6h6"/>
        <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
      </svg>
    </button>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'

const emit = defineEmits(['success'])
const verified = ref(false)
const thumbPx = ref(0)
const wrapRef = ref(null)
let trackWidth = 0
const thumbWidth = 44
const threshold = 0.92

function reset() {
  verified.value = false
  thumbPx.value = 0
}

function getTrackWidth() {
  if (!wrapRef.value) return 0
  const track = wrapRef.value.querySelector('.slider-track')
  return track ? track.offsetWidth : 0
}

function getClientX(e, type) {
  return type === 'touch' ? e.touches[0].clientX : e.clientX
}

function onPointerDown(e, type) {
  if (verified.value) return
  trackWidth = getTrackWidth()
  const trackEl = wrapRef.value.querySelector('.slider-track')
  const rect = trackEl.getBoundingClientRect()

  function updateThumb(clientX) {
    let px = clientX - rect.left - thumbWidth / 2
    px = Math.max(0, Math.min(trackWidth - thumbWidth, px))
    thumbPx.value = px
  }

  function onMouseMove(ev) {
    ev.preventDefault()
    updateThumb(ev.clientX)
    if (thumbPx.value >= (trackWidth - thumbWidth) * threshold) {
      verified.value = true
      thumbPx.value = trackWidth - thumbWidth
      emit('success')
      cleanup()
    }
  }
  function onTouchMove(ev) {
    ev.preventDefault()
    updateThumb(ev.touches[0].clientX)
    if (thumbPx.value >= (trackWidth - thumbWidth) * threshold) {
      verified.value = true
      thumbPx.value = trackWidth - thumbWidth
      emit('success')
      cleanup()
    }
  }
  function onMouseEnd() {
    if (!verified.value) thumbPx.value = 0
    cleanup()
  }
  function onTouchEnd() {
    if (!verified.value) thumbPx.value = 0
    cleanup()
  }
  function cleanup() {
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseEnd)
    document.removeEventListener('touchmove', onTouchMove)
    document.removeEventListener('touchend', onTouchEnd)
  }

  if (type === 'touch') {
    document.addEventListener('touchmove', onTouchMove, { passive: false })
    document.addEventListener('touchend', onTouchEnd)
  } else {
    document.addEventListener('mousemove', onMouseMove)
    document.addEventListener('mouseup', onMouseEnd)
  }
  updateThumb(getClientX(e, type))
}

onUnmounted(() => {
  document.removeEventListener('mousemove', () => {})
  document.removeEventListener('mouseup', () => {})
  document.removeEventListener('touchmove', () => {})
  document.removeEventListener('touchend', () => {})
})

defineExpose({ reset, verified })
</script>

<style scoped>
.slider-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}
.slider-track {
  flex: 1;
  height: 44px;
  min-height: 44px;
  background: rgba(0, 0, 0, 0.22);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 2px;
  position: relative;
  cursor: pointer;
  user-select: none;
  overflow: hidden;
  transition: border-color 0.2s ease, background 0.25s ease;
}
.slider-track:hover:not(.verified) {
  border-color: rgba(255, 255, 255, 0.16);
  background: rgba(0, 0, 0, 0.28);
}
.slider-track.verified {
  background: rgba(34, 197, 94, 0.18);
  border-color: rgba(74, 222, 128, 0.35);
}
.slider-label {
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 0.02em;
  pointer-events: none;
  transition: color 0.2s ease;
}
.slider-track.verified .slider-label {
  color: rgba(74, 222, 128, 0.95);
}
.slider-thumb {
  position: absolute;
  top: 2px;
  bottom: 2px;
  left: 2px;
  width: 40px;
  border-radius: 2px;
  background: rgba(59, 130, 246, 0.95);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  transition: transform 0.08s ease-out, background 0.2s ease, box-shadow 0.2s ease;
  cursor: grab;
}
.slider-thumb:active {
  cursor: grabbing;
}
.slider-thumb.verified {
  background: rgba(34, 197, 94, 0.95);
  box-shadow: 0 1px 4px rgba(34, 197, 94, 0.35);
}
.thumb-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}
.thumb-check {
  width: 16px;
  height: 16px;
}
.slider-refresh {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  padding: 0;
  border: none;
  border-radius: 2px;
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s, background 0.2s, border-color 0.2s;
}
.slider-refresh:hover {
  color: rgba(255, 255, 255, 0.9);
  background: rgba(0, 0, 0, 0.32);
  border-color: rgba(255, 255, 255, 0.18);
}
.refresh-icon {
  width: 18px;
  height: 18px;
}
</style>

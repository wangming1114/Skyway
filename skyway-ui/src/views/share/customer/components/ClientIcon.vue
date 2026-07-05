<template>
  <span :class="['client-icon', type, size]">
    <img v-if="type === 'v2ray'" :src="iconV2rayn" alt="" />
    <img v-else-if="type === 'clash'" :src="iconClashVerge" alt="" />
    <img v-else-if="type === 'v2rayng'" :src="iconV2rayng" alt="" />
    <img v-else-if="type === 'shadowrocket'" :src="iconShadowrocket" alt="" />
    <span v-else-if="type === 'android'" class="client-icon-mark mark-android"></span>
    <span v-else-if="type === 'apple'" class="client-icon-mark mark-phone"></span>
    <span v-else-if="type === 'windows'" class="client-icon-mark mark-windows"></span>
    <span v-else class="client-icon-mark mark-generic"></span>
  </span>
</template>

<script setup>
import iconV2rayn from '@/assets/share/customer/icon-v2rayn.png'
import iconClashVerge from '@/assets/share/customer/icon-clash-verge.png'
import iconV2rayng from '@/assets/share/customer/icon-v2rayng.png'
import iconShadowrocket from '@/assets/share/customer/icon-shadowrocket.png'

defineProps({
  type: { type: String, default: 'v2ray' },
  size: { type: String, default: 'md' }
})
</script>

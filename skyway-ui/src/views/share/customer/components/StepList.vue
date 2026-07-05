<template>
  <ol :class="['step-list', { purple }]">
    <li v-for="(step, index) in steps" :key="step">
      <span>{{ index + 1 }}</span>
      <strong>{{ step }}</strong>
    </li>
  </ol>
</template>

<script setup>
defineProps({
  steps: { type: Array, default: () => [] },
  purple: { type: Boolean, default: false }
})
</script>

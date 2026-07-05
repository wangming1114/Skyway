<template>
  <header :class="['share-hero', variant]">
    <div class="share-hero-copy">
      <div v-if="crumb" class="share-crumb">{{ crumb }}</div>
      <h1>{{ title }}</h1>
      <p>{{ desc }}</p>
      <slot name="below" />
    </div>
    <img v-if="image" class="share-hero-art" :src="image" alt="" />
    <div v-if="$slots.actions" class="share-hero-actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  desc: { type: String, required: true },
  crumb: { type: String, default: '' },
  image: { type: String, default: '' },
  variant: { type: String, default: '' }
})
</script>

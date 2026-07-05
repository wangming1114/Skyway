<template>
  <article class="download-client-card">
    <ClientIcon :type="type" size="xl" />
    <div>
      <h3>{{ name }} <i>{{ platform }}</i></h3>
      <p>{{ desc }}</p>
      <div class="download-tags">
        <span v-for="tag in tagList" :key="tag">{{ tag }}</span>
      </div>
      <div class="download-actions">
        <small>版本：{{ version }}</small>
        <button type="button" class="share-primary" @click="$emit('download')"><span class="ui-icon download"></span>立即下载</button>
        <button type="button" class="share-outline" @click="$emit('tutorial')"><span class="ui-icon tutorial"></span>查看教程</button>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import ClientIcon from './ClientIcon.vue'

const props = defineProps({
  type: { type: String, required: true },
  name: { type: String, required: true },
  platform: { type: String, required: true },
  version: { type: String, required: true },
  desc: { type: String, required: true },
  tags: { type: String, default: '' }
})

defineEmits(['tutorial', 'download'])

const tagList = computed(() => props.tags.split(',').filter(Boolean))
</script>

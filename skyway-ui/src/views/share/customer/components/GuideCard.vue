<template>
  <article :class="['guide-big-card', card.accent]">
    <img :src="card.image" alt="" />
    <div>
      <h3>{{ card.title }}</h3>
      <p><span class="guide-field-icon device"></span><b>适用设备：</b><em>{{ card.device }}</em></p>
      <p><span class="guide-field-icon client"></span><b>客户端名称：</b><em>{{ card.client }}</em></p>
      <p>
        <span class="guide-field-icon link"></span>
        <b>图文教程链接：</b>
        <a href="#" @click.prevent="$emit('open')">{{ card.url }}</a>
        <button type="button" aria-label="复制教程链接" @click="$emit('copy')"><span class="ui-icon copy"></span></button>
      </p>
      <div class="guide-card-tip"><span class="title-icon tip"></span>适合{{ audience }}用户，按图文步骤导入节点即可使用。</div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  card: { type: Object, required: true }
})

defineEmits(['copy', 'open'])

const audience = computed(() => {
  if (props.card.title.includes('安卓')) return '安卓'
  if (props.card.title.includes('Windows')) return '电脑'
  return '苹果'
})
</script>

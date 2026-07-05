<template>
  <section class="share-screen guide-screen">
    <HeroHeader
      crumb="帮助中心 / 新手教程导航"
      title="新手客户端图文教程导航"
      desc="按设备选择对应客户端，查看图文教程，适合新手快速上手。"
      :image="heroGuide"
      variant="guide-hero"
    >
      <template #actions>
        <button type="button" class="share-outline hero-btn" @click="$emit('navigate', 'overview')"><span class="ui-icon back-arrow"></span>返回我的订阅</button>
      </template>
    </HeroHeader>

    <section class="share-panel guide-section">
      <h2><span class="title-icon mobile"></span>手机端</h2>
      <div class="guide-card-grid">
        <GuideCard v-for="card in mobileCards" :key="card.title" :card="card" @copy="$emit('copy-text', card.url)" @open="$emit('open-url', card.url)" />
      </div>
    </section>

    <section class="share-panel guide-section">
      <h2><span class="title-icon desktop"></span>电脑端</h2>
      <div class="guide-card-grid">
        <GuideCard v-for="card in desktopCards" :key="card.title" :card="card" @copy="$emit('copy-text', card.url)" @open="$emit('open-url', card.url)" />
      </div>
    </section>

    <section class="guide-warm-strip">
      <strong><span class="title-icon tip"></span>温馨提示</strong>
      <span><i class="ui-icon check"></i>请先确认自己的设备类型，再选择对应教程；</span>
      <span><i class="ui-icon check"></i>不同客户端导入方式不同，请不要混用教程；</span>
      <span><i class="ui-icon check"></i>如导入失败，可返回帮助中心联系管理员。</span>
    </section>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import HeroHeader from '../components/HeroHeader.vue'
import GuideCard from '../components/GuideCard.vue'
import heroGuide from '@/assets/share/customer/hero-guide.png'
import guideApple from '@/assets/share/customer/guide-apple.png'
import guideAndroid from '@/assets/share/customer/guide-android.png'
import guideWindows from '@/assets/share/customer/guide-windows.png'
import guideLaptop from '@/assets/share/customer/guide-laptop.png'
import { guideCards } from '../data'

const imageMap = {
  apple: guideApple,
  android: guideAndroid,
  windows: guideWindows,
  laptop: guideLaptop
}

const cards = computed(() => guideCards.map(card => ({ ...card, image: imageMap[card.icon] })))
const mobileCards = computed(() => cards.value.filter(card => card.section === 'mobile'))
const desktopCards = computed(() => cards.value.filter(card => card.section === 'desktop'))

defineEmits(['copy-text', 'open-url', 'navigate'])
</script>

<template>
  <section class="share-screen mobile-screen">
    <HeroHeader
      crumb="我的订阅 / 导入教程 / 手机端"
      title="手机端导入教程"
      desc="适用于 Android 与 iPhone / iPad 用户，支持 v2rayNG 与 Shadowrocket 小火箭 导入。"
      :image="heroMobile"
      variant="tutorial-hero mobile-hero"
    >
      <template #actions>
        <button type="button" class="share-outline hero-btn" @click="$emit('navigate', 'overview')"><span class="ui-icon back-arrow"></span>返回订阅详情</button>
        <button type="button" class="share-primary hero-btn" @click="$emit('navigate', 'download')"><span class="ui-icon download"></span>下载手机端客户端</button>
      </template>
    </HeroHeader>

    <div class="mobile-recommend-grid">
      <article><ClientIcon type="android" size="xl" /><div><strong>Android 推荐</strong><b>v2rayNG</b><small>支持 VLESS / VMess 节点，操作简单易用</small></div><span class="watermark android-mark"></span></article>
      <article><ClientIcon type="shadowrocket" size="xl" /><div><strong>iOS 推荐</strong><b>Shadowrocket 小火箭</b><small>稳定可靠，支持二维码与链接导入</small></div><span class="watermark rocket-mark"></span></article>
      <article><ClientIcon type="clash" size="xl" /><div><strong>推荐方式</strong><b>链接导入 / 二维码扫码</b><small>两种方式任选，快速导入节点</small></div><span class="watermark guide-mark"></span></article>
    </div>

    <div class="mobile-method-grid">
      <section class="share-panel mobile-method android-method">
        <h2><ClientIcon type="android" />Android · v2rayNG 导入 <i>Android</i></h2>
        <p>适合直接导入 VLESS 原始链接</p>
        <div class="round-step-grid green">
          <div v-for="(step, index) in androidSteps" :key="step.title"><span>{{ index + 1 }}</span><strong>{{ step.title }}</strong><small>{{ step.desc }}</small></div>
        </div>
        <div class="mobile-actions tip-only">
          <span class="green-tip"><i class="ui-icon info"></i>首次使用请允许 VPN 连接权限。</span>
        </div>
      </section>

      <section class="share-panel mobile-method ios-method">
        <h2><ClientIcon type="shadowrocket" />iPhone / iPad · Shadowrocket 小火箭 <i>iOS</i></h2>
        <p>二维码请在订阅列表打开节点详情页后扫码导入</p>
        <div class="round-step-grid blue">
          <div v-for="(step, index) in iosSteps" :key="step.title"><span>{{ index + 1 }}</span><strong>{{ step.title }}</strong><small>{{ step.desc }}</small></div>
        </div>
        <div class="mobile-actions tip-only">
          <span><i class="ui-icon info"></i>请先回到订阅列表，选择节点详情页中的二维码扫码导入。</span>
        </div>
      </section>
    </div>

    <div class="mobile-bottom-grid">
      <section class="share-panel mobile-download-panel">
        <h2><span class="title-icon download"></span>移动端下载</h2>
        <div class="mobile-download-cards">
          <button type="button" @click="$emit('navigate', 'download')"><ClientIcon type="v2rayng" size="xl" /><strong>Android 下载</strong><b>v2rayNG</b><small>轻量高效，支持 VLESS / VMess 等多种协议</small><span><i class="ui-icon download"></i>下载 v2rayNG</span></button>
          <button type="button" @click="$emit('navigate', 'download')"><ClientIcon type="shadowrocket" size="xl" /><strong>iOS 下载</strong><b>Shadowrocket 小火箭</b><small>功能强大，稳定可靠，支持多种协议与规则</small><span><i class="ui-icon phone"></i>在 App Store 下载</span></button>
        </div>
      </section>
      <section class="share-panel mobile-faq-panel">
        <h2><span>?</span>手机端常见问题</h2>
        <div class="mobile-faq-grid">
          <button v-for="item in mobileFaq" :key="item.title" type="button"><div><strong>{{ item.title }}</strong><small>{{ item.desc }}</small></div><span class="ui-icon chevron-right"></span></button>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import HeroHeader from '../components/HeroHeader.vue'
import ClientIcon from '../components/ClientIcon.vue'
import heroMobile from '@/assets/share/customer/hero-mobile.png'
import { androidSteps, iosSteps, mobileFaq } from '../data'

defineEmits(['navigate'])
</script>

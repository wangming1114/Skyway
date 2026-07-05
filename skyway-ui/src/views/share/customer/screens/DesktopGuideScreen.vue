<template>
  <section class="share-screen desktop-screen">
    <HeroHeader
      crumb="我的订阅 / 导入教程 / 桌面端"
      title="桌面端导入教程"
      desc="适用于 Windows / macOS 用户，支持 v2rayN 与 Clash Verge Rev 两种常见导入方式。"
      :image="heroDesktop"
      variant="tutorial-hero"
    >
      <template #actions>
        <button type="button" class="share-outline hero-btn" @click="$emit('navigate', 'overview')">
          <img :src="iconBackArrow" alt="" />返回订阅详情
        </button>
        <button type="button" class="share-primary hero-btn" @click="$emit('navigate', 'download')">
          <img :src="iconDownloadTray" alt="" />下载桌面客户端
        </button>
      </template>
    </HeroHeader>

    <div class="desktop-feature-bar">
      <article><span class="summary-icon recommend"></span><div><strong>推荐方式</strong><b>v2rayN / Clash Verge Rev</b><small>两种主流方式，按需选择使用</small></div></article>
      <article><span class="summary-icon protocol"></span><div><strong>支持协议</strong><b>VLESS / Clash</b><small>多种协议灵活兼容</small></div></article>
      <article><span class="summary-icon platform teal"></span><div><strong>适用系统</strong><b>Windows / macOS</b><small>覆盖主流桌面操作系统</small></div></article>
    </div>

    <div class="desktop-method-grid">
      <section class="share-panel method-card">
        <h2><ClientIcon type="v2ray" size="lg" />方式一：导入到 v2rayN</h2>
        <div class="badge-row"><img :src="tagWindows" alt="Windows" /><span>适合使用 VLESS 原始链接进行快速导入</span></div>
        <div class="method-body">
          <StepList :steps="desktopV2raySteps" />
          <img class="client-shot" :src="shotV2rayn" alt="v2rayN 导入示意" />
        </div>
        <div class="method-actions">
          <button type="button" class="share-primary" @click="$emit('copy-vless')"><img :src="iconCopy" alt="" />复制 VLESS</button>
          <button type="button" class="share-outline" @click="$emit('open-guide', 'v2rayN')">查看原始链接<img :src="iconExternalLink" alt="" /></button>
          <span><i>i</i>导入后建议先进行延迟测试，再设置系统代理。</span>
        </div>
      </section>

      <section class="share-panel method-card">
        <h2><ClientIcon type="clash" size="lg" />方式二：导入到 Clash Verge Rev</h2>
        <div class="badge-row"><img :src="tagWindows" alt="Windows" /><img :src="tagMacos" alt="macOS" /><span>适合使用 Clash 订阅地址进行自动更新管理</span></div>
        <div class="method-body">
          <StepList :steps="desktopClashSteps" purple />
          <img class="client-shot" :src="shotClash" alt="Clash Verge Rev 导入示意" />
        </div>
        <div class="method-actions">
          <button type="button" class="share-primary purple-btn" @click="$emit('copy-clash')"><img :src="iconCopy" alt="" />复制 Clash 订阅</button>
          <button type="button" class="share-outline" @click="$emit('open-guide', 'clash')">打开订阅教程<img :src="iconExternalLink" alt="" /></button>
          <span class="purple-tip"><i>i</i>建议开启自动更新订阅，便于节点变化后及时同步。</span>
        </div>
      </section>
    </div>

    <div class="desktop-bottom-grid">
      <section class="share-panel desktop-faq">
        <h2><img class="panel-title-icon" :src="desktopIconHelp" alt="" />常见问题</h2>
        <button v-for="item in desktopFaq" :key="item[0]" type="button">
          <strong>{{ item[0] }}</strong><small>{{ item[1] }}</small><span class="ui-icon chevron-right"></span>
        </button>
      </section>
      <section class="share-panel desktop-download">
        <h2><img class="panel-title-icon" :src="iconDownloadArrow" alt="" />桌面端下载</h2>
        <div class="desktop-download-cards">
          <button type="button" @click="$emit('navigate', 'download')">
            <ClientIcon type="v2ray" size="lg" />
            <div class="desktop-download-copy">
              <strong>v2rayN <img :src="tagWindows" alt="Windows" /></strong>
              <small>轻量高效，支持 VLESS 等多种协议。</small>
            </div>
            <span class="desktop-download-cta"><img :src="iconDownloadTray" alt="" />立即下载</span>
          </button>
          <button type="button" @click="$emit('navigate', 'download')">
            <ClientIcon type="clash" size="lg" />
            <div class="desktop-download-copy">
              <strong>Clash Verge Rev <img :src="tagWindows" alt="Windows" /><img :src="tagMacos" alt="macOS" /></strong>
              <small>精美易用，支持订阅管理与规则分流。</small>
            </div>
            <span class="desktop-download-cta"><img :src="iconDownloadTray" alt="" />立即下载</span>
          </button>
        </div>
      </section>
    </div>

    <section class="share-panel help-strip">
      <div><img class="help-icon" :src="desktopIconContact" alt="" /><strong>还需要帮助？</strong><small>如在导入过程中遇到问题，可联系我们获取协助。</small></div>
      <button type="button" class="share-primary"><img :src="desktopIconContact" alt="" />联系管理员</button>
      <button type="button" class="share-outline" @click="$emit('navigate', 'guide')"><img :src="desktopIconHelp" alt="" />查看帮助中心</button>
    </section>
  </section>
</template>

<script setup>
import HeroHeader from '../components/HeroHeader.vue'
import ClientIcon from '../components/ClientIcon.vue'
import StepList from '../components/StepList.vue'
import heroDesktop from '@/assets/share/customer/hero-desktop.png'
import shotV2rayn from '@/assets/share/customer/shot-v2rayn.png'
import shotClash from '@/assets/share/customer/shot-clash.png'
import desktopIconHelp from '@/assets/share/customer/desktop-icon-help.png'
import desktopIconContact from '@/assets/share/customer/desktop-icon-contact.png'
import iconBackArrow from '@/assets/share/customer/icon-back-arrow.png'
import iconCopy from '@/assets/share/customer/icon-copy.png'
import iconDownloadArrow from '@/assets/share/customer/icon-download-arrow.png'
import iconDownloadTray from '@/assets/share/customer/icon-download-tray.png'
import iconExternalLink from '@/assets/share/customer/icon-external-link.png'
import tagWindows from '@/assets/share/customer/tag-windows.png'
import tagMacos from '@/assets/share/customer/tag-macos.png'
import { desktopClashSteps, desktopFaq, desktopV2raySteps } from '../data'

defineEmits(['navigate', 'copy-vless', 'copy-clash', 'open-guide'])
</script>

<template>
  <section class="share-screen download-screen">
    <HeroHeader
      crumb="我的订阅 / 下载中心"
      title="下载中心"
      desc="根据设备类型选择合适的客户端，支持桌面端与手机端下载。"
      :image="heroDownload"
      variant="download-hero"
    >
      <template #below>
        <div class="download-tabs">
          <button type="button" class="active"><span class="title-icon all"></span>全部客户端</button>
          <button type="button"><span class="title-icon desktop"></span>桌面端</button>
          <button type="button"><span class="title-icon mobile"></span>手机端</button>
          <button type="button"><span class="title-icon recommend"></span>推荐客户端</button>
        </div>
      </template>
      <template #actions>
        <button type="button" class="share-outline hero-btn" @click="$emit('navigate', 'overview')"><span class="ui-icon back-arrow"></span>返回我的订阅</button>
      </template>
    </HeroHeader>

    <section class="download-note-strip">
      <div><strong>下载后导入</strong><small>打开客户端后使用订阅链接、原始链接或扫码完成导入。</small></div>
      <div><strong>优先官方版本</strong><small>建议从官方网站或 Release 页面下载，避免来源不明的安装包。</small></div>
      <div><strong>保持客户端更新</strong><small>新版客户端通常包含协议兼容、安全修复与性能优化。</small></div>
    </section>

    <div class="download-grid">
      <main>
        <section class="share-panel download-section">
          <h2><span class="title-icon desktop"></span>桌面端下载</h2>
          <div class="download-card-grid">
            <DownloadClientCard type="v2ray" name="v2rayN" platform="Windows" version="v7.2.4" desc="轻量、易用，适合 VLESS 链接导入" tags="VLESS,Reality,gRPC" @tutorial="$emit('navigate', 'desktop')" @download="$emit('download-client', 'v2rayN')" />
            <DownloadClientCard type="clash" name="Clash Verge Rev" platform="Windows  macOS" version="v2.1.0" desc="现代化 Clash 客户端，适合订阅管理" tags="Clash,Clash Meta,自动更新" @tutorial="$emit('navigate', 'desktop')" @download="$emit('download-client', 'clash')" />
          </div>
        </section>
        <section class="share-panel download-section">
          <h2><span class="title-icon mobile"></span>手机端下载</h2>
          <div class="download-card-grid">
            <DownloadClientCard type="v2rayng" name="v2rayNG" platform="Android" version="v1.9.40" desc="Android 平台 V2Ray 客户端，支持 VLESS 导入" tags="Android,VLESS" @tutorial="$emit('navigate', 'mobile')" @download="$emit('download-client', 'v2rayNG')" />
            <DownloadClientCard type="shadowrocket" name="Shadowrocket 小火箭" platform="iOS" version="App Store" desc="iPhone / iPad 常用代理客户端，支持扫码导入" tags="iOS,二维码,订阅" @tutorial="$emit('navigate', 'mobile')" @download="$emit('download-client', 'shadowrocket')" />
          </div>
        </section>
        <section class="share-panel compare-section">
          <h2><span class="title-icon compare"></span>客户端对比</h2>
          <table>
            <thead><tr><th>客户端</th><th>平台</th><th>推荐导入方式</th><th>支持协议</th><th>适用人群</th></tr></thead>
            <tbody>
              <tr v-for="row in compareRows" :key="row.name"><td>{{ row.name }}</td><td>{{ row.platform }}</td><td>{{ row.importWay }}</td><td>{{ row.protocols }}</td><td>{{ row.people }}</td></tr>
            </tbody>
          </table>
        </section>
      </main>
    </div>
  </section>
</template>

<script setup>
import HeroHeader from '../components/HeroHeader.vue'
import DownloadClientCard from '../components/DownloadClientCard.vue'
import heroDownload from '@/assets/share/customer/hero-download.png'
import { compareRows } from '../data'

defineEmits(['navigate', 'download-client'])
</script>

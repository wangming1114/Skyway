<template>
  <section class="share-screen node-detail-screen">
    <HeroHeader
      crumb="我的订阅 / 节点详情"
      :title="node?.name || '节点详情'"
      :desc="node ? `${node.endpoint || '-'} · ${node.protocol || 'VLESS'}` : '查看当前节点的导入方式、订阅链接与二维码。'"
      :image="heroNodeDetail"
      variant="tutorial-hero node-detail-hero"
    >
      <template #actions>
        <button type="button" class="share-outline hero-btn" @click="$emit('navigate', 'overview')">
          <img :src="iconBackArrow" alt="" />返回订阅列表
        </button>
          <button type="button" class="share-primary hero-btn node-download-btn" @click="$emit('navigate', 'download')">
            <img :src="iconDownloadTray" alt="" />下载客户端
          </button>
      </template>
    </HeroHeader>

    <div class="node-detail-summary">
      <article>
        <span class="summary-icon protocol"></span>
        <div><strong>节点协议</strong><b>{{ node?.protocol || 'VLESS-REALITY' }}</b><small>按协议选择对应客户端导入</small></div>
      </article>
      <article>
        <span class="summary-icon endpoint"></span>
        <div><strong>地址端口</strong><b>{{ node?.endpoint || '-' }}</b><small>连接时使用的节点入口</small></div>
      </article>
      <article>
        <span class="summary-icon expire"></span>
        <div><strong>有效期</strong><b>{{ formatDate(node?.expireTime) }}</b><small>{{ node?.remainingLabel || '长期可用' }}</small></div>
      </article>
      <article>
        <span class="summary-icon status"></span>
        <div><strong>节点状态</strong><b>{{ node?.isExpiringSoon ? '即将到期' : node?.statusText || '正常' }}</b><small>建议导入后先测试延迟</small></div>
      </article>
    </div>

    <div class="node-detail-layout">
      <section class="share-panel method-card node-method-card">
        <h2><ClientIcon type="v2ray" size="lg" />方式一：使用 VLESS 原始链接</h2>
        <div class="badge-row"><img :src="tagWindows" alt="Windows" /><span>适合 v2rayN、v2rayNG、Shadowrocket 等客户端</span></div>
        <div class="method-body">
          <StepList :steps="desktopV2raySteps" />
          <div class="node-link-box">
            <div class="node-link-head">
              <strong>当前节点 VLESS</strong>
              <button type="button" class="share-primary small" :disabled="!node?.vlessUrl" @click="$emit('copy-vless', node)">复制 VLESS</button>
            </div>
            <textarea readonly :value="node?.vlessUrl || '当前节点暂无 VLESS 原始链接'"></textarea>
          </div>
        </div>
        <div class="method-actions node-method-actions">
          <button type="button" class="share-primary node-action-btn" :disabled="!node?.vlessUrl" @click="$emit('copy-vless', node)"><img :src="iconCopy" alt="" />复制 VLESS</button>
          <button type="button" class="share-outline node-action-btn" @click="$emit('open-guide', 'v2rayN')">查看 v2rayN 教程<img :src="iconExternalLink" alt="" /></button>
          <span><i>i</i>导入后建议先进行延迟测试，再设置系统代理。</span>
        </div>
      </section>

      <section class="share-panel method-card node-method-card">
        <h2><ClientIcon type="clash" size="lg" />方式二：使用 Clash Verge 订阅</h2>
        <div class="badge-row"><img :src="tagWindows" alt="Windows" /><img :src="tagMacos" alt="macOS" /><span>适合 Clash Verge Rev 自动更新管理</span></div>
        <div class="method-body">
          <StepList :steps="desktopClashSteps" purple />
          <div class="node-link-box purple">
            <div class="node-link-head">
              <strong>当前节点 Clash 订阅</strong>
              <button type="button" class="share-primary small purple-btn" :disabled="!node?.clashUrl" @click="$emit('copy-clash', node)">复制 Clash</button>
            </div>
            <textarea readonly :value="node?.clashUrl || '当前节点暂无 Clash 订阅地址'"></textarea>
          </div>
        </div>
        <div class="method-actions node-method-actions">
          <button type="button" class="share-primary purple-btn node-action-btn" :disabled="!node?.clashUrl" @click="$emit('copy-clash', node)"><img :src="iconCopy" alt="" />复制 Clash</button>
          <button type="button" class="share-outline node-action-btn" @click="$emit('open-guide', 'clash')">打开 Clash 教程<img :src="iconExternalLink" alt="" /></button>
          <span class="purple-tip"><i>i</i>建议开启自动更新订阅，便于节点变化后及时同步。</span>
        </div>
      </section>
    </div>

    <div class="node-detail-bottom">
      <section class="share-panel node-qr-panel polished-node-card">
        <div class="node-card-head">
          <ClientIcon type="shadowrocket" />
          <div>
            <h2>手机端扫码导入</h2>
            <p>二维码内容为当前节点的 VLESS 原始链接，可用于 Shadowrocket、v2rayNG 等客户端。</p>
          </div>
        </div>
        <div class="node-qr-layout">
          <div class="node-qr-copy">
            <strong>当前节点二维码</strong>
            <small>{{ node?.name || '订阅节点' }}</small>
            <div class="dialog-actions node-secondary-actions">
              <button type="button" class="share-outline small" :disabled="!node?.vlessUrl" @click="$emit('copy-vless', node)">复制链接</button>
              <button type="button" class="share-outline small" :disabled="!qrUrl" @click="$emit('download-qr')">下载二维码</button>
            </div>
          </div>
          <div class="dialog-qr">
            <img v-if="qrUrl" :src="qrUrl" alt="订阅二维码" />
            <span v-else>{{ node?.vlessUrl ? '生成中...' : '暂无二维码' }}</span>
          </div>
        </div>
      </section>

      <section class="share-panel node-detail-tips polished-node-card">
        <div class="node-card-head">
          <span class="section-lightning-icon"></span>
          <div>
            <h2>导入提示</h2>
            <p>按当前节点状态选择合适的导入方式。</p>
          </div>
        </div>
        <button type="button"><i>1</i><strong>先复制再导入</strong><small>桌面端优先复制当前节点链接或 Clash 订阅。</small><span class="ui-icon chevron-right"></span></button>
        <button type="button"><i>2</i><strong>二维码用于手机端</strong><small>手机端可直接扫码，也可以复制链接后粘贴。</small><span class="ui-icon chevron-right"></span></button>
        <button type="button"><i>3</i><strong>连接前测试延迟</strong><small>导入成功后建议先测试延迟，再启用系统代理。</small><span class="ui-icon chevron-right"></span></button>
        <button type="button"><i>4</i><strong>无法导入时</strong><small>检查订阅是否到期，或联系管理员重新生成。</small><span class="ui-icon chevron-right"></span></button>
      </section>
    </div>
  </section>
</template>

<script setup>
import HeroHeader from '../components/HeroHeader.vue'
import ClientIcon from '../components/ClientIcon.vue'
import StepList from '../components/StepList.vue'
import heroNodeDetail from '@/assets/share/customer/hero-node-detail.png'
import iconBackArrow from '@/assets/share/customer/icon-back-arrow.png'
import iconCopy from '@/assets/share/customer/icon-copy.png'
import iconDownloadTray from '@/assets/share/customer/icon-download-tray.png'
import iconExternalLink from '@/assets/share/customer/icon-external-link.png'
import tagWindows from '@/assets/share/customer/tag-windows.png'
import tagMacos from '@/assets/share/customer/tag-macos.png'
import { desktopClashSteps, desktopV2raySteps } from '../data'
import { parseTime } from '@/utils/skyway'

defineProps({
  node: { type: Object, default: null },
  qrUrl: { type: String, default: '' }
})

defineEmits(['navigate', 'copy-vless', 'copy-clash', 'download-qr', 'open-guide'])

function formatDate(value) {
  return value ? parseTime(value, '{y}-{m}-{d}') : '永久'
}
</script>

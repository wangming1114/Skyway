<template>
  <section class="share-screen overview-screen">
    <HeroHeader
      title="我的订阅"
      desc="查看已开通订阅的使用状态、有效期、配置信息，并快速完成桌面端与手机端导入。"
      :image="heroOverview"
      variant="overview-hero"
    >
      <template #actions>
        <button type="button" class="share-primary refresh-btn" :disabled="loading" @click="$emit('refresh')">
          <span class="ui-icon refresh"></span>刷新订阅
        </button>
      </template>
    </HeroHeader>

    <div class="overview-stats">
      <article class="stat-card">
        <span class="summary-icon active"></span>
        <div><strong>活跃订阅</strong><b>{{ summary.activeCount }}</b><small>当前可正常使用的订阅数量</small></div>
      </article>
      <article class="stat-card">
        <span class="summary-icon expiring"></span>
        <div><strong>即将到期</strong><b>{{ summary.expiringSoonCount }}</b><small>7 天内到期的订阅数量</small></div>
      </article>
      <article class="stat-card">
        <span class="summary-icon protocol"></span>
        <div><strong>支持协议</strong><b>VLESS / Clash</b><small>多种主流协议灵活兼容</small></div>
      </article>
      <article class="stat-card">
        <span class="summary-icon platform"></span>
        <div><strong>支持平台</strong><b>Windows / macOS / Android / iOS</b><small>覆盖主流桌面端与移动端设备</small></div>
      </article>
    </div>

    <div class="overview-main-grid">
      <section class="share-panel subscription-list-panel">
        <div class="panel-head">
          <div>
            <h2><span class="section-list-icon"></span>订阅列表</h2>
            <p>管理您的订阅节点，查看有效期、状态与导入入口</p>
          </div>
          <div class="table-tools">
            <label class="status-filter">
              <select :value="statusFilter" @change="$emit('update:statusFilter', $event.target.value)">
                <option value="all">全部状态</option>
                <option value="active">正常</option>
                <option value="expiring">即将到期</option>
                <option value="expired">已过期</option>
                <option value="disabled">停用</option>
              </select>
              <span class="ui-icon caret-down"></span>
            </label>
            <label>
              <input :value="keyword" placeholder="搜索节点名称或地址" @input="$emit('update:keyword', $event.target.value)" />
              <span class="ui-icon search"></span>
            </label>
          </div>
        </div>
        <div class="share-table">
          <div class="share-tr share-th">
            <span>节点名称</span><span>地址端口</span><span>有效期</span><span>状态</span><span>操作</span>
          </div>
          <div v-for="node in nodes" :key="node.id" class="share-tr">
            <div class="node-title-cell">
              <button type="button" class="star-btn" aria-label="收藏节点"><img :src="tagStar" alt="" /></button>
              <div>
                <strong>{{ node.name }}</strong>
              </div>
            </div>
            <span class="mono">{{ node.endpoint }}</span>
            <span class="date-cell">{{ formatDate(node.expireTime) }}<small :class="{ warn: node.isExpiringSoon }">{{ node.remainingLabel }}</small></span>
            <span>
              <img v-if="!node.isExpiringSoon && node.isActive" class="status-img" :src="statusNormal" :alt="node.statusText" />
              <i v-else :class="['status-chip', node.isExpiringSoon ? 'warning' : node.isExpired ? 'danger' : 'muted']">{{ node.isExpiringSoon ? '即将到期' : node.statusText }}</i>
            </span>
            <span class="table-actions">
              <button type="button" class="asset-action copy-subscription" aria-label="复制订阅" @click="$emit('copy-subscription', node)">
                复制订阅
              </button>
              <button type="button" class="asset-action detail" aria-label="查看详情" @click="$emit('detail', node)">
                查看详情
              </button>
            </span>
          </div>
          <div v-if="!nodes.length" class="empty-row">暂无可用节点</div>
        </div>
      </section>

      <aside class="overview-side-stack">
        <section class="share-panel quick-guide-panel">
          <h2><span class="section-lightning-icon"></span>快速导入指南</h2>
          <p>按设备类型选择导入方式</p>
          <div class="quick-group desktop">
            <h3>桌面端导入</h3>
            <button type="button" class="quick-item" @click="$emit('navigate', 'desktop')">
              <ClientIcon type="v2ray" />
              <div><strong>v2rayN</strong><small>复制 VLESS 链接导入，支持 Reality / gRPC。</small></div>
              <span>查看教程</span>
            </button>
            <button type="button" class="quick-item" @click="$emit('navigate', 'desktop')">
              <ClientIcon type="clash" />
              <div><strong>Clash Verge Rev</strong><small>复制 Clash 订阅导入，支持自动更新。</small></div>
              <span>查看教程</span>
            </button>
          </div>
          <div class="quick-group mobile">
            <h3>手机端导入</h3>
            <button type="button" class="quick-item" @click="$emit('navigate', 'mobile')">
              <ClientIcon type="v2rayng" />
              <div><strong>v2rayNG</strong><small>Android 客户端，导入 VLESS 链接。</small></div>
              <span>查看教程</span>
            </button>
            <button type="button" class="quick-item" @click="$emit('navigate', 'mobile')">
              <ClientIcon type="shadowrocket" />
              <div><strong>Shadowrocket 小火箭</strong><small>iPhone/iPad 扫码导入或粘贴订阅。</small></div>
              <span>查看教程</span>
            </button>
          </div>
        </section>
      </aside>
    </div>

    <section class="share-panel download-overview-panel">
      <h2><img class="section-icon-img" :src="iconDownload" alt="" />客户端下载</h2>
      <div class="download-overview-lanes">
        <div class="download-lane desktop">
          <h3>桌面端下载</h3>
          <button type="button" class="mini-client" @click="$emit('navigate', 'download')">
            <ClientIcon type="v2ray" />
            <div><strong>v2rayN</strong><i>Windows</i><small>轻量易用的 v2Ray 客户端</small><img class="mini-button-img" :src="buttonDownloadNow" alt="立即下载" /></div>
          </button>
          <button type="button" class="mini-client" @click="$emit('navigate', 'download')">
            <ClientIcon type="clash" />
            <div><strong>Clash Verge Rev</strong><i>Windows</i><i>macOS</i><small>现代化的 Clash 客户端</small><img class="mini-button-img" :src="buttonDownloadNow" alt="立即下载" /></div>
          </button>
        </div>
        <div class="download-lane mobile">
          <h3>手机端下载</h3>
          <button type="button" class="mini-client" @click="$emit('navigate', 'download')">
            <ClientIcon type="v2rayng" />
            <div><strong>v2rayNG</strong><i>Android</i><small>适用于 Android 的 V2Ray 客户端</small><img class="mini-button-img green" :src="buttonGoDownload" alt="前往下载" /></div>
          </button>
          <button type="button" class="mini-client" @click="$emit('navigate', 'download')">
            <ClientIcon type="shadowrocket" />
            <div><strong>Shadowrocket 小火箭</strong><i>iOS</i><small>iOS 代理工具，稳定高效</small><img class="mini-button-img green" :src="buttonGoDownload" alt="前往下载" /></div>
          </button>
        </div>
      </div>
    </section>
  </section>
</template>

<script setup>
import HeroHeader from '../components/HeroHeader.vue'
import ClientIcon from '../components/ClientIcon.vue'
import heroOverview from '@/assets/share/customer/hero-desktop.png'
import iconDownload from '@/assets/share/customer/icon-download.png'
import statusNormal from '@/assets/share/customer/status-normal.png'
import buttonDownloadNow from '@/assets/share/customer/button-download-now.png'
import buttonGoDownload from '@/assets/share/customer/button-go-download.png'
import tagStar from '@/assets/share/customer/tag-star.png'
import { parseTime } from '@/utils/skyway'

defineProps({
  nodes: { type: Array, default: () => [] },
  summary: { type: Object, required: true },
  keyword: { type: String, default: '' },
  statusFilter: { type: String, default: 'all' },
  loading: { type: Boolean, default: false }
})

defineEmits(['update:keyword', 'update:statusFilter', 'refresh', 'navigate', 'detail', 'copy-subscription'])

function formatDate(value) {
  return value ? parseTime(value, '{y}-{m}-{d}') : '永久'
}
</script>

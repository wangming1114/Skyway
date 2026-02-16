<template>
  <div class="product-center">
    <div class="product-layout">
      <aside class="sidebar">
        <div class="sidebar-search">
          <input
            v-model="keyword"
            type="text"
            placeholder="请输入产品名称/关键字查找"
            class="search-input"
          />
        </div>
        <div class="sidebar-title">产品分类</div>
        <ul class="category-list">
          <li
            v-for="cat in categories"
            :key="cat.id"
            :class="['category-item', { active: currentCategory === cat.id }]"
            @click="currentCategory = cat.id"
          >
            {{ cat.name }}
            <span v-if="cat.hot" class="cat-tag">最新</span>
          </li>
        </ul>
      </aside>
      <div class="content">
        <div class="region-bar">
          <h2 class="region-title">区域({{ currentRegionName }})</h2>
          <p class="region-tip">线路说明与使用须知请以实际购买页为准，如有疑问请联系客服。</p>
        </div>
        <div class="product-cards">
          <div v-for="item in mockProducts" :key="item.id" class="p-card">
            <div class="p-card-head">
              <span class="p-card-series">{{ item.series }}</span>
            </div>
            <ul class="p-specs">
              <li><span class="label">CPU处理器</span><span class="val">{{ item.cpu }}</span></li>
              <li><span class="label">核心</span><span class="val">{{ item.cores }}</span></li>
              <li><span class="label">内存</span><span class="val">{{ item.memory }}</span></li>
              <li><span class="label">系统盘</span><span class="val">{{ item.systemDisk }}</span></li>
              <li><span class="label">数据盘</span><span class="val">{{ item.dataDisk }}</span></li>
              <li><span class="label">带宽</span><span class="val">{{ item.bandwidth }}</span></li>
              <li><span class="label">线路</span><span class="val">{{ item.line }}</span></li>
              <li><span class="label">可安装系统</span><span class="val">{{ item.os }}</span></li>
              <li><span class="label">虚拟化系统</span><span class="val">{{ item.virtualization }}</span></li>
            </ul>
            <div class="p-card-footer">
              <div class="p-price">¥{{ item.price }} 起/月</div>
              <button type="button" class="btn-buy">立即购买</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const keyword = ref('')
const currentCategory = ref('linux-9')

const categories = [
  { id: 'ecs-cn', name: '中国内地云服务器 ECS' },
  { id: 'ecs-hk', name: '中国香港云服务器 ECS' },
  { id: 'ecs-overseas', name: '境外国外云服务器 ECS' },
  { id: 'linux-9', name: '美国9区-Linux专区' },
  { id: 'linux-35', name: '美国35区-Linux专区', hot: true },
  { id: 'ddh-cn', name: '中国内地物理机/宿主机 DDH' },
  { id: 'ddh-overseas', name: '境外海外物理机/宿主机 DDH' }
]

const currentRegionName = computed(() => {
  const c = categories.find(x => x.id === currentCategory.value)
  return c ? c.name : '美国9区-Linux专区'
})

const mockProducts = ref([
  {
    id: 1,
    series: '4H系列',
    cpu: '英特尔® 至强® E5-2696 v4',
    cores: '4H 100%性能',
    memory: '4-8G 高速DDR4',
    systemDisk: '40G U.2高性能硬盘',
    dataDisk: '自选 U.2读取高性能硬盘',
    bandwidth: '100Mbps',
    line: '163电信4134 联通4837 移动58453线路',
    os: '仅支持Linux系统',
    virtualization: 'KVM',
    price: '25.90'
  },
  {
    id: 2,
    series: '8H系列',
    cpu: '英特尔® 至强® E5-2696 v4',
    cores: '8H 100%性能',
    memory: '8-16G 高速DDR4',
    systemDisk: '80G U.2高性能硬盘',
    dataDisk: '自选 U.2读取高性能硬盘',
    bandwidth: '100Mbps',
    line: '163电信4134 联通4837 移动58453线路',
    os: '仅支持Linux系统',
    virtualization: 'KVM',
    price: '45.90'
  },
  {
    id: 3,
    series: '16H系列',
    cpu: '英特尔® 至强® E5-2696 v4',
    cores: '16H 100%性能',
    memory: '16-32G 高速DDR4',
    systemDisk: '120G U.2高性能硬盘',
    dataDisk: '自选 U.2读取高性能硬盘',
    bandwidth: '100Mbps',
    line: '163电信4134 联通4837 移动58453线路',
    os: '仅支持Linux系统',
    virtualization: 'KVM',
    price: '85.90'
  }
])
</script>

<style scoped>
.product-center { padding: 20px; min-height: calc(100vh - 56px); background: #f8fafc; }
.product-layout { display: flex; gap: 20px; max-width: 1280px; margin: 0 auto; }
.sidebar {
  width: 240px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  padding: 16px;
  height: fit-content;
}
.sidebar-search { margin-bottom: 16px; }
.search-input {
  width: 100%;
  padding: 8px 12px;
  font-size: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  outline: none;
}
.search-input:focus { border-color: #2563eb; }
.search-input::placeholder { color: #94a3b8; }
.sidebar-title { font-size: 14px; font-weight: 600; color: #0f172a; margin-bottom: 12px; }
.category-list { list-style: none; padding: 0; margin: 0; }
.category-item {
  padding: 10px 12px;
  font-size: 13px;
  color: #475569;
  cursor: pointer;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.category-item:hover { background: #f8fafc; }
.category-item.active { background: #eff6ff; color: #2563eb; font-weight: 500; }
.cat-tag { font-size: 11px; padding: 2px 6px; background: #fef2f2; color: #dc2626; border-radius: 4px; margin-left: 6px; }
.content { flex: 1; min-width: 0; }
.region-bar { margin-bottom: 20px; }
.region-title { margin: 0 0 8px; font-size: 18px; font-weight: 600; color: #0f172a; }
.region-tip { margin: 0; font-size: 13px; color: #b45309; line-height: 1.5; }
.product-cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 20px; }
.p-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  flex-direction: column;
}
.p-card-head { margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid #f1f5f9; }
.p-card-series { font-size: 16px; font-weight: 600; color: #0f172a; }
.p-specs {
  list-style: none;
  padding: 0;
  margin: 0 0 20px 0;
  font-size: 13px;
  flex: 1;
}
.p-specs li { display: flex; padding: 6px 0; line-height: 1.4; }
.p-specs .label { color: #64748b; width: 100px; flex-shrink: 0; }
.p-specs .val { color: #334155; flex: 1; }
.p-card-footer { padding-top: 16px; border-top: 1px solid #f1f5f9; }
.p-price { font-size: 20px; font-weight: 700; color: #2563eb; margin-bottom: 12px; }
.btn-buy {
  width: 100%;
  padding: 10px 16px;
  font-size: 14px;
  font-weight: 500;
  background: #2563eb;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}
.btn-buy:hover { background: #1d4ed8; }
</style>

export const clientLinks = {
  v2rayN: 'https://github.com/2dust/v2rayN/releases',
  clash: 'https://github.com/clash-verge-rev/clash-verge-rev/releases',
  v2rayNG: 'https://github.com/2dust/v2rayNG/releases',
  shadowrocket: 'https://apps.apple.com/us/app/shadowrocket/id932747118'
}

export const desktopV2raySteps = [
  '复制 VLESS 链接',
  '打开 v2rayN 客户端',
  '在“服务器”菜单选择“从剪贴板导入批量链接”',
  '启用节点并测试延迟'
]

export const desktopClashSteps = [
  '复制 Clash 订阅地址',
  '打开 Clash Verge Rev',
  '在“Profiles / 订阅”中新增订阅并粘贴地址',
  '更新订阅、选择配置并启用系统代理'
]

export const androidSteps = [
  { title: '复制 VLESS 链接', desc: '在订阅详情页点击「复制 VLESS」' },
  { title: '打开 v2rayNG', desc: '安装并打开 v2rayNG 客户端' },
  { title: '点击右上角 +', desc: '选择「从剪贴板导入」自动读取链接' },
  { title: '选中节点并连接', desc: '选择导入的节点，点击连接即可' }
]

export const iosSteps = [
  { title: '打开节点详情', desc: '回到订阅列表，点击需要导入的节点详情' },
  { title: '打开 Shadowrocket', desc: '安装并打开 Shadowrocket 小火箭' },
  { title: '扫描详情二维码', desc: '使用详情页二维码完成解析导入' },
  { title: '保存节点并连接', desc: '点击「完成」保存，选择节点并连接' }
]

export const overviewTips = [
  { icon: 'time', title: '订阅到期后将无法使用', desc: '订阅到期后将无法连接，请及时续费或联系管理员更新订阅。' },
  { icon: 'shield', title: '不同协议请使用对应客户端', desc: '请根据订阅协议类型（VLESS / Clash 等）选择对应的客户端使用。' },
  { icon: 'switch', title: '手机端与桌面端导入方式不同', desc: '手机端通常使用二维码或链接导入，桌面端多为链接或订阅地址导入。' },
  { icon: 'service', title: '如无法导入请联系管理员', desc: '遇到导入问题或使用异常，请联系管理员协助处理。' }
]

export const desktopFaq = [
  ['导入后无节点', '检查订阅链接是否完整、有效，或尝试重新订阅。'],
  ['更新失败', '网络不稳定或订阅失效，建议检查网络或重新获取订阅。'],
  ['无法连接', '检查节点状态、协议与端口，或切换节点后重试。'],
  ['系统代理未生效', '请确认已启用系统代理权限，并选择正确的代理模式。']
]

export const mobileFaq = [
  { title: '扫码失败', desc: '二维码无法识别或扫描后无反应' },
  { title: '系统 VPN 权限未开启', desc: '未授予 VPN 配置或连接权限' },
  { title: '链接无法粘贴', desc: '剪贴板无法粘贴或提示格式错误' },
  { title: 'iOS 网络权限异常', desc: 'iOS 系统限制导致网络连接异常' },
  { title: '节点导入成功但不能连', desc: '连接超时、延迟高或无法访问网络' }
]

export const downloadNotes = [
  { icon: 'link', title: '下载后如何导入？', desc: '打开客户端后，选择「订阅链接」或「扫码导入」，粘贴或扫描您的订阅链接即可完成导入。' },
  { icon: 'users', title: '是否需要管理员提供订阅？', desc: '是的，您需要从订阅管理员或服务提供商处获取订阅链接，才能在客户端中使用。' },
  { icon: 'bulb', title: '为什么不同设备推荐不同客户端？', desc: '不同客户端在功能、协议支持、系统兼容性和使用体验上有所差异。' }
]

export const warmNotes = [
  { title: '优先下载官方或 GitHub Release 版本', desc: '确保来源安全可靠，避免下载到篡改或捆绑恶意软件的版本。' },
  { title: '保持客户端更新', desc: '及时更新客户端可获得更好的性能、协议支持与安全修复。' },
  { title: '不随意泄露订阅链接', desc: '订阅链接等同于访问权限，避免分享给不可信的人，以防被滥用或封禁。' }
]

export const compareRows = [
  { name: 'v2rayN', platform: 'Windows', importWay: '订阅链接 / 手动添加', protocols: 'VLESS / VMess / Reality / gRPC', people: 'Windows 桌面用户，追求轻量与稳定' },
  { name: 'Clash Verge Rev', platform: 'Windows / macOS', importWay: '订阅链接 / 扫码导入', protocols: 'Clash / Clash Meta / VLESS 等', people: '需要订阅管理与规则分组的进阶用户' },
  { name: 'v2rayNG', platform: 'Android', importWay: '订阅链接 / 二维码', protocols: 'VLESS / VMess / Trojan 等', people: 'Android 手机用户，日常科学上网' },
  { name: 'Shadowrocket 小火箭', platform: 'iOS', importWay: '订阅链接 / 二维码', protocols: 'VLESS / VMess / Trojan 等', people: 'iOS 设备用户，注重易用与稳定性' }
]

export const guideCards = [
  {
    section: 'mobile',
    title: '苹果手机 / iPad',
    device: 'iPhone、iPad',
    client: 'Shadowrocket 小火箭',
    url: 'https://doc.theojs.cn/serve/antiwall/shadowrocket',
    accent: 'blue',
    icon: 'apple'
  },
  {
    section: 'mobile',
    title: '安卓手机',
    device: 'Android 安卓手机',
    client: 'v2rayNG',
    url: 'https://v2rayng.wiki/',
    accent: 'green',
    icon: 'android'
  },
  {
    section: 'desktop',
    title: 'Windows',
    device: 'Windows 电脑',
    client: 'v2rayN',
    url: 'https://doc.theojs.cn/serve/antiwall/v2rayn',
    accent: 'blue',
    icon: 'windows'
  },
  {
    section: 'desktop',
    title: 'Windows / macOS',
    device: 'Windows、macOS 电脑',
    client: 'Clash Verge Rev',
    url: 'https://doc.theojs.cn/serve/antiwall/clash-verge-rev',
    accent: 'purple',
    icon: 'laptop'
  }
]

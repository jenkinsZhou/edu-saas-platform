import {
  BookOutlined,
  ReadOutlined,
  BankOutlined,
  CrownOutlined,
  RocketOutlined,
  BulbOutlined,
  TrophyOutlined,
  AppstoreOutlined,
  ClusterOutlined,
  FireOutlined,
  StarOutlined,
  ThunderboltOutlined
} from '@ant-design/icons-vue'
import type { Component } from 'vue'

/** 可选品牌图标（侧边栏 Logo）。key 持久化到机构主题，跨组件用 resolveBrandIcon 还原。 */
export interface BrandIconOption {
  key: string
  label: string
  component: Component
}

export const BRAND_ICONS: BrandIconOption[] = [
  { key: 'book', label: '书籍', component: BookOutlined },
  { key: 'read', label: '阅读', component: ReadOutlined },
  { key: 'bank', label: '院校', component: BankOutlined },
  { key: 'crown', label: '皇冠', component: CrownOutlined },
  { key: 'rocket', label: '火箭', component: RocketOutlined },
  { key: 'bulb', label: '灵感', component: BulbOutlined },
  { key: 'trophy', label: '奖杯', component: TrophyOutlined },
  { key: 'appstore', label: '矩阵', component: AppstoreOutlined },
  { key: 'cluster', label: '多校区', component: ClusterOutlined },
  { key: 'fire', label: '热度', component: FireOutlined },
  { key: 'star', label: '星标', component: StarOutlined },
  { key: 'thunderbolt', label: '高效', component: ThunderboltOutlined }
]

const ICON_MAP: Record<string, Component> = Object.fromEntries(
  BRAND_ICONS.map((item) => [item.key, item.component])
)

export const DEFAULT_BRAND_ICON = 'book'

export function resolveBrandIcon(key?: string | null): Component {
  return (key && ICON_MAP[key]) || BookOutlined
}

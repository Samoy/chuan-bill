# 小川记账 (Chuan Bill) — 前端分析报告

> 分析日期: 2026-05-08  
> 技术栈: uni-app / Vue 3 / TypeScript / Pinia / Alova / wot-design-uni / UnoCSS

---

## 1. 页面与路由

### 1.1 路由机制

采用 `vite-plugin-uni-pages` 文件路由，页面从 `src/pages/` 自动发现，`pages.json` 由构建工具自动生成。原生 tabbar 被隐藏（`height: '0'`），改用 `src/layouts/tabbar.vue` 中的自定义 `<wd-tabbar>` 组件。

### 1.2 Tabbar 页面（底部导航）

| Tab | 路由路径 | 页面文件 | 导航栏标题 | 布局 |
|-----|---------|---------|-----------|------|
| 账单 | `pages/bill/index` | `src/pages/bill/index.vue` | 我的账单 | tabbar |
| 家庭 | `pages/family/index` | `src/pages/family/index.vue` | 家庭 | tabbar |
| 统计 | `pages/statistics/index` | `src/pages/statistics/index.vue` | 统计 | tabbar |
| 我的 | `pages/mine/index` | `src/pages/mine/index.vue` | 我的 | tabbar |

### 1.3 普通页面

| 路由路径 | 页面文件 | 导航栏标题 | 布局 |
|---------|---------|-----------|------|
| `pages/family/detail` | `src/pages/family/detail.vue` | 家庭详情 | default |
| `pages/family/edit` | `src/pages/family/edit.vue` | 编辑家庭 | default |
| `pages/family/bill` | `src/pages/family/bill.vue` | 家庭账单 | default |
| `pages/family/statistics` | `src/pages/family/statistics.vue` | 家庭统计 | default |
| `pages/mine/profile` | `src/pages/mine/profile.vue` | 个人信息 | default |
| `pages/mine/account` | `src/pages/mine/account.vue` | 账号与安全 | default |
| `pages/mine/about` | `src/pages/mine/about.vue` | 关于 | default |
| `pages/mine/help` | `src/pages/mine/help.vue` | 帮助与反馈 | default |
| `pages/message/index` | `src/pages/message/index.vue` | 消息通知 | default |
| `pages/agreement/index` | `src/pages/agreement/index.vue` | 用户协议 | default |
| `pages/privacy/index` | `src/pages/privacy/index.vue` | 隐私政策 | default |

### 1.4 子组件（非独立页面）

**账单页面组件** (`src/pages/bill/components/`):
- `QuickBillModal.vue` — 快速记账弹框（手动/OCR/语音入口）
- `ManualEdit.vue` — 手动记账表单
- `OcrEdit.vue` — 图片OCR识别账单
- `VoiceEdit.vue` — 语音识别账单
- `BillDetailModal.vue` — 账单详情弹框
- `BillDetail.vue` — 账单详情内容
- `BillItem.vue` — 账单列表项
- `BillSection.vue` — 按月分组的账单区块
- `BillCard.vue` — 识别结果预览卡片
- `FilterModal.vue` — 账单筛选弹框

**统计页面组件** (`src/pages/statistics/components/`):
- `CategoryChart.vue` — 分类饼图（ECharts）
- `DailyTrendChart.vue` — 每日收支趋势折线图（ECharts）
- `AiSuggestionCard.vue` — AI消费建议卡片

**家庭页面组件** (`src/pages/family/components/`):
- `MemberRankingChart.vue` — 成员收支排行图（ECharts）

**我的页面组件** (`src/pages/mine/components/`):
- `ThemePickerPopup.vue` — 主题切换弹框
- `NotificationSettingsPopup.vue` — 通知设置弹框
- `SyncStatusPopup.vue` — 账单同步状态弹框
- `ExportFilterPopup.vue` — 账单导出筛选弹框

**全局组件** (`src/components/`):
- `LoginPopup.vue` — 登录弹框（验证码/密码/微信登录）
- `GlobalToast.vue` — 全局 Toast 提示
- `GlobalMessage.vue` — 全局确认对话框
- `GlobalLoading.vue` — 全局加载指示器

---

## 2. 用户交互流程

### 2.1 登录流程

**入口**: 任何需要鉴权的操作都会触发 `userStore.requireAuth(callback)`，弹出 `LoginPopup`。

**三种登录方式**:
1. **验证码登录**: 输入手机号 → 发送验证码 (`POST /auth/send-code`) → 输入验证码 → `POST /auth/login-phone` → 返回 TokenVO
2. **密码登录**: 输入手机号 + 密码 → `POST /auth/login-password` → 返回 TokenVO
3. **微信登录**: `uni.login()` 获取 code → `POST /auth/login-wechat` → 返回 TokenVO

**登录成功后**:
- `userStore.login()` 保存 token、expireTime、userId、nickname
- 自动调用 `getProfile()` 获取完整用户资料
- 执行 `pendingCallback`（待执行的操作）

**自动注册**: 未注册手机号验证通过后自动注册。

**协议要求**: 登录前必须勾选同意《用户协议》和《隐私政策》。

### 2.2 账单记账流程

#### 2.2.1 手动记账
1. 点击 FAB 按钮或"开始记账" → 打开 `QuickBillModal`
2. 选择"手动添加"tab → 显示 `ManualEdit` 表单
3. 填写：账单类型（支出/收入）→ 金额 → 名称 → 时间 → 类目 → 支付方式 → 可选：共享到家庭 → 可选：备注
4. 提交：
   - **未登录**: `billStore.addLocalBill()` 存入本地（带 `syncStatus: 'init'`）
   - **已登录**: `POST /bill/add` 提交到服务器

#### 2.2.2 OCR图片识别记账
1. 选择"图片识别"tab → 显示 `OcrEdit`
2. 上传图片到 `POST /file/temp/upload`（需登录）
3. 上传成功后调用 `GET /ai/ocr?fileId=xxx&fileExt=xxx` 进行AI识别
4. 显示识别结果 `BillCard`，用户确认 → "确认入账"
5. OCR结果转入手动编辑表单进行确认和修改

#### 2.2.3 语音识别记账
1. 选择"语音识别"tab → 显示 `VoiceEdit`
2. 按住麦克风按钮录音（使用 `useAsr()` WebSocket ASR 实时转文字）
3. 松开后将识别文字发送到 `GET /ai/text?text=xxx`
4. AI返回 `BillVO` 结构，显示结果预览
5. 用户确认 → "确认入账"

**离线/本地模式**: 未登录时手动记账存储在 `billStore.localBillList`，支持最多1000条，登录后可通过 `POST /bill/batchCreate` 同步到服务器。

### 2.3 账单查看流程

1. 进入账单页面 → 调用 `GET /bill/page-list` 分页加载（每页10条）
2. 支持下拉刷新和触底加载更多
3. 搜索框可按名称/备注模糊搜索
4. 筛选功能支持：类型、类目、支付方式、金额范围、日期范围
5. 点击账单项 → 弹出 `BillDetailModal` 查看详情
6. 详情弹框内可编辑或删除账单

### 2.4 统计查看流程

#### 2.4.1 个人统计
1. 进入统计tab → 自动加载当月数据
2. 月份选择器（最近12个月，左右切换或弹出选择）
3. 并行加载三组数据：
   - **概览卡片**: 月度支出/收入/结余 (`GET /bill/monthly-stats`)
   - **分类饼图**: 按类目分类 (`GET /statistics/category`)
   - **每日趋势折线图**: (`GET /statistics/daily-trend`)
4. **AI消费建议**: `GET /ai/analysis` 获取AI分析结果（支持缓存）

#### 2.4.2 家庭统计
1. 从家庭详情页进入
2. 显示内容与个人统计类似，但传入 `familyId` 参数
3. 额外显示**成员收支排行榜** (`GET /statistics/members-bill`)
4. AI建议使用家庭分析类型

### 2.5 家庭管理流程

#### 2.5.1 创建家庭
1. 家庭页面 → "创建家庭" → 跳转 `family/edit` 页面
2. 填写家庭名称、头像、描述 → `POST /family/create`

#### 2.5.2 加入家庭
1. "加入家庭" → 弹出输入邀请码弹框
2. 输入6位数字邀请码 + 可选备注 → `POST /family/join`
3. 提交后等待户主审批

#### 2.5.3 家庭管理（户主权限）
- 查看邀请码、复制、刷新 (`POST /family/refresh-invite-code`)
- 处理加入申请：同意/拒绝 (`POST /family/handle-apply`)
- 移除成员 (`POST /family/remove-member`)
- 转让户主 (`POST /family/transfer-owner`)
- 编辑家庭信息 (`POST /family/update`)
- 解散/删除家庭 (`POST /family/delete`)

#### 2.5.4 家庭成员
- 退出家庭 (`POST /family/leave`)
- 查看家庭账单列表
- 查看家庭统计

### 2.6 个人设置流程

#### 2.6.1 个人信息编辑 (`/pages/mine/profile`)
- 修改头像（上传到 `POST /file/upload`）
- 修改昵称（最长20字）
- 修改性别（男/女/保密）
- 保存调用 `POST /user/profile/update`

#### 2.6.2 账号与安全 (`/pages/mine/account`)
- 查看/更换手机号（功能开发中）
- 修改密码（验证码方式）：`POST /user/password/update-by-code`
- 登录设备管理（功能开发中）
- 注销账号（功能开发中）

#### 2.6.3 其他设置
- **主题切换**: 跟随系统/手动切换（light/dark），支持自定义主题色
- **通知设置**: 通知偏好配置
- **账单同步**: 将本地账单批量同步到服务器
- **账单导出**: 按条件筛选后导出

---

## 3. API 集成层

### 3.1 架构概览

```
src/api/
├── apiDefinitions.ts    # 自动生成 - 所有API端点定义（方法+路径）
├── globals.d.ts         # 自动生成 - TypeScript类型定义（DTO/VO）
├── createApis.ts        # 自动生成 - Proxy-based API代理工厂
├── index.ts             # 创建Alova实例和Apis全局对象
├── core/
│   ├── instance.ts      # Alova实例配置（baseURL/token/缓存/加载状态）
│   ├── handlers.ts      # 响应处理（成功/错误/401处理）
│   └── middleware.ts    # 中间件（延迟加载/全局加载）
└── mock/
    └── mockAdapter.ts   # 开发环境Mock适配器
```

### 3.2 API调用方式

使用 `Apis.<tag>.<method>(config)` 模式，基于 Proxy 逐级解析到对应的 `[METHOD, URL]`：

```typescript
// 示例
Apis.bill.getPageBillList({ params: { page: 1, size: 10 } })
Apis.bill.addBill({ data: { name: '午餐', amount: '25.00', ... } })
Apis.ai.ocr({ params: { fileId, fileExt } })
```

### 3.3 完整API端点列表

#### 认证模块 (`auth.*`)

| 方法 | HTTP | 端点 | 用途 |
|------|------|------|------|
| `auth.loginByPhone` | POST | `/auth/login-phone` | 验证码登录 |
| `auth.loginByPassword` | POST | `/auth/login-password` | 密码登录 |
| `auth.loginByWechat` | POST | `/auth/login-wechat` | 微信登录 |
| `auth.sendCode` | POST | `/auth/send-code` | 发送短信验证码 |
| `auth.logout` | POST | `/auth/logout` | 退出登录 |

#### 用户模块 (`user.*`)

| 方法 | HTTP | 端点 | 用途 |
|------|------|------|------|
| `user.getProfile` | GET | `/user/profile` | 获取用户资料 |
| `user.updateProfile` | POST | `/user/profile/update` | 更新用户资料 |
| `user.hasPassword` | GET | `/user/has-password` | 检查是否设置密码 |
| `user.updatePasswordByOld` | POST | `/user/password/update-by-old` | 通过旧密码修改密码 |
| `user.updatePasswordByCode` | POST | `/user/password/update-by-code` | 通过验证码修改密码 |

#### 账单模块 (`bill.*`)

| 方法 | HTTP | 端点 | 用途 |
|------|------|------|------|
| `bill.getPageBillList` | GET | `/bill/page-list` | 分页查询账单列表 |
| `bill.getBillDetail` | GET | `/bill/detail` | 获取账单详情 |
| `bill.getMonthlyStats` | GET | `/bill/monthly-stats` | 获取月度统计概览 |
| `bill.getCategories` | GET | `/bill/categories` | 获取账单分类列表 |
| `bill.getPaymentMethods` | GET | `/bill/payment-methods` | 获取支付方式列表 |
| `bill.addBill` | POST | `/bill/add` | 添加账单 |
| `bill.batchCreate` | POST | `/bill/batchCreate` | 批量创建账单 |
| `bill.updateBill` | POST | `/bill/update` | 更新账单 |
| `bill.deleteBill` | POST | `/bill/delete` | 删除账单 |

#### 统计模块 (`statistics.*`)

| 方法 | HTTP | 端点 | 用途 |
|------|------|------|------|
| `statistics.getOverview` | GET | `/statistics/overview` | 获取统计概览 |
| `statistics.getCategoryStats` | GET | `/statistics/category` | 获取分类统计数据 |
| `statistics.getDailyTrend` | GET | `/statistics/daily-trend` | 获取每日收支趋势 |
| `statistics.getMembersStats` | GET | `/statistics/members-bill` | 获取成员收支统计 |

#### 家庭模块 (`family.*`)

| 方法 | HTTP | 端点 | 用途 |
|------|------|------|------|
| `family.getMyFamilies` | GET | `/family/my-families` | 获取我的家庭列表 |
| `family.getFamilyDetail` | GET | `/family/detail` | 获取家庭详情 |
| `family.getMembers` | GET | `/family/members` | 获取家庭成员列表 |
| `family.getPendingApplies` | GET | `/family/pending-applies` | 获取待处理加入申请 |
| `family.createFamily` | POST | `/family/create` | 创建家庭 |
| `family.updateFamily` | POST | `/family/update` | 更新家庭信息 |
| `family.deleteFamily` | POST | `/family/delete` | 删除家庭 |
| `family.joinFamily` | POST | `/family/join` | 申请加入家庭 |
| `family.handleJoinApply` | POST | `/family/handle-apply` | 处理加入申请 |
| `family.leaveFamily` | POST | `/family/leave` | 退出家庭 |
| `family.removeMember` | POST | `/family/remove-member` | 移除家庭成员 |
| `family.transferOwner` | POST | `/family/transfer-owner` | 转让户主 |
| `family.refreshInviteCode` | POST | `/family/refresh-invite-code` | 刷新邀请码 |

#### 消息模块 (`message.*`)

| 方法 | HTTP | 端点 | 用途 |
|------|------|------|------|
| `message.getMessageList` | GET | `/message/page-list` | 分页获取消息列表 |
| `message.getUnreadCount` | GET | `/message/unread-count` | 获取未读消息数量 |
| `message.markAsRead` | POST | `/message/mark-read` | 标记消息已读 |
| `message.markAllAsRead` | POST | `/message/mark-all-read` | 全部标记已读 |

#### AI模块 (`ai.*`)

| 方法 | HTTP | 端点 | 用途 |
|------|------|------|------|
| `ai.ocr` | GET | `/ai/ocr` | 图片OCR识别账单 |
| `ai.text` | GET | `/ai/text` | 文本解析账单（语音转文字后的文本） |
| `ai.analysis` | GET | `/ai/analysis` | AI消费分析建议 |

#### 文件模块 (`file.*`)

| 方法 | HTTP | 端点 | 用途 |
|------|------|------|------|
| `file.uploadTempFile` | POST | `/file/temp/upload` | 上传临时文件（OCR用） |
| `file.uploadTempFileToR2` | POST | `/file/upload` | 上传永久文件（头像等） |

### 3.4 请求/响应处理机制

**请求链路**:
1. `beforeRequest` 拦截器：注入 token header、设置 Content-Type、添加 `_t` 时间戳防缓存
2. H5开发环境自动添加 `/api` 前缀（匹配 Vite 代理）
3. Loading 管理器：300ms 延迟显示（防闪烁），支持 `meta.silent` 静默模式
4. 响应拦截：HTTP 401/403 → 清除 token + 弹出登录框；400+ → Toast 错误提示
5. 全局缓存关闭（`cacheFor: null`）

**响应格式**: `Result<T>` — `{ code, message, data, timestamp, success }`

---

## 4. 状态管理（Pinia Stores）

### 4.1 `useUserStore` (id: `user`) — 用户状态

**数据**:
| 字段 | 类型 | 说明 |
|------|------|------|
| `token` | `string` | 认证token |
| `userId` | `string` | 用户ID |
| `nickname` | `string` | 昵称 |
| `phone` | `string` | 手机号 |
| `avatar` | `string` | 头像URL |
| `gender` | `string` | 性别（0-未知/1-男/2-女）|
| `expireTime` | `number` | token过期时间戳 |
| `showLoginPopup` | `boolean` | 是否显示登录弹框 |
| `pendingCallback` | `function` | 登录后待执行的回调 |

**计算属性**: `isLoggedIn` — token存在且未过期

**持久化**: ✅ 自动持久化到 `uni.setStorageSync`

### 4.2 `useBillStore` (id: `bill`) — 账单状态

**数据**:
| 字段 | 类型 | 说明 |
|------|------|------|
| `categoryListMap` | `{expense: CategoryVO[], income: CategoryVO[]}` | 分类列表（按类型分） |
| `paymentMethodList` | `PaymentMethodVO[]` | 支付方式列表 |
| `localBillList` | `LocalBillVO[]` | 本地账单列表（含syncStatus） |
| `isInitialzed` | `boolean` | 是否已初始化 |
| `lastSyncTime` | `string` | 最后同步时间 |

**计算属性**: `hasLocalBills`, `pendingSyncCount`, `syncedCount`

**持久化**: ❌ 排除在持久化之外（`bill` 不在 excludedIds 中但数据较敏感）

> 注：`persist.ts` 中排除的是 `statistics` 和 `message`，`bill` store 会被持久化到本地存储。

### 4.3 `useFamilyStore` (id: `family`) — 家庭状态

**数据**:
| 字段 | 类型 | 说明 |
|------|------|------|
| `familyList` | `FamilyVO[]` | 家庭列表 |
| `currentFamily` | `FamilyVO \| null` | 当前选中家庭 |
| `memberList` | `FamilyMemberVO[]` | 当前家庭成员列表 |
| `pendingApplies` | `FamilyJoinApplyVO[]` | 待处理加入申请 |

**持久化**: ✅ 自动持久化

### 4.4 `useStatisticsStore` (id: `statistics`) — 统计状态

**数据**:
| 字段 | 类型 | 说明 |
|------|------|------|
| `overview` | `BillMonthlyStatsVO` | 月度概览（支出/收入/结余）|
| `categoryData` | `CategoryStatItem[]` | 分类统计数据 |
| `dailyTrend` | `{days, expenses, incomes}` | 每日趋势数据 |
| `aiSuggestion` | `string` | AI建议文本 |
| `aiCached` | `boolean` | AI建议是否来自缓存 |
| `aiRemainingCount` | `number` | AI剩余使用次数 |
| `currentAnalysisType` | `AnalysisType` | 当前分析类型（个人/家庭）|

**持久化**: ❌ 已排除

### 4.5 `useMessageStore` (id: `message`) — 消息状态

**数据**:
| 字段 | 类型 | 说明 |
|------|------|------|
| `unreadCount` | `UnreadCountVO` | 未读消息数（total, familyCount）|
| `messageList` | `MessageVO[]` | 消息列表 |

**持久化**: ❌ 已排除

### 4.6 `useThemeStore` (id: `theme`) — 系统主题

**数据**: `theme`（light/dark）、`themeVars`（CSS变量集合）

**持久化**: ✅ 自动持久化

### 4.7 `useManualThemeStore` (id: `manualTheme`) — 手动主题

**数据**: `theme`、`followSystem`、`hasUserSet`、`currentThemeColor`、`themeVars`

**持久化**: ✅ 自动持久化

### 4.8 持久化机制

`src/store/persist.ts` 中的插件自动将 store 状态保存到 `uni.setStorageSync(store.$id, state)`。排除列表：`['statistics', 'message']`。所有其他 store（user、bill、family、theme、manualTheme）都会被持久化。

---

## 5. 关键功能与UI组件

### 5.1 账单管理

- **列表展示**: 按月分组，支持搜索、筛选、分页加载
- **三种输入方式**: 手动、OCR图片识别、语音识别
- **离线支持**: 未登录时本地记录，登录后批量同步
- **共享功能**: 可将账单关联到家庭

### 5.2 统计分析

- **ECharts图表**: 分类饼图、每日趋势折线图、成员排行图
- **AI消费建议**: 调用阿里云 DashScope 的大模型，支持个人/家庭维度分析，有缓存和次数限制
- **月份选择**: 最近12个月

### 5.3 家庭协作

- **家庭创建/加入**: 邀请码机制（6位数字）
- **权限管理**: 户主 vs 普通成员，户主可审批申请、移除成员、转让户主
- **家庭账单**: 共享账单查看
- **家庭统计**: 集合统计 + 成员排行

### 5.4 主题系统

- **明暗主题**: 跟随系统或手动切换
- **自定义主题色**: 多种预设颜色可选
- **CSS变量**: 通过 `themeVars` 动态注入 wot-design-uni 组件主题

### 5.5 UI框架

- **wot-design-uni**: 主要UI组件库（按钮、弹框、表单、Tabbar等）
- **UnoCSS**: 原子化CSS工具类
- **Lucide Icons**: 图标系统（`i-lucide:*`）
- **ECharts**: 图表可视化
- **自定义Tabbar**: 隐藏原生tabbar，使用 `wd-tabbar` 实现

---

## 6. 敏感数据分析

### 6.1 用户个人信息

| 数据项 | 存储位置 | 敏感等级 | 说明 |
|--------|---------|---------|------|
| `token` | userStore → `uni.setStorageSync('user')` | 🔴 高 | 认证令牌，可冒充用户身份 |
| `userId` | userStore | 🟡 中 | 用户唯一标识 |
| `phone` | userStore | 🔴 高 | 手机号（前端展示时做脱敏 `138****1234`）|
| `nickname` | userStore | 🟢 低 | 用户昵称 |
| `avatar` | userStore | 🟢 低 | 头像URL |
| `gender` | userStore | 🟢 低 | 性别信息 |
| `expireTime` | userStore | 🟡 中 | token过期时间 |

### 6.2 财务数据

| 数据项 | 存储位置 | 敏感等级 | 说明 |
|--------|---------|---------|------|
| 账单金额 | `billStore.localBillList` → 本地存储 | 🔴 高 | 支出/收入金额 |
| 账单名称/备注 | `billStore.localBillList` | 🟡 中 | 可能包含消费描述 |
| 账单分类 | 本地+服务器 | 🟢 低 | 支出类别 |
| 月度统计 | statisticsStore（不持久化） | 🟡 中 | 月度汇总数据 |

### 6.3 家庭数据

| 数据项 | 存储位置 | 敏感等级 | 说明 |
|--------|---------|---------|------|
| 家庭邀请码 | familyStore → 本地存储 | 🔴 高 | 可用于加入家庭 |
| 家庭成员列表 | familyStore | 🟡 中 | 成员信息暴露 |
| 成员头像/昵称 | familyStore | 🟢 低 | 个人信息 |
| 家庭账单数据 | 服务器 | 🔴 高 | 共享财务数据 |

### 6.4 认证凭证

| 数据项 | 存储位置 | 敏感等级 | 说明 |
|--------|---------|---------|------|
| Sa-Token | userStore.token → 本地存储 | 🔴 高 | HTTP请求头 `token` |
| OCR/ASR 固定Token | `OcrEdit.vue` / `VoiceEdit.vue` 硬编码 | 🔴 高 | `LKr82GJOAIwZAN2uPQzls2y2DOzZ05dzzlqikZvMRdlPgdHOpoRNmOUDpfsX3oOX` |
| 文件上传Token | `OcrEdit.vue` 硬编码 | 🔴 高 | 同上固定token用于文件上传 |

### 6.5 安全风险项

1. **硬编码Token**: OCR和语音功能中使用了硬编码的固定token（标记为FIXME），存在泄露风险
2. **本地存储持久化**: userStore（含token、手机号）和 billStore（含财务数据）持久化到 `uni.setStorageSync`，设备被越权访问时可读取
3. **邀请码暴露**: 邀请码持久化到本地存储，且在页面上明文展示（未脱敏）
4. **AI消费分析**: 用户财务数据被发送到AI服务进行分析
5. **文件上传**: 头像和OCR图片上传到 `/file/upload` 和 `/file/temp/upload`

---

## 附录：技术细节

### 自动导入

以下内容在 `.vue` 文件中无需 import 即可使用：
- Vue 3 Composition API（`ref`, `computed`, `watch`, `onLoad`, `onShow` 等）
- Pinia stores（`useBillStore`, `useUserStore`, `useFamilyStore` 等）
- VueUse 工具函数
- Alova hooks（`useRequest` 等）
- 全局工具（`Apis`, `CommonUtil`）
- 全局反馈（`useGlobalToast()`, `useGlobalMessage()`, `useGlobalLoading()`）
- 路由（`useRouter()`, `useRoute()`）

### 条件编译

使用 uni-app 预处理指令进行平台差异处理：
- `// #ifdef H5` — H5平台
- `// #ifdef MP-WEIXIN` — 微信小程序
- `// #ifdef APP` — 原生App
- `// #ifndef H5` — 非H5平台

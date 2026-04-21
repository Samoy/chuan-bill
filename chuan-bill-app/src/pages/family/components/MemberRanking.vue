<script setup lang="ts">
import type { FamilyMemberStatsVO } from '@/api/globals'

const props = defineProps<{
  data: FamilyMemberStatsVO[]
  loading: boolean
}>()

const activeTab = ref(0)

// 支出排行
const expenseRanking = computed(() => {
  return [...props.data]
    .filter(item => Number(item.expense) > 0)
    .sort((a, b) => Number(b.expense) - Number(a.expense))
})

// 收入排行
const incomeRanking = computed(() => {
  return [...props.data]
    .filter(item => Number(item.income) > 0)
    .sort((a, b) => Number(b.income) - Number(a.income))
})

// 排行背景色
function rankBgColor(index: number) {
  if (index === 0)
    return 'bg-yellow-500'
  if (index === 1)
    return 'bg-gray-400'
  if (index === 2)
    return 'bg-orange-400'
  return 'bg-gray-300'
}
</script>

<template>
  <view class="rounded-2xl bg-white p-4 shadow-sm dark:bg-[var(--wot-dark-background2)]">
    <text class="mb-4 block text-base font-500">
      收支排行榜
    </text>

    <!-- 加载中 -->
    <view v-if="loading" class="py-8">
      <wd-skeleton :row="3" animation="gradient" />
    </view>

    <!-- 无数据 -->
    <view v-else-if="data.length === 0" class="py-12 text-center">
      <view class="i-lucide:trophy h-12 w-12 text-gray-300" />
      <text class="mt-2 block text-sm text-gray-400">
        暂无成员数据
      </text>
    </view>

    <template v-else>
      <wd-tabs v-model="activeTab" :slidable-num="2">
        <!-- 支出排行 -->
        <wd-tab title="支出排行">
          <view v-if="expenseRanking.length === 0" class="py-8 text-center">
            <text class="text-sm text-gray-400">
              暂无支出数据
            </text>
          </view>
          <view v-else class="mt-2 space-y-3">
            <view
              v-for="(item, index) in expenseRanking"
              :key="item.userId"
              class="flex items-center gap-3 rounded-lg bg-gray-50 p-3 dark:bg-gray-800"
            >
              <!-- 排名 -->
              <view
                class="h-6 w-6 flex items-center justify-center rounded-full text-xs text-white font-600"
                :class="rankBgColor(index)"
              >
                {{ index + 1 }}
              </view>

              <!-- 头像 -->
              <image
                v-if="item.avatar"
                :src="item.avatar"
                class="h-10 w-10 rounded-full"
                mode="aspectFill"
              />
              <view v-else class="h-10 w-10 flex items-center justify-center rounded-full bg-gray-200">
                <view class="i-lucide:user h-5 w-5 text-gray-400" />
              </view>

              <!-- 信息 -->
              <view class="flex-1">
                <view class="flex items-center gap-2">
                  <text class="text-sm font-500">
                    {{ item.nickname || '未知' }}
                  </text>
                  <view
                    v-if="item.isOwner"
                    class="flex items-center rounded bg-primary/10 px-1.5 py-0.5"
                  >
                    <text class="text-xs text-primary">
                      户主
                    </text>
                  </view>
                </view>
                <text class="text-xs text-gray-400">
                  占比 {{ item.expensePercentage }}%
                </text>
              </view>

              <!-- 金额 -->
              <text class="text-red-400 font-600">
                ¥{{ item.expense }}
              </text>
            </view>
          </view>
        </wd-tab>

        <!-- 收入排行 -->
        <wd-tab title="收入排行">
          <view v-if="incomeRanking.length === 0" class="py-8 text-center">
            <text class="text-sm text-gray-400">
              暂无收入数据
            </text>
          </view>
          <view v-else class="mt-2 space-y-3">
            <view
              v-for="(item, index) in incomeRanking"
              :key="item.userId"
              class="flex items-center gap-3 rounded-lg bg-gray-50 p-3 dark:bg-gray-800"
            >
              <!-- 排名 -->
              <view
                class="h-6 w-6 flex items-center justify-center rounded-full text-xs text-white font-600"
                :class="rankBgColor(index)"
              >
                {{ index + 1 }}
              </view>

              <!-- 头像 -->
              <image
                v-if="item.avatar"
                :src="item.avatar"
                class="h-10 w-10 rounded-full"
                mode="aspectFill"
              />
              <view v-else class="h-10 w-10 flex items-center justify-center rounded-full bg-gray-200">
                <view class="i-lucide:user h-5 w-5 text-gray-400" />
              </view>

              <!-- 信息 -->
              <view class="flex-1">
                <view class="flex items-center gap-2">
                  <text class="text-sm font-500">
                    {{ item.nickname || '未知' }}
                  </text>
                  <view
                    v-if="item.isOwner"
                    class="flex items-center rounded bg-primary/10 px-1.5 py-0.5"
                  >
                    <text class="text-xs text-primary">
                      户主
                    </text>
                  </view>
                </view>
                <text class="text-xs text-gray-400">
                  占比 {{ item.incomePercentage }}%
                </text>
              </view>

              <!-- 金额 -->
              <text class="text-green-500 font-600">
                ¥{{ item.income }}
              </text>
            </view>
          </view>
        </wd-tab>
      </wd-tabs>
    </template>
  </view>
</template>

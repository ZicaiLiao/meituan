<script setup lang="ts">
import { onMounted, ref } from 'vue';

const token = 'demo-admin-4001';
const apiBase = 'http://localhost:8080';

const merchants = ref<any>(null);
const orderSummary = ref<any>(null);
const membershipRules = ref<any>(null);
const recommendationConfigs = ref<any>(null);
const supportConversations = ref<any[]>([]);

async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${apiBase}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    }
  });
  return response.json();
}

async function loadAll() {
  const [merchantData, orderData, memberData, recommendationData, conversationData] = await Promise.all([
    api('/api/admin/merchants'),
    api('/api/admin/orders'),
    api('/api/admin/membership/rules'),
    api('/api/admin/recommendation/configs'),
    api('/api/admin/support/conversations')
  ]);
  merchants.value = merchantData;
  orderSummary.value = orderData;
  membershipRules.value = memberData;
  recommendationConfigs.value = recommendationData;
  supportConversations.value = conversationData as any[];
}

onMounted(loadAll);
</script>

<template>
  <main class="page">
    <section class="hero">
      <div>平台运营后台</div>
      <h1>统一查看履约、营销、会员、推荐与客服</h1>
    </section>

    <section class="grid">
      <div class="panel">
        <h2>商家与店铺</h2>
        <ul>
          <li v-for="merchant in merchants?.merchants ?? []" :key="merchant.id">
            {{ merchant.displayName }} · 角色 {{ merchant.role }}
          </li>
        </ul>
      </div>

      <div class="panel">
        <h2>订单总览</h2>
        <div>GMV：{{ orderSummary?.gmv }}</div>
        <div>已完成：{{ orderSummary?.completedCount }}</div>
        <pre>{{ orderSummary?.statusBreakdown }}</pre>
      </div>

      <div class="panel">
        <h2>会员规则</h2>
        <ul>
          <li v-for="level in membershipRules?.levels ?? []" :key="level">{{ level }}</li>
        </ul>
        <p>{{ membershipRules?.pointRules?.[0] }}</p>
      </div>

      <div class="panel">
        <h2>推荐配置</h2>
        <div>通道：{{ recommendationConfigs?.channels?.join(' / ') }}</div>
        <div>活动加权：{{ recommendationConfigs?.activityBoost }}</div>
        <div>距离加权：{{ recommendationConfigs?.distanceBoost }}</div>
      </div>

      <div class="panel">
        <h2>客服会话</h2>
        <ul>
          <li v-for="conversation in supportConversations" :key="conversation.id">
            {{ conversation.title }} · {{ conversation.scene }}
          </li>
        </ul>
      </div>
    </section>
  </main>
</template>

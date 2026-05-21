<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';

const apiBase = import.meta.env.VITE_API_BASE_URL || '';
const accessToken = ref(localStorage.getItem('admin-token') ?? '');
const adminUser = ref<any>(JSON.parse(localStorage.getItem('admin-user') ?? 'null'));

const authForm = reactive({
  username: 'admin4001',
  password: '123456'
});

const merchants = ref<any>(null);
const orderSummary = ref<any>(null);
const membershipRules = ref<any>(null);
const recommendationConfigs = ref<any>(null);
const supportConversations = ref<any[]>([]);
const coupons = ref<any[]>([]);
const riders = ref<any[]>([]);
const statusText = ref('请先登录平台后台。');

function persistSession(payload: any) {
  accessToken.value = payload.accessToken;
  adminUser.value = payload.user;
  localStorage.setItem('admin-token', payload.accessToken);
  localStorage.setItem('admin-user', JSON.stringify(payload.user));
}

function logout() {
  accessToken.value = '';
  adminUser.value = null;
  localStorage.removeItem('admin-token');
  localStorage.removeItem('admin-user');
}

async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers || {});
  if (!(options.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  if (accessToken.value) {
    headers.set('Authorization', `Bearer ${accessToken.value}`);
  }
  const response = await fetch(`${apiBase}${path}`, { ...options, headers });
  const raw = await response.text();
  const payload = raw ? JSON.parse(raw) : null;
  if (!response.ok) {
    throw new Error(payload?.message ?? raw ?? `HTTP ${response.status}`);
  }
  return payload as T;
}

async function login() {
  const payload = await api<any>('/api/auth/admin/login', {
    method: 'POST',
    body: JSON.stringify(authForm)
  });
  persistSession(payload);
  await loadAll();
}

async function loadAll() {
  const [merchantData, orderData, memberData, recommendationData, conversationData, couponData, riderData] = await Promise.all([
    api('/api/admin/merchants'),
    api('/api/admin/orders'),
    api('/api/admin/membership/rules'),
    api('/api/admin/recommendation/configs'),
    api('/api/admin/support/conversations'),
    api('/api/admin/coupons'),
    api('/api/admin/riders')
  ]);
  merchants.value = merchantData;
  orderSummary.value = orderData;
  membershipRules.value = memberData;
  recommendationConfigs.value = recommendationData;
  supportConversations.value = conversationData as any[];
  coupons.value = couponData as any[];
  riders.value = riderData as any[];
  statusText.value = '后台数据已刷新。';
}

async function rebuildSearch() {
  const result = await api<any>('/api/admin/search/rebuild', { method: 'POST' });
  statusText.value = `索引重建完成：${result.indexName}，店铺 ${result.shopCount}，商品 ${result.productCount}`;
}

onMounted(async () => {
  if (accessToken.value) {
    try {
      await loadAll();
    } catch (error) {
      logout();
      statusText.value = error instanceof Error ? error.message : '后台加载失败';
    }
  }
});
</script>

<template>
  <main class="page">
    <section class="hero">
      <div>平台运营后台</div>
      <h1>{{ adminUser ? adminUser.displayName : '统一查看商家、骑手、订单、优惠券与客服数据' }}</h1>
      <p>{{ statusText }}</p>
      <div v-if="adminUser">
        <button @click="loadAll">刷新</button>
        <button @click="rebuildSearch" style="margin-left:12px;">重建搜索索引</button>
        <button @click="logout" style="margin-left:12px;">退出登录</button>
      </div>
    </section>

    <section v-if="!adminUser" class="panel" style="margin-top: 16px;">
      <h2>管理员登录</h2>
      <input v-model="authForm.username" placeholder="用户名" />
      <input v-model="authForm.password" type="password" placeholder="密码" />
      <div style="margin-top: 12px;">
        <button @click="login">登录后台</button>
      </div>
    </section>

    <section v-if="adminUser" class="grid">
      <div class="panel">
        <h2>商家与店铺</h2>
        <ul>
          <li v-for="merchant in merchants?.merchants ?? []" :key="merchant.id">{{ merchant.displayName }} · {{ merchant.phone }}</li>
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
        <h2>骑手池</h2>
        <ul>
          <li v-for="rider in riders" :key="rider.id">{{ rider.displayName }} · {{ rider.phone }}</li>
        </ul>
      </div>

      <div class="panel">
        <h2>优惠券库</h2>
        <ul>
          <li v-for="coupon in coupons" :key="coupon.id">{{ coupon.title }} · 库存 {{ coupon.stock }}</li>
        </ul>
      </div>

      <div class="panel">
        <h2>客服会话</h2>
        <ul>
          <li v-for="conversation in supportConversations" :key="conversation.id">{{ conversation.title }} · {{ conversation.scene }}</li>
        </ul>
      </div>
    </section>
  </main>
</template>

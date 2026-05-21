<script setup lang="ts">
import { onMounted, ref } from 'vue';

const apiBase = 'http://localhost:8080';
const accessToken = ref('');
const shop = ref<any>(null);
const orders = ref<any[]>([]);
const conversations = ref<any[]>([]);
const statusText = ref('准备连接商家后台...');
const liveEvents = ref<string[]>([]);

function canMerchantHandle(order: any) {
  return order.status === 'PAID_WAITING_MERCHANT';
}

async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${apiBase}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(accessToken.value ? { Authorization: `Bearer ${accessToken.value}` } : {})
    }
  });
  return response.json();
}

async function login() {
  const response = await fetch(`${apiBase}/api/auth/merchant/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'merchant2001' })
  });
  const payload = await response.json();
  accessToken.value = payload.accessToken;
}

async function loadAll() {
  const [shopData, orderData, conversationData] = await Promise.all([
    api<any>('/api/merchant/shop'),
    api<any[]>('/api/merchant/orders'),
    api<any[]>('/api/merchant/chat/conversations')
  ]);
  shop.value = shopData;
  orders.value = orderData;
  conversations.value = conversationData;
  statusText.value = '商家后台已加载，可直接接单或拒单。';
}

async function accept(orderId: number) {
  await api(`/api/merchant/orders/${orderId}/accept`, { method: 'POST' });
  statusText.value = `订单 #${orderId} 已接单并进入骑手抢单池`;
  await loadAll();
}

async function reject(orderId: number) {
  await api(`/api/merchant/orders/${orderId}/reject`, { method: 'POST' });
  statusText.value = `订单 #${orderId} 已拒单`;
  await loadAll();
}

onMounted(async () => {
  await login();
  await loadAll();
  const source = new EventSource(`${apiBase}/api/merchant/stream/events?token=${accessToken.value}`);
  ['connected', 'order.created', 'payment.succeeded', 'rider.accepted', 'delivery.completed'].forEach((eventName) => {
    source.addEventListener(eventName, async (event: MessageEvent) => {
      liveEvents.value = [`${eventName}: ${event.data}`, ...liveEvents.value].slice(0, 6);
      statusText.value = `收到实时事件：${eventName}`;
      await loadAll();
    });
  });
  source.onerror = () => source.close();
});
</script>

<template>
  <main class="page">
    <section class="hero">
      <div>商家工作台</div>
      <h1>{{ shop?.shop?.name ?? '加载中...' }}</h1>
      <p>{{ statusText }}</p>
    </section>

    <section class="grid">
      <div class="panel">
        <h2>店铺概览</h2>
        <p>{{ shop?.shop?.announcement }}</p>
        <div>分类：{{ shop?.shop?.category }}</div>
        <div>评分：{{ shop?.shop?.score }}</div>
        <div>月售：{{ shop?.shop?.monthlySales }}</div>
      </div>

      <div class="panel">
        <h2>聊天协同</h2>
        <div>当前会话数：{{ conversations.length }}</div>
        <p>客服、用户、骑手都可以围绕订单建立会话。</p>
        <ul>
          <li v-for="eventItem in liveEvents" :key="eventItem">{{ eventItem }}</li>
        </ul>
      </div>
    </section>

    <section class="table-card" style="margin-top: 18px;">
      <h2>待处理订单</h2>
      <div v-for="order in orders" :key="order.id" class="row">
        <div>
          <strong>#{{ order.id }}</strong>
          <div>状态：{{ order.status }} · 实付 {{ order.payableAmount }}</div>
        </div>
        <div>
          <button @click="accept(order.id)" :disabled="!canMerchantHandle(order)">接单</button>
          <button class="secondary" @click="reject(order.id)" :disabled="!canMerchantHandle(order)">拒单</button>
        </div>
      </div>
    </section>
  </main>
</template>

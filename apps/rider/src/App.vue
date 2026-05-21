<script setup lang="ts">
import { onMounted, ref } from 'vue';

const apiBase = 'http://localhost:8080';
const accessToken = ref('');
const availableOrders = ref<any[]>([]);
const assignedOrders = ref<any[]>([]);
const conversations = ref<any[]>([]);
const statusText = ref('骑手控制台加载中...');
const liveEvents = ref<string[]>([]);

function canDeliver(order: any) {
  return order.status === 'DELIVERING';
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
  const response = await fetch(`${apiBase}/api/auth/rider/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'rider3001' })
  });
  const payload = await response.json();
  accessToken.value = payload.accessToken;
}

async function loadAll() {
  const [available, mine, conversationData] = await Promise.all([
    api<any[]>('/api/rider/orders/available'),
    api<any[]>('/api/rider/orders/mine'),
    api<any[]>('/api/rider/chat/conversations')
  ]);
  availableOrders.value = available;
  assignedOrders.value = mine;
  conversations.value = conversationData;
  statusText.value = '当前可查看抢单池、配送中订单与相关会话。';
}

async function accept(orderId: number) {
  await api(`/api/rider/orders/${orderId}/accept`, { method: 'POST' });
  statusText.value = `已接单 #${orderId}`;
  await loadAll();
}

async function deliver(orderId: number) {
  await api(`/api/rider/orders/${orderId}/deliver`, { method: 'POST' });
  statusText.value = `订单 #${orderId} 已送达`;
  await loadAll();
}

onMounted(async () => {
  await login();
  await loadAll();
  const source = new EventSource(`${apiBase}/api/rider/stream/events?token=${accessToken.value}`);
  ['connected', 'merchant.accepted'].forEach((eventName) => {
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
      <div>骑手工作台</div>
      <h1>实时履约面板</h1>
      <p>{{ statusText }}</p>
    </section>

    <section class="card">
      <h2>抢单池</h2>
      <div v-for="order in availableOrders" :key="order.id" class="row">
        <div>
          <strong>#{{ order.id }}</strong>
          <div>商家 {{ order.shopId }} · 实付 {{ order.payableAmount }}</div>
        </div>
        <button @click="accept(order.id)">接单</button>
      </div>
    </section>

    <section class="card">
      <h2>配送中订单</h2>
      <div v-for="order in assignedOrders" :key="order.id" class="row">
        <div>
          <strong>#{{ order.id }}</strong>
          <div>状态：{{ order.status }} · 用户 {{ order.userId }} · 实付 {{ order.payableAmount }}</div>
        </div>
        <button @click="deliver(order.id)" :disabled="!canDeliver(order)">标记送达</button>
      </div>
    </section>

    <section class="card">
      <h2>协同会话</h2>
      <div v-for="conversation in conversations" :key="conversation.id" class="row">
        <div>
          <strong>{{ conversation.title }}</strong>
          <div>场景：{{ conversation.scene }} · 订单 {{ conversation.orderId }}</div>
        </div>
      </div>
      <ul>
        <li v-for="eventItem in liveEvents" :key="eventItem">{{ eventItem }}</li>
      </ul>
    </section>
  </main>
</template>

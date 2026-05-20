<script setup lang="ts">
import { onMounted, ref } from 'vue';

const token = 'demo-rider-3001';
const apiBase = 'http://localhost:8080';
const availableOrders = ref<any[]>([]);
const myOrders = ref<any[]>([]);
const statusText = ref('骑手控制台加载中...');
const liveEvents = ref<string[]>([]);

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
  const [available, mine] = await Promise.all([
    api<any[]>('/api/rider/orders/available'),
    api<any[]>('/api/rider/chat/conversations')
  ]);
  availableOrders.value = available;
  myOrders.value = mine;
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
  await loadAll();
  const source = new EventSource(`${apiBase}/api/rider/stream/events?token=${token}`);
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
      <h2>协同会话</h2>
      <div v-for="conversation in myOrders" :key="conversation.id" class="row">
        <div>
          <strong>{{ conversation.title }}</strong>
          <div>场景：{{ conversation.scene }} · 订单 {{ conversation.orderId }}</div>
        </div>
        <button @click="deliver(conversation.orderId)">标记送达</button>
      </div>
      <ul>
        <li v-for="eventItem in liveEvents" :key="eventItem">{{ eventItem }}</li>
      </ul>
    </section>
  </main>
</template>

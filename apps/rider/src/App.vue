<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue';

const apiBase = import.meta.env.VITE_API_BASE_URL || '';
const accessToken = ref(localStorage.getItem('rider-token') ?? '');
const riderUser = ref<any>(JSON.parse(localStorage.getItem('rider-user') ?? 'null'));
const source = ref<EventSource | null>(null);

const authForm = reactive({
  mode: 'login',
  username: 'rider3001',
  password: '123456',
  displayName: '新骑手',
  phone: '',
  email: '',
  vehicleType: '电动车'
});

const availabilityForm = reactive({
  online: true,
  capacity: 3
});

const availableOrders = ref<any[]>([]);
const assignedOrders = ref<any[]>([]);
const conversations = ref<any[]>([]);
const riderProfile = ref<any>(null);
const liveEvents = ref<string[]>([]);
const statusText = ref('请先登录骑手端。');

function persistSession(payload: any) {
  accessToken.value = payload.accessToken;
  riderUser.value = payload.user;
  localStorage.setItem('rider-token', payload.accessToken);
  localStorage.setItem('rider-user', JSON.stringify(payload.user));
}

function logout() {
  accessToken.value = '';
  riderUser.value = null;
  localStorage.removeItem('rider-token');
  localStorage.removeItem('rider-user');
  source.value?.close();
  source.value = null;
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

async function submitAuth() {
  const path = authForm.mode === 'login' ? '/api/auth/rider/login' : '/api/auth/rider/register';
  const payload = await api<any>(path, {
    method: 'POST',
    body: JSON.stringify({
      username: authForm.username,
      password: authForm.password,
      displayName: authForm.displayName,
      phone: authForm.phone,
      email: authForm.email,
      vehicleType: authForm.vehicleType
    })
  });
  persistSession(payload);
  await loadAll();
}

async function loadAll() {
  const [available, mine, conversationData, profile] = await Promise.all([
    api<any[]>('/api/rider/orders/available'),
    api<any[]>('/api/rider/orders/mine'),
    api<any[]>('/api/rider/chat/conversations'),
    api<any>('/api/rider/profile')
  ]);
  availableOrders.value = available;
  assignedOrders.value = mine;
  conversations.value = conversationData;
  riderProfile.value = profile;
  availabilityForm.online = profile.online;
  availabilityForm.capacity = profile.capacity;
  connectStream();
  statusText.value = `欢迎回来，${riderUser.value?.displayName ?? '骑手'}。`;
}

function connectStream() {
  if (!accessToken.value || source.value) {
    return;
  }
  const eventSource = new EventSource(`${apiBase}/api/rider/stream/events?token=${accessToken.value}`);
  source.value = eventSource;
  ['connected', 'merchant.accepted', 'delivery.completed'].forEach((eventName) => {
    eventSource.addEventListener(eventName, async (event: MessageEvent) => {
      liveEvents.value = [`${eventName}: ${event.data}`, ...liveEvents.value].slice(0, 8);
      await loadAll();
    });
  });
  eventSource.onerror = () => {
    eventSource.close();
    source.value = null;
  };
}

async function updateAvailability() {
  const profile = await api<any>('/api/rider/profile/availability', {
    method: 'POST',
    body: JSON.stringify({
      online: availabilityForm.online,
      capacity: Number(availabilityForm.capacity)
    })
  });
  riderProfile.value = profile;
  statusText.value = '接单状态已更新。';
}

async function accept(orderId: number) {
  await api(`/api/rider/orders/${orderId}/accept`, { method: 'POST' });
  statusText.value = `已接单 #${orderId}。`;
  await loadAll();
}

async function deliver(orderId: number) {
  await api(`/api/rider/orders/${orderId}/deliver`, { method: 'POST' });
  statusText.value = `订单 #${orderId} 已送达。`;
  await loadAll();
}

function canDeliver(order: any) {
  return order.status === 'DELIVERING';
}

onMounted(async () => {
  if (accessToken.value) {
    try {
      await loadAll();
    } catch (error) {
      logout();
      statusText.value = error instanceof Error ? error.message : '骑手端加载失败';
    }
  }
});

onBeforeUnmount(() => source.value?.close());
</script>

<template>
  <main class="page">
    <section class="hero">
      <div>骑手工作台</div>
      <h1>{{ riderUser ? riderUser.displayName : '支持真实注册与运力配置' }}</h1>
      <p>{{ statusText }}</p>
      <div v-if="riderUser">
        <button @click="loadAll">刷新</button>
        <button @click="logout" style="background:#93c5fd;color:#102a43;">退出登录</button>
      </div>
    </section>

    <section v-if="!riderUser" class="card">
      <h2>{{ authForm.mode === 'login' ? '骑手登录' : '骑手注册' }}</h2>
      <input v-model="authForm.username" placeholder="用户名" />
      <input v-model="authForm.password" type="password" placeholder="密码" />
      <template v-if="authForm.mode === 'register'">
        <input v-model="authForm.displayName" placeholder="昵称" />
        <input v-model="authForm.phone" placeholder="手机号" />
        <input v-model="authForm.email" placeholder="邮箱" />
        <input v-model="authForm.vehicleType" placeholder="交通工具" />
      </template>
      <div style="margin-top: 12px;">
        <button @click="submitAuth">{{ authForm.mode === 'login' ? '登录' : '注册并入驻' }}</button>
        <button @click="authForm.mode = authForm.mode === 'login' ? 'register' : 'login'" style="background:#93c5fd;color:#102a43;">
          {{ authForm.mode === 'login' ? '切换注册' : '切换登录' }}
        </button>
      </div>
    </section>

    <template v-if="riderUser">
      <section class="card">
        <h2>接单状态</h2>
        <div>车辆类型：{{ riderProfile?.vehicleType }}</div>
        <label><input v-model="availabilityForm.online" type="checkbox" /> 在线接单</label>
        <input v-model="availabilityForm.capacity" type="number" placeholder="最大并行单量" />
        <div style="margin-top: 12px;">
          <button @click="updateAvailability">保存接单状态</button>
        </div>
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
            <div>状态：{{ order.status }} · 用户 {{ order.userId }}</div>
          </div>
          <button @click="deliver(order.id)" :disabled="!canDeliver(order)">标记送达</button>
        </div>
      </section>

      <section class="card">
        <h2>协同会话与事件</h2>
        <div>会话数：{{ conversations.length }}</div>
        <ul>
          <li v-for="eventItem in liveEvents" :key="eventItem">{{ eventItem }}</li>
        </ul>
      </section>
    </template>
  </main>
</template>

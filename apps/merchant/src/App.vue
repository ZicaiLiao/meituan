<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue';

const apiBase = import.meta.env.VITE_API_BASE_URL || '';
const accessToken = ref(localStorage.getItem('merchant-token') ?? '');
const merchantUser = ref<any>(JSON.parse(localStorage.getItem('merchant-user') ?? 'null'));
const source = ref<EventSource | null>(null);

const authForm = reactive({
  mode: 'login',
  username: 'merchant2001',
  password: '123456',
  displayName: '新商家',
  phone: '',
  email: '',
  shopName: '',
  shopCategory: '快餐便当'
});

const shopForm = reactive({
  name: '',
  category: '',
  deliveryFee: 4,
  deliveryMinutes: 30,
  averagePrice: 25,
  minOrderAmount: 20,
  tagsText: '新店开业,品质商家',
  announcement: '',
  serviceModesText: 'DELIVERY,PICKUP',
  status: 'OPEN'
});

const productForm = reactive({
  id: null as number | null,
  name: '',
  category: '',
  price: 0,
  originalPrice: 0,
  stock: 0,
  description: '',
  enabled: true
});

const shop = ref<any>(null);
const products = ref<any[]>([]);
const orders = ref<any[]>([]);
const dashboard = ref<any>(null);
const conversations = ref<any[]>([]);
const liveEvents = ref<string[]>([]);
const statusText = ref('请先登录商家后台。');

function persistSession(payload: any) {
  accessToken.value = payload.accessToken;
  merchantUser.value = payload.user;
  localStorage.setItem('merchant-token', payload.accessToken);
  localStorage.setItem('merchant-user', JSON.stringify(payload.user));
}

function logout() {
  accessToken.value = '';
  merchantUser.value = null;
  localStorage.removeItem('merchant-token');
  localStorage.removeItem('merchant-user');
  source.value?.close();
  source.value = null;
  statusText.value = '已退出商家后台。';
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
  const path = authForm.mode === 'login' ? '/api/auth/merchant/login' : '/api/auth/merchant/register';
  const payload = await api<any>(path, {
    method: 'POST',
    body: JSON.stringify({
      username: authForm.username,
      password: authForm.password,
      displayName: authForm.displayName,
      phone: authForm.phone,
      email: authForm.email,
      shopName: authForm.shopName || authForm.displayName,
      shopCategory: authForm.shopCategory
    })
  });
  persistSession(payload);
  await loadAll();
}

async function loadAll() {
  const [shopData, orderData, productData, dashboardData, conversationData] = await Promise.all([
    api<any>('/api/merchant/shop'),
    api<any[]>('/api/merchant/orders'),
    api<any[]>('/api/merchant/products'),
    api<any>('/api/merchant/dashboard'),
    api<any[]>('/api/merchant/chat/conversations')
  ]);
  shop.value = shopData.shop;
  products.value = productData;
  orders.value = orderData;
  dashboard.value = dashboardData;
  conversations.value = conversationData;
  Object.assign(shopForm, {
    name: shopData.shop.name,
    category: shopData.shop.category,
    deliveryFee: shopData.shop.deliveryFee,
    deliveryMinutes: shopData.shop.deliveryMinutes,
    averagePrice: shopData.shop.averagePrice,
    minOrderAmount: shopData.shop.minOrderAmount,
    tagsText: (shopData.shop.tags || []).join(','),
    announcement: shopData.shop.announcement,
    serviceModesText: (shopData.shop.serviceModes || []).join(','),
    status: shopData.shop.status
  });
  connectStream();
  statusText.value = `欢迎回来，${merchantUser.value?.displayName ?? '商家'}。`;
}

function connectStream() {
  if (!accessToken.value || source.value) {
    return;
  }
  const eventSource = new EventSource(`${apiBase}/api/merchant/stream/events?token=${accessToken.value}`);
  source.value = eventSource;
  ['connected', 'order.created', 'payment.succeeded', 'rider.accepted', 'delivery.completed', 'order.reviewed'].forEach((eventName) => {
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

async function saveShop() {
  await api('/api/merchant/shop', {
    method: 'PUT',
    body: JSON.stringify({
      name: shopForm.name,
      category: shopForm.category,
      deliveryFee: Number(shopForm.deliveryFee),
      deliveryMinutes: Number(shopForm.deliveryMinutes),
      averagePrice: Number(shopForm.averagePrice),
      minOrderAmount: Number(shopForm.minOrderAmount),
      tags: shopForm.tagsText.split(',').map((item) => item.trim()).filter(Boolean),
      announcement: shopForm.announcement,
      serviceModes: shopForm.serviceModesText.split(',').map((item) => item.trim()).filter(Boolean),
      status: shopForm.status
    })
  });
  statusText.value = '店铺信息已更新。';
  await loadAll();
}

function editProduct(product: any) {
  Object.assign(productForm, {
    id: product.id,
    name: product.name,
    category: product.category,
    price: product.price,
    originalPrice: product.originalPrice,
    stock: product.stock,
    description: product.description,
    enabled: product.enabled
  });
}

function resetProductForm() {
  Object.assign(productForm, {
    id: null,
    name: '',
    category: '',
    price: 0,
    originalPrice: 0,
    stock: 0,
    description: '',
    enabled: true
  });
}

async function saveProduct() {
  const payload = {
    name: productForm.name,
    category: productForm.category,
    price: Number(productForm.price),
    originalPrice: Number(productForm.originalPrice),
    stock: Number(productForm.stock),
    description: productForm.description,
    enabled: productForm.enabled
  };
  if (productForm.id) {
    await api(`/api/merchant/products/${productForm.id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    });
    statusText.value = `商品 #${productForm.id} 已更新。`;
  } else {
    await api('/api/merchant/products', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
    statusText.value = '商品已创建。';
  }
  resetProductForm();
  await loadAll();
}

async function deleteProduct(productId: number) {
  await api(`/api/merchant/products/${productId}`, { method: 'DELETE' });
  statusText.value = `商品 #${productId} 已删除。`;
  await loadAll();
}

async function accept(orderId: number) {
  await api(`/api/merchant/orders/${orderId}/accept`, { method: 'POST' });
  statusText.value = `订单 #${orderId} 已接单。`;
  await loadAll();
}

async function reject(orderId: number) {
  await api(`/api/merchant/orders/${orderId}/reject`, { method: 'POST' });
  statusText.value = `订单 #${orderId} 已拒单。`;
  await loadAll();
}

function canMerchantHandle(order: any) {
  return order.status === 'PAID_WAITING_MERCHANT';
}

onMounted(async () => {
  if (accessToken.value) {
    try {
      await loadAll();
    } catch (error) {
      logout();
      statusText.value = error instanceof Error ? error.message : '商家后台加载失败';
    }
  }
});

onBeforeUnmount(() => source.value?.close());
</script>

<template>
  <main class="page">
    <section class="hero">
      <div>商家工作台</div>
      <h1>{{ merchantUser ? merchantUser.displayName : '注册后即可创建并经营自己的店铺' }}</h1>
      <p>{{ statusText }}</p>
      <div v-if="merchantUser">
        <button @click="loadAll">刷新</button>
        <button class="secondary" @click="logout">退出登录</button>
      </div>
    </section>

    <section v-if="!merchantUser" class="panel" style="margin-top: 18px;">
      <h2>{{ authForm.mode === 'login' ? '商家登录' : '商家注册' }}</h2>
      <input v-model="authForm.username" placeholder="用户名" />
      <input v-model="authForm.password" type="password" placeholder="密码" />
      <template v-if="authForm.mode === 'register'">
        <input v-model="authForm.displayName" placeholder="联系人或品牌名" />
        <input v-model="authForm.phone" placeholder="手机号" />
        <input v-model="authForm.email" placeholder="邮箱" />
        <input v-model="authForm.shopName" placeholder="店铺名" />
        <input v-model="authForm.shopCategory" placeholder="店铺类目" />
      </template>
      <div style="margin-top: 12px;">
        <button @click="submitAuth">{{ authForm.mode === 'login' ? '登录' : '注册并开店' }}</button>
        <button class="secondary" @click="authForm.mode = authForm.mode === 'login' ? 'register' : 'login'">
          {{ authForm.mode === 'login' ? '切换注册' : '切换登录' }}
        </button>
      </div>
    </section>

    <template v-if="merchantUser">
      <section class="grid">
        <div class="panel">
          <h2>经营概览</h2>
          <div>订单数：{{ dashboard?.orderCount }}</div>
          <div>待接单：{{ dashboard?.pendingCount }}</div>
          <div>配送中：{{ dashboard?.deliveringCount }}</div>
          <div>已完成：{{ dashboard?.completedCount }}</div>
          <div>GMV：{{ dashboard?.gmv }}</div>
        </div>

        <div class="panel">
          <h2>聊天与事件</h2>
          <div>会话数：{{ conversations.length }}</div>
          <ul>
            <li v-for="eventItem in liveEvents" :key="eventItem">{{ eventItem }}</li>
          </ul>
        </div>
      </section>

      <section class="grid">
        <div class="panel">
          <h2>店铺配置</h2>
          <input v-model="shopForm.name" placeholder="店铺名" />
          <input v-model="shopForm.category" placeholder="店铺类目" />
          <input v-model="shopForm.deliveryFee" type="number" placeholder="配送费" />
          <input v-model="shopForm.deliveryMinutes" type="number" placeholder="配送时长" />
          <input v-model="shopForm.averagePrice" type="number" placeholder="客单价" />
          <input v-model="shopForm.minOrderAmount" type="number" placeholder="起送价" />
          <input v-model="shopForm.tagsText" placeholder="标签，用英文逗号分隔" />
          <input v-model="shopForm.serviceModesText" placeholder="服务方式，例如 DELIVERY,PICKUP" />
          <input v-model="shopForm.status" placeholder="状态，例如 OPEN" />
          <input v-model="shopForm.announcement" placeholder="店铺公告" />
          <div style="margin-top: 12px;">
            <button @click="saveShop">保存店铺配置</button>
          </div>
        </div>

        <div class="panel">
          <h2>{{ productForm.id ? `编辑商品 #${productForm.id}` : '新增商品' }}</h2>
          <input v-model="productForm.name" placeholder="商品名" />
          <input v-model="productForm.category" placeholder="类目" />
          <input v-model="productForm.price" type="number" placeholder="售价" />
          <input v-model="productForm.originalPrice" type="number" placeholder="原价" />
          <input v-model="productForm.stock" type="number" placeholder="库存" />
          <input v-model="productForm.description" placeholder="商品描述" />
          <label><input v-model="productForm.enabled" type="checkbox" /> 上架</label>
          <div style="margin-top: 12px;">
            <button @click="saveProduct">{{ productForm.id ? '更新商品' : '创建商品' }}</button>
            <button class="secondary" @click="resetProductForm">清空表单</button>
          </div>
        </div>
      </section>

      <section class="table-card" style="margin-top: 18px;">
        <h2>商品列表</h2>
        <div v-for="product in products" :key="product.id" class="row">
          <div>
            <strong>{{ product.name }}</strong>
            <div>库存 {{ product.stock }} · 月售 {{ product.monthlySales }} · {{ product.enabled ? '上架中' : '已下架' }}</div>
          </div>
          <div>
            <button @click="editProduct(product)">编辑</button>
            <button class="secondary" @click="deleteProduct(product.id)">删除</button>
          </div>
        </div>
      </section>

      <section class="table-card" style="margin-top: 18px;">
        <h2>订单处理</h2>
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
    </template>
  </main>
</template>

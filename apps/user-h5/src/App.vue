<script setup lang="ts">
import { onMounted, ref } from 'vue';

type Shop = {
  id: number;
  name: string;
  category: string;
  score: number;
  monthlySales: number;
  deliveryFee: number;
  deliveryMinutes: number;
  tags: string[];
};

type Coupon = {
  id: number;
  title: string;
  description: string;
  discountAmount: number;
  minimumSpend: number;
};

type Order = {
  id: number;
  status: string;
  payableAmount: number;
  items: { productName: string; quantity: number }[];
};

type CartItem = {
  productId: number;
  quantity: number;
};

type TimelineItem = {
  orderId: number;
  status: string;
  note: string;
  createdAt: string;
};

const apiBase = 'http://localhost:8080';
const accessToken = ref('');

const shops = ref<Shop[]>([]);
const recommendations = ref<{ guessYouLike: Shop[]; nearbyHot: Shop[]; similarShops: Shop[] } | null>(null);
const coupons = ref<Coupon[]>([]);
const membership = ref<any>(null);
const orders = ref<Order[]>([]);
const conversations = ref<any[]>([]);
const searchKeyword = ref('');
const messageContent = ref('骑手您好，麻烦到楼下给我打电话。');
const statusText = ref('正在加载演示数据...');
const liveEvents = ref<string[]>([]);
const featuredProductByShop = ref<Record<number, number>>({});
const latestTimeline = ref<TimelineItem[]>([]);

async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${apiBase}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(accessToken.value ? { Authorization: `Bearer ${accessToken.value}` } : {}),
      ...(options.headers || {})
    }
  });
  if (!response.ok) {
    throw new Error(await response.text());
  }
  return response.json();
}

async function login() {
  const result = await fetch(`${apiBase}/api/auth/customer/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ username: 'customer1001' })
  });
  const payload = await result.json();
  accessToken.value = payload.accessToken;
}

async function loadAll() {
  const [shopData, recommendationData, couponData, membershipData, orderData, conversationData] = await Promise.all([
    api<Shop[]>('/api/shops'),
    api<any>('/api/recommendations/home'),
    api<Coupon[]>('/api/coupons'),
    api<any>('/api/membership'),
    api<Order[]>('/api/orders'),
    api<any[]>('/api/chat/conversations')
  ]);
  shops.value = shopData;
  recommendations.value = recommendationData;
  coupons.value = couponData;
  membership.value = membershipData;
  orders.value = orderData;
  conversations.value = conversationData;
  const detailResults = await Promise.all(shopData.map((shop) => api<any>(`/api/shops/${shop.id}`)));
  featuredProductByShop.value = Object.fromEntries(
    detailResults
      .map((detail) => [detail.shop.id, detail.products?.[0]?.id])
      .filter((entry) => entry[1] !== undefined)
  );
  if (orderData.length > 0) {
    latestTimeline.value = await api<TimelineItem[]>(`/api/orders/${orderData[0].id}/timeline`);
  }
  statusText.value = '演示数据已就绪，可直接体验加购、下单和聊天。';
}

async function addToCart(productId: number) {
  await api('/api/cart/items', {
    method: 'POST',
    body: JSON.stringify({ productId, quantity: 1 })
  });
  statusText.value = `商品 ${productId} 已加入购物车`;
}

function resolveDefaultProductId(): number | undefined {
  return Object.values(featuredProductByShop.value)[0];
}

async function ensureCartReady() {
  const cartItems = await api<CartItem[]>('/api/cart');
  if (cartItems.length > 0) {
    return;
  }
  // 一键下单要与真实后端规则保持一致，购物车为空时先补入一个热销商品。
  const fallbackProductId = resolveDefaultProductId();
  if (!fallbackProductId) {
    throw new Error('当前没有可下单商品，请先刷新商家数据');
  }
  await addToCart(fallbackProductId);
}

async function createOrderAndPay() {
  try {
    await ensureCartReady();
    const created = await api<Order>('/api/orders', {
      method: 'POST',
      body: JSON.stringify({ couponId: 6101, addressId: 6001 })
    });
    const paid = await api<Order>(`/api/orders/${created.id}/pay`, {
      method: 'POST',
      body: JSON.stringify({ paymentChannel: 'MOCK_PAY' })
    });
    orders.value = [paid, ...orders.value.filter((order) => order.id !== paid.id)];
    latestTimeline.value = await api<TimelineItem[]>(`/api/orders/${paid.id}/timeline`);
    statusText.value = `订单 #${paid.id} 已创建并支付，等待商家接单`;
  } catch (error) {
    statusText.value = error instanceof Error ? error.message : '下单失败，请稍后重试';
  }
}

async function runSearch() {
  const result = await api<any>(`/api/search?q=${encodeURIComponent(searchKeyword.value)}`);
  shops.value = result.shops;
  statusText.value = `搜索完成，返回 ${result.shops.length} 家店铺和 ${result.products.length} 个商品`;
}

async function sendMessage() {
  const targetConversation = conversations.value[0];
  if (!targetConversation) {
    statusText.value = '暂无会话可发送消息';
    return;
  }
  await api('/api/chat/messages', {
    method: 'POST',
    body: JSON.stringify({
      conversationId: targetConversation.id,
      content: messageContent.value,
      type: 'TEXT'
    })
  });
  statusText.value = '消息已发送到后端聊天中心';
}

onMounted(async () => {
  await login();
  await loadAll();
  const source = new EventSource(`${apiBase}/api/stream/events?token=${accessToken.value}`);
  ['connected', 'payment.succeeded', 'merchant.accepted', 'merchant.rejected', 'rider.accepted', 'delivery.completed', 'chat.message']
    .forEach((eventName) => {
      source.addEventListener(eventName, (event: MessageEvent) => {
        liveEvents.value = [`${eventName}: ${event.data}`, ...liveEvents.value].slice(0, 6);
        statusText.value = `收到实时事件：${eventName}`;
      });
    });
  source.onerror = () => {
    source.close();
  };
});
</script>

<template>
  <main class="mobile-shell">
    <section class="hero">
      <div>Hi，李雷</div>
      <h1>外卖、优惠、会员、聊天都已接通</h1>
      <p>这是一套可运行的首版平台演示壳，下面直接对接 Java 后端接口。</p>
      <div class="button-row">
        <button @click="createOrderAndPay">一键下单并支付</button>
        <button class="secondary" @click="loadAll">刷新数据</button>
      </div>
      <div class="status">{{ statusText }}</div>
    </section>

    <section class="section">
      <div class="section-header">
        <h2>搜索入口</h2>
        <span class="section-subtitle">Elasticsearch 风格接口</span>
      </div>
      <div class="panel">
        <input v-model="searchKeyword" placeholder="试试搜索：轻食 / 烧烤 / 川味" />
        <div class="button-row">
          <button @click="runSearch">执行搜索</button>
          <button class="secondary" @click="loadAll">恢复默认推荐</button>
        </div>
      </div>
    </section>

    <section class="section" v-if="recommendations">
      <div class="section-header">
        <h2>猜你喜欢</h2>
        <span class="section-subtitle">多路召回 + 规则重排</span>
      </div>
      <div class="scroll-x">
        <article v-for="shop in recommendations.guessYouLike" :key="shop.id" class="card mini-card">
          <strong>{{ shop.name }}</strong>
          <p>{{ shop.category }} · 评分 {{ shop.score }}</p>
          <div class="chip-strip">
            <span v-for="tag in shop.tags" :key="tag" class="tag">{{ tag }}</span>
          </div>
        </article>
      </div>
    </section>

    <section class="section">
      <div class="section-header">
        <h2>附近商家</h2>
        <span class="section-subtitle">带搜索、排序、推荐权重</span>
      </div>
      <article v-for="shop in shops" :key="shop.id" class="card">
        <strong>{{ shop.name }}</strong>
        <p>{{ shop.category }} · 评分 {{ shop.score }} · 月售 {{ shop.monthlySales }}</p>
        <div>
          <span v-for="tag in shop.tags" :key="tag" class="tag">{{ tag }}</span>
        </div>
        <div class="button-row">
          <button @click="addToCart(featuredProductByShop[shop.id])" :disabled="!featuredProductByShop[shop.id]">加购店铺热销</button>
        </div>
      </article>
    </section>

    <section class="section grid-two">
      <div class="panel">
        <h2>优惠券中心</h2>
        <div v-for="coupon in coupons" :key="coupon.id" class="card">
          <strong>{{ coupon.title }}</strong>
          <p>{{ coupon.description }}</p>
          <div>满 {{ coupon.minimumSpend }} 减 {{ coupon.discountAmount }}</div>
        </div>
      </div>

      <div class="panel" v-if="membership">
        <h2>会员中心</h2>
        <div class="card">
          <strong>{{ membership.level }}</strong>
          <p>成长值 {{ membership.growthPoints }} · 积分 {{ membership.rewardPoints }}</p>
          <ul class="detail-list">
            <li v-for="benefit in membership.benefits" :key="benefit">{{ benefit }}</li>
          </ul>
        </div>
      </div>
    </section>

    <section class="section">
      <div class="section-header">
        <h2>我的订单</h2>
        <span class="section-subtitle">下单后会进入待商家接单流程</span>
      </div>
      <div v-for="order in orders" :key="order.id" class="card">
        <strong>订单 #{{ order.id }}</strong>
        <p>状态 {{ order.status }} · 实付 {{ order.payableAmount }}</p>
        <ul class="detail-list">
          <li v-for="item in order.items" :key="`${order.id}-${item.productName}`" class="order-line">
            {{ item.productName }} x {{ item.quantity }}
          </li>
        </ul>
      </div>
      <div class="card" v-if="latestTimeline.length > 0">
        <strong>最新订单时间线</strong>
        <ul class="detail-list">
          <li v-for="item in latestTimeline" :key="`${item.orderId}-${item.createdAt}-${item.status}`">
            {{ item.status }} · {{ item.note }}
          </li>
        </ul>
      </div>
    </section>

    <section class="section">
      <div class="section-header">
        <h2>即时聊天</h2>
        <span class="section-subtitle">后端消息中心 + Netty 预留接入</span>
      </div>
      <div class="panel">
        <div>当前会话数：{{ conversations.length }}</div>
        <input v-model="messageContent" placeholder="输入要发送给骑手或客服的消息" />
        <div class="button-row">
          <button @click="sendMessage">发送消息</button>
        </div>
      </div>
    </section>

    <section class="section">
      <div class="section-header">
        <h2>实时事件流</h2>
        <span class="section-subtitle">SSE 消费命名事件</span>
      </div>
      <div class="panel">
        <ul class="detail-list">
          <li v-for="eventItem in liveEvents" :key="eventItem">{{ eventItem }}</li>
        </ul>
      </div>
    </section>
  </main>
</template>

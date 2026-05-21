<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue';

type Shop = {
  id: number;
  name: string;
  category: string;
  score: number;
  monthlySales: number;
  deliveryFee: number;
  deliveryMinutes: number;
  tags: string[];
  status: string;
};

type Coupon = {
  id: number;
  title: string;
  description: string;
  discountAmount: number;
  minimumSpend: number;
  shopId?: number | null;
};

type Address = {
  id: number;
  label: string;
  detail: string;
  contactName: string;
  phone: string;
  isDefault: boolean;
};

type Order = {
  id: number;
  status: string;
  payableAmount: number;
  reviewScore?: number | null;
  items: { productId: number; productName: string; quantity: number }[];
};

const apiBase = import.meta.env.VITE_API_BASE_URL || '';
const accessToken = ref(localStorage.getItem('user-h5-token') ?? '');
const currentUser = ref<any>(JSON.parse(localStorage.getItem('user-h5-user') ?? 'null'));
const source = ref<EventSource | null>(null);

const authForm = reactive({
  mode: 'login',
  username: 'customer1001',
  password: '123456',
  displayName: '新用户',
  phone: '',
  email: ''
});

const addressForm = reactive({
  label: '家',
  detail: '',
  contactName: '',
  phone: '',
  isDefault: true
});

const reviewDraft = reactive({
  score: 5,
  content: '口味不错，配送及时。'
});

const searchKeyword = ref('');
const selectedAddressId = ref<number | null>(null);
const selectedCouponId = ref<number | null>(null);
const messageContent = ref('你好，我想咨询一下今天的活动。');
const statusText = ref('请先登录或注册后体验完整功能。');

const shops = ref<Shop[]>([]);
const recommendations = ref<{ guessYouLike: Shop[]; nearbyHot: Shop[]; similarShops: Shop[] } | null>(null);
const ownedCoupons = ref<Coupon[]>([]);
const couponCenter = ref<Coupon[]>([]);
const membership = ref<any>(null);
const orders = ref<Order[]>([]);
const conversations = ref<any[]>([]);
const searchHistory = ref<string[]>([]);
const favoriteShops = ref<Shop[]>([]);
const addresses = ref<Address[]>([]);
const cart = ref<any[]>([]);
const liveEvents = ref<string[]>([]);
const featuredProductByShop = ref<Record<number, number>>({});
const latestTimeline = ref<any[]>([]);

function persistSession(payload: any) {
  accessToken.value = payload.accessToken;
  currentUser.value = payload.user;
  localStorage.setItem('user-h5-token', payload.accessToken);
  localStorage.setItem('user-h5-user', JSON.stringify(payload.user));
}

function logout() {
  accessToken.value = '';
  currentUser.value = null;
  localStorage.removeItem('user-h5-token');
  localStorage.removeItem('user-h5-user');
  source.value?.close();
  source.value = null;
  statusText.value = '已退出登录。';
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
  const payload = await api<any>('/api/auth/customer/login', {
    method: 'POST',
    body: JSON.stringify({ username: authForm.username, password: authForm.password })
  });
  persistSession(payload);
  await loadAll();
}

async function register() {
  const payload = await api<any>('/api/auth/customer/register', {
    method: 'POST',
    body: JSON.stringify({
      username: authForm.username,
      password: authForm.password,
      displayName: authForm.displayName,
      phone: authForm.phone,
      email: authForm.email
    })
  });
  persistSession(payload);
  statusText.value = '注册成功，已自动登录。';
  await loadAll();
}

async function loadAll() {
  const [shopData, recommendationData, userCouponData, couponCenterData, membershipData, orderData, conversationData, favoriteData, historyData, addressData, cartData] =
    await Promise.all([
      api<Shop[]>('/api/shops'),
      api<any>('/api/recommendations/home'),
      api<Coupon[]>('/api/coupons'),
      api<Coupon[]>('/api/coupon-center'),
      api<any>('/api/membership'),
      api<Order[]>('/api/orders'),
      api<any[]>('/api/chat/conversations'),
      api<Shop[]>('/api/favorites'),
      api<string[]>('/api/search/history'),
      api<Address[]>('/api/addresses'),
      api<any[]>('/api/cart')
    ]);

  shops.value = shopData;
  recommendations.value = recommendationData;
  ownedCoupons.value = userCouponData;
  couponCenter.value = couponCenterData;
  membership.value = membershipData;
  orders.value = orderData;
  conversations.value = conversationData;
  favoriteShops.value = favoriteData;
  searchHistory.value = historyData;
  addresses.value = addressData;
  cart.value = cartData;
  selectedAddressId.value = addressData[0]?.id ?? null;
  const detailResults = await Promise.all(shopData.map((shop) => api<any>(`/api/shops/${shop.id}`)));
  featuredProductByShop.value = Object.fromEntries(
    detailResults.map((detail) => [detail.shop.id, detail.products?.[0]?.id]).filter((entry) => entry[1] !== undefined)
  );
  await refreshLatestTimeline();
  connectStream();
  statusText.value = `欢迎回来，${currentUser.value?.displayName ?? '用户'}。`;
}

async function refreshLatestTimeline() {
  if (!orders.value.length) {
    latestTimeline.value = [];
    return;
  }
  latestTimeline.value = await api<any[]>(`/api/orders/${orders.value[0].id}/timeline`);
}

function connectStream() {
  if (!accessToken.value || source.value) {
    return;
  }
  const eventSource = new EventSource(`${apiBase}/api/stream/events?token=${accessToken.value}`);
  source.value = eventSource;
  [
    'connected',
    'payment.succeeded',
    'merchant.accepted',
    'merchant.rejected',
    'rider.accepted',
    'delivery.completed',
    'chat.message',
    'order.cancelled'
  ].forEach((eventName) => {
    eventSource.addEventListener(eventName, async (event: MessageEvent) => {
      liveEvents.value = [`${eventName}: ${event.data}`, ...liveEvents.value].slice(0, 8);
      if (eventName !== 'connected') {
        await loadAll();
      }
    });
  });
  eventSource.onerror = () => {
    eventSource.close();
    source.value = null;
  };
}

async function runSearch() {
  const result = await api<any>(`/api/search?q=${encodeURIComponent(searchKeyword.value)}`);
  shops.value = result.shops;
  statusText.value = `搜索完成，返回 ${result.shops.length} 家店铺。`;
}

async function refreshDefaultView() {
  await loadAll();
  searchKeyword.value = '';
}

async function toggleFavorite(shopId: number, favorite: boolean) {
  await api(`/api/favorites/${shopId}`, { method: favorite ? 'POST' : 'DELETE' });
  await loadAll();
  statusText.value = favorite ? `已收藏店铺 #${shopId}` : `已取消收藏店铺 #${shopId}`;
}

async function claimCoupon(couponId: number) {
  const result = await api<any>(`/api/coupons/${couponId}/claim`, { method: 'POST' });
  statusText.value = result.message;
  await loadAll();
}

async function saveAddress() {
  await api('/api/addresses', {
    method: 'POST',
    body: JSON.stringify(addressForm)
  });
  statusText.value = '地址已保存。';
  await loadAll();
}

async function removeAddress(addressId: number) {
  await api(`/api/addresses/${addressId}`, { method: 'DELETE' });
  statusText.value = `地址 #${addressId} 已删除。`;
  await loadAll();
}

async function addToCart(productId?: number) {
  if (!productId) {
    statusText.value = '当前店铺没有可加购商品。';
    return;
  }
  await api('/api/cart/items', {
    method: 'POST',
    body: JSON.stringify({ productId, quantity: 1 })
  });
  cart.value = await api<any[]>('/api/cart');
  statusText.value = '商品已加入购物车。';
}

async function clearCart() {
  await api('/api/cart', { method: 'DELETE' });
  cart.value = [];
  statusText.value = '购物车已清空。';
}

async function createOrderAndPay() {
  const created = await api<Order>('/api/orders', {
    method: 'POST',
    body: JSON.stringify({
      couponId: selectedCouponId.value,
      addressId: selectedAddressId.value
    })
  });
  const paid = await api<Order>(`/api/orders/${created.id}/pay`, {
    method: 'POST',
    body: JSON.stringify({ paymentChannel: 'MOCK_PAY' })
  });
  statusText.value = `订单 #${paid.id} 已支付，等待商家接单。`;
  await loadAll();
}

async function cancelOrder(orderId: number) {
  await api(`/api/orders/${orderId}/cancel`, { method: 'POST' });
  statusText.value = `订单 #${orderId} 已取消。`;
  await loadAll();
}

async function reviewOrder(orderId: number) {
  await api(`/api/orders/${orderId}/review`, {
    method: 'POST',
    body: JSON.stringify(reviewDraft)
  });
  statusText.value = `订单 #${orderId} 评价成功。`;
  await loadAll();
}

async function ensureConversation() {
  if (conversations.value.length) {
    return conversations.value[0];
  }
  const conversation = await api<any>('/api/chat/conversations', {
    method: 'POST',
    body: JSON.stringify({
      scene: 'CONSULTING',
      title: '用户咨询会话',
      participantIds: [5001]
    })
  });
  conversations.value = [conversation];
  return conversation;
}

async function sendMessage() {
  const conversation = await ensureConversation();
  await api('/api/chat/messages', {
    method: 'POST',
    body: JSON.stringify({
      conversationId: conversation.id,
      content: messageContent.value,
      type: 'TEXT'
    })
  });
  statusText.value = '消息已发送。';
}

function isFavorite(shopId: number) {
  return favoriteShops.value.some((shop) => shop.id === shopId);
}

function canCancel(order: Order) {
  return order.status === 'PENDING_PAYMENT' || order.status === 'PAID_WAITING_MERCHANT';
}

function canReview(order: Order) {
  return order.status === 'COMPLETED' && !order.reviewScore;
}

async function submitAuth() {
  try {
    if (authForm.mode === 'login') {
      await login();
    } else {
      await register();
    }
  } catch (error) {
    statusText.value = error instanceof Error ? error.message : '认证失败';
  }
}

onMounted(async () => {
  if (accessToken.value) {
    try {
      await loadAll();
    } catch (error) {
      logout();
      statusText.value = error instanceof Error ? error.message : '加载失败，请重新登录。';
    }
  }
});

onBeforeUnmount(() => {
  source.value?.close();
});
</script>

<template>
  <main class="mobile-shell">
    <section class="hero">
      <div>{{ currentUser ? `Hi，${currentUser.displayName}` : '欢迎来到外卖中心' }}</div>
      <h1>注册、领券、收藏、下单、评价、聊天都已接入真实后端</h1>
      <p>默认演示账号：`customer1001 / 123456`，也可以直接注册新用户。</p>
      <div class="status">{{ statusText }}</div>
      <div v-if="currentUser" class="button-row">
        <button @click="refreshDefaultView">刷新数据</button>
        <button class="secondary" @click="logout">退出登录</button>
      </div>
    </section>

    <section class="section panel" v-if="!currentUser">
      <h2>{{ authForm.mode === 'login' ? '用户登录' : '用户注册' }}</h2>
      <input v-model="authForm.username" placeholder="用户名" />
      <input v-model="authForm.password" type="password" placeholder="密码（至少 6 位）" />
      <template v-if="authForm.mode === 'register'">
        <input v-model="authForm.displayName" placeholder="昵称" />
        <input v-model="authForm.phone" placeholder="手机号" />
        <input v-model="authForm.email" placeholder="邮箱（可选）" />
      </template>
      <div class="button-row">
        <button @click="submitAuth">{{ authForm.mode === 'login' ? '登录' : '注册并登录' }}</button>
        <button class="secondary" @click="authForm.mode = authForm.mode === 'login' ? 'register' : 'login'">
          {{ authForm.mode === 'login' ? '切换注册' : '切换登录' }}
        </button>
      </div>
    </section>

    <template v-if="currentUser">
      <section class="section">
        <div class="section-header">
          <h2>搜索入口</h2>
          <span class="section-subtitle">搜索词会写入后端历史记录</span>
        </div>
        <div class="panel">
          <input v-model="searchKeyword" placeholder="试试搜索：轻食 / 烧烤 / 川味" />
          <div class="button-row">
            <button @click="runSearch">执行搜索</button>
            <button class="secondary" @click="refreshDefaultView">恢复推荐</button>
          </div>
          <div class="status">历史搜索：{{ searchHistory.join(' / ') || '暂无' }}</div>
        </div>
      </section>

      <section class="section" v-if="recommendations">
        <div class="section-header">
          <h2>猜你喜欢</h2>
          <span class="section-subtitle">收藏店铺会影响推荐排序</span>
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
          <span class="section-subtitle">加购、收藏都是真实 API</span>
        </div>
        <article v-for="shop in shops" :key="shop.id" class="card">
          <strong>{{ shop.name }}</strong>
          <p>{{ shop.category }} · 评分 {{ shop.score }} · 月售 {{ shop.monthlySales }}</p>
          <div>
            <span v-for="tag in shop.tags" :key="tag" class="tag">{{ tag }}</span>
          </div>
          <div class="button-row">
            <button @click="addToCart(featuredProductByShop[shop.id])">加购热销</button>
            <button class="secondary" @click="toggleFavorite(shop.id, !isFavorite(shop.id))">
              {{ isFavorite(shop.id) ? '取消收藏' : '收藏店铺' }}
            </button>
          </div>
        </article>
      </section>

      <section class="section grid-two">
        <div class="panel">
          <h2>优惠券中心</h2>
          <div v-for="coupon in couponCenter" :key="coupon.id" class="card">
            <strong>{{ coupon.title }}</strong>
            <p>{{ coupon.description }}</p>
            <div>满 {{ coupon.minimumSpend }} 减 {{ coupon.discountAmount }}</div>
            <div class="button-row">
              <button @click="claimCoupon(coupon.id)">领取</button>
            </div>
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
          <div class="card">
            <strong>已持有优惠券</strong>
            <ul class="detail-list">
              <li v-for="coupon in ownedCoupons" :key="coupon.id">{{ coupon.title }}</li>
            </ul>
          </div>
        </div>
      </section>

      <section class="section grid-two">
        <div class="panel">
          <h2>地址管理</h2>
          <input v-model="addressForm.label" placeholder="标签" />
          <input v-model="addressForm.detail" placeholder="详细地址" />
          <input v-model="addressForm.contactName" placeholder="联系人" />
          <input v-model="addressForm.phone" placeholder="联系电话" />
          <label><input v-model="addressForm.isDefault" type="checkbox" /> 设为默认地址</label>
          <div class="button-row">
            <button @click="saveAddress">保存地址</button>
          </div>
          <div v-for="address in addresses" :key="address.id" class="card">
            <strong>{{ address.label }} {{ address.isDefault ? '· 默认' : '' }}</strong>
            <p>{{ address.detail }}</p>
            <div class="button-row">
              <button class="secondary" @click="selectedAddressId = address.id">用于下单</button>
              <button class="secondary" @click="removeAddress(address.id)">删除</button>
            </div>
          </div>
        </div>

        <div class="panel">
          <h2>购物车与结算</h2>
          <div class="card">
            <strong>购物车商品数：{{ cart.length }}</strong>
            <p>下单前可选择地址和优惠券。</p>
            <select v-model="selectedAddressId">
              <option :value="null">不指定地址</option>
              <option v-for="address in addresses" :key="address.id" :value="address.id">{{ address.label }}</option>
            </select>
            <select v-model="selectedCouponId">
              <option :value="null">不使用优惠券</option>
              <option v-for="coupon in ownedCoupons" :key="coupon.id" :value="coupon.id">{{ coupon.title }}</option>
            </select>
            <div class="button-row">
              <button @click="createOrderAndPay">下单并支付</button>
              <button class="secondary" @click="clearCart">清空购物车</button>
            </div>
          </div>
        </div>
      </section>

      <section class="section">
        <div class="section-header">
          <h2>我的订单</h2>
          <span class="section-subtitle">支持取消和评价</span>
        </div>
        <div v-for="order in orders" :key="order.id" class="card">
          <strong>订单 #{{ order.id }}</strong>
          <p>状态 {{ order.status }} · 实付 {{ order.payableAmount }}</p>
          <ul class="detail-list">
            <li v-for="item in order.items" :key="`${order.id}-${item.productId}`">{{ item.productName }} x {{ item.quantity }}</li>
          </ul>
          <div class="button-row">
            <button v-if="canCancel(order)" class="secondary" @click="cancelOrder(order.id)">取消订单</button>
            <button v-if="canReview(order)" @click="reviewOrder(order.id)">提交评价</button>
          </div>
        </div>
        <div class="card" v-if="latestTimeline.length">
          <strong>最新订单时间线</strong>
          <ul class="detail-list">
            <li v-for="item in latestTimeline" :key="`${item.orderId}-${item.createdAt}-${item.status}`">{{ item.status }} · {{ item.note }}</li>
          </ul>
        </div>
      </section>

      <section class="section">
        <div class="section-header">
          <h2>即时聊天</h2>
          <span class="section-subtitle">会话不存在时会自动创建客服咨询会话</span>
        </div>
        <div class="panel">
          <div>当前会话数：{{ conversations.length }}</div>
          <input v-model="messageContent" placeholder="输入要发送的咨询内容" />
          <div class="button-row">
            <button @click="sendMessage">发送消息</button>
          </div>
        </div>
      </section>

      <section class="section">
        <div class="section-header">
          <h2>实时事件流</h2>
          <span class="section-subtitle">SSE 消费后端状态推送</span>
        </div>
        <div class="panel">
          <ul class="detail-list">
            <li v-for="eventItem in liveEvents" :key="eventItem">{{ eventItem }}</li>
          </ul>
        </div>
      </section>
    </template>
  </main>
</template>

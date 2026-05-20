package com.meituan.demo.backend.service;

import com.meituan.demo.backend.model.DomainModels.Conversation;
import com.meituan.demo.backend.model.DomainModels.ConversationScene;
import com.meituan.demo.backend.model.DomainModels.Coupon;
import com.meituan.demo.backend.model.DomainModels.MemberProfile;
import com.meituan.demo.backend.model.DomainModels.Message;
import com.meituan.demo.backend.model.DomainModels.MessageType;
import com.meituan.demo.backend.model.DomainModels.Order;
import com.meituan.demo.backend.model.DomainModels.OrderLine;
import com.meituan.demo.backend.model.DomainModels.OrderStatus;
import com.meituan.demo.backend.model.DomainModels.Product;
import com.meituan.demo.backend.model.DomainModels.Role;
import com.meituan.demo.backend.model.DomainModels.Shop;
import com.meituan.demo.backend.model.DomainModels.User;
import com.meituan.demo.backend.model.DomainModels.Address;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class DemoDataStore {

    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final Map<Long, Shop> shops = new ConcurrentHashMap<>();
    private final Map<Long, Product> products = new ConcurrentHashMap<>();
    private final Map<Long, List<Address>> addresses = new ConcurrentHashMap<>();
    private final Map<Long, List<Long>> userCoupons = new ConcurrentHashMap<>();
    private final Map<Long, Coupon> coupons = new ConcurrentHashMap<>();
    private final Map<Long, MemberProfile> memberProfiles = new ConcurrentHashMap<>();
    private final Map<Long, List<com.meituan.demo.backend.model.DomainModels.CartItem>> carts = new ConcurrentHashMap<>();
    private final Map<Long, Order> orders = new ConcurrentHashMap<>();
    private final Map<Long, Conversation> conversations = new ConcurrentHashMap<>();
    private final Map<Long, List<Message>> messagesByConversation = new ConcurrentHashMap<>();
    private final AtomicLong orderIdSequence = new AtomicLong(9000);
    private final AtomicLong conversationIdSequence = new AtomicLong(7000);
    private final AtomicLong messageIdSequence = new AtomicLong(8000);

    @PostConstruct
    public void init() {
        users.put(1001L, new User(1001L, Role.CUSTOMER, "customer1001", "李雷", "13800000001", "金卡", null, true));
        users.put(1002L, new User(1002L, Role.CUSTOMER, "customer1002", "韩梅梅", "13800000002", "普通", null, true));
        users.put(2001L, new User(2001L, Role.MERCHANT, "merchant2001", "川味小馆", "13900000001", null, 3001L, true));
        users.put(2002L, new User(2002L, Role.MERCHANT, "merchant2002", "轻食厨房", "13900000002", null, 3002L, true));
        users.put(3001L, new User(3001L, Role.RIDER, "rider3001", "王师傅", "13700000001", null, null, true));
        users.put(3002L, new User(3002L, Role.RIDER, "rider3002", "张师傅", "13700000002", null, null, true));
        users.put(4001L, new User(4001L, Role.ADMIN, "admin4001", "平台管理员", "13600000001", null, null, true));
        users.put(5001L, new User(5001L, Role.SUPPORT, "support5001", "在线客服", "13500000001", null, null, true));

        shops.put(3001L, new Shop(3001L, 2001L, "川味小馆", "川湘菜", new BigDecimal("4.8"), 2560,
                new BigDecimal("4.0"), 32, new BigDecimal("32"), 1.2, List.of("满30减15", "回头客多"), "招牌口水鸡今日特价"));
        shops.put(3002L, new Shop(3002L, 2002L, "轻食厨房", "轻食沙拉", new BigDecimal("4.7"), 1680,
                new BigDecimal("3.0"), 28, new BigDecimal("28"), 0.9, List.of("低脂健康", "新客立减"), "夏日蛋白碗上新"));
        shops.put(3003L, new Shop(3003L, 2002L, "深夜烧烤铺", "烧烤夜宵", new BigDecimal("4.6"), 3120,
                new BigDecimal("5.0"), 40, new BigDecimal("45"), 2.3, List.of("夜宵热门", "配送快"), "羊肉串第二份半价"));

        seedProducts();
        seedAddresses();
        seedCoupons();
        seedMemberships();
        seedOrdersAndChats();
    }

    private void seedProducts() {
        products.put(5001L, new Product(5001L, 3001L, "口水鸡饭", "招牌主食", new BigDecimal("26"), new BigDecimal("30"), 99, 860,
                "经典麻辣口味，配时蔬"));
        products.put(5002L, new Product(5002L, 3001L, "冒椒肥牛饭", "热销主食", new BigDecimal("29"), new BigDecimal("34"), 88, 740,
                "鲜香麻辣，肥牛量足"));
        products.put(5003L, new Product(5003L, 3001L, "酸梅汤", "饮品", new BigDecimal("6"), new BigDecimal("8"), 160, 520,
                "手工熬制酸甜解腻"));
        products.put(5101L, new Product(5101L, 3002L, "鸡胸肉能量碗", "能量碗", new BigDecimal("24"), new BigDecimal("28"), 120, 670,
                "高蛋白低脂"));
        products.put(5102L, new Product(5102L, 3002L, "牛油果沙拉", "沙拉", new BigDecimal("22"), new BigDecimal("26"), 100, 540,
                "清爽低卡"));
        products.put(5201L, new Product(5201L, 3003L, "羊肉串 10 串", "烧烤", new BigDecimal("38"), new BigDecimal("42"), 80, 930,
                "炭火现烤"));
        products.put(5202L, new Product(5202L, 3003L, "蒜香烤茄子", "烧烤", new BigDecimal("16"), new BigDecimal("18"), 75, 610,
                "宵夜必点"));
    }

    private void seedAddresses() {
        addresses.put(1001L, new ArrayList<>(List.of(
                new Address(6001L, 1001L, "公司", "朝阳区建国路 88 号 A 座 1201", "李雷", "13800000001"),
                new Address(6002L, 1001L, "家", "望京街道花园小区 7 号楼 503", "李雷", "13800000001"))));
        addresses.put(1002L, new ArrayList<>(List.of(
                new Address(6003L, 1002L, "学校", "海淀区学院路 99 号 3 号宿舍", "韩梅梅", "13800000002"))));
    }

    private void seedCoupons() {
        coupons.put(6101L, new Coupon(6101L, "PLATFORM", "平台满30减12", "全平台午餐券", new BigDecimal("12"), new BigDecimal("30"), 999,
                Instant.now().plus(10, ChronoUnit.DAYS), null));
        coupons.put(6102L, new Coupon(6102L, "SHOP", "川味小馆满40减15", "店铺专属券", new BigDecimal("15"), new BigDecimal("40"), 300,
                Instant.now().plus(7, ChronoUnit.DAYS), 3001L));
        coupons.put(6103L, new Coupon(6103L, "SHOP", "轻食厨房运费券", "限轻食厨房使用", new BigDecimal("3"), new BigDecimal("20"), 500,
                Instant.now().plus(5, ChronoUnit.DAYS), 3002L));
        userCoupons.put(1001L, new ArrayList<>(List.of(6101L, 6102L)));
        userCoupons.put(1002L, new ArrayList<>(List.of(6101L, 6103L)));
    }

    private void seedMemberships() {
        memberProfiles.put(1001L, new MemberProfile(1001L, "金卡", 1280, 430,
                List.of("每月专属券包", "积分加速 1.5x", "会员日优先领券"),
                List.of("完成一次评价 +20 成长值", "连续 3 天下单奖励 50 积分")));
        memberProfiles.put(1002L, new MemberProfile(1002L, "普通", 240, 80,
                List.of("新客券包", "节日专属提醒"),
                List.of("下单满 2 次可升级银卡", "邀请好友得 30 积分")));
    }

    private void seedOrdersAndChats() {
        orders.put(9001L, new Order(9001L, 1001L, 3001L, null, OrderStatus.PAID_WAITING_MERCHANT,
                new BigDecimal("55"), new BigDecimal("43"), 6101L, 6001L, Instant.now().minus(20, ChronoUnit.MINUTES),
                List.of(new OrderLine(5001L, "口水鸡饭", 1, new BigDecimal("26")),
                        new OrderLine(5002L, "冒椒肥牛饭", 1, new BigDecimal("29")))));
        orders.put(9002L, new Order(9002L, 1002L, 3002L, 3001L, OrderStatus.DELIVERING,
                new BigDecimal("46"), new BigDecimal("43"), 6103L, 6003L, Instant.now().minus(40, ChronoUnit.MINUTES),
                List.of(new OrderLine(5101L, "鸡胸肉能量碗", 1, new BigDecimal("24")),
                        new OrderLine(5102L, "牛油果沙拉", 1, new BigDecimal("22")))));

        conversations.put(7001L, new Conversation(7001L, ConversationScene.ORDER, 9002L, "订单配送沟通",
                Set.of(1002L, 3001L, 5001L),
                Map.of(1002L, Role.CUSTOMER, 3001L, Role.RIDER, 5001L, Role.SUPPORT)));
        messagesByConversation.put(7001L, new ArrayList<>(List.of(
                new Message(8001L, 7001L, 5001L, Role.SUPPORT, MessageType.SYSTEM, "客服已加入会话，如需帮助可直接留言。", Instant.now().minus(35, ChronoUnit.MINUTES)),
                new Message(8002L, 7001L, 1002L, Role.CUSTOMER, MessageType.TEXT, "请问骑手还有多久到？", Instant.now().minus(10, ChronoUnit.MINUTES)),
                new Message(8003L, 7001L, 3001L, Role.RIDER, MessageType.TEXT, "大约 8 分钟，我已经到小区门口。", Instant.now().minus(8, ChronoUnit.MINUTES)))));
    }

    public Map<Long, User> users() {
        return users;
    }

    public Map<Long, Shop> shops() {
        return shops;
    }

    public Map<Long, Product> products() {
        return products;
    }

    public Map<Long, List<Address>> addresses() {
        return addresses;
    }

    public Map<Long, List<Long>> userCoupons() {
        return userCoupons;
    }

    public Map<Long, Coupon> coupons() {
        return coupons;
    }

    public Map<Long, MemberProfile> memberProfiles() {
        return memberProfiles;
    }

    public Map<Long, List<com.meituan.demo.backend.model.DomainModels.CartItem>> carts() {
        return carts;
    }

    public Map<Long, Order> orders() {
        return orders;
    }

    public Map<Long, Conversation> conversations() {
        return conversations;
    }

    public Map<Long, List<Message>> messagesByConversation() {
        return messagesByConversation;
    }

    public long nextOrderId() {
        return orderIdSequence.incrementAndGet();
    }

    public long nextConversationId() {
        return conversationIdSequence.incrementAndGet();
    }

    public long nextMessageId() {
        return messageIdSequence.incrementAndGet();
    }

    public List<Order> sortedOrders() {
        return orders.values().stream()
                .sorted(Comparator.comparing(Order::createdAt).reversed())
                .toList();
    }
}


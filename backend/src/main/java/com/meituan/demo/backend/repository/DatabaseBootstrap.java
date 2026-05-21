package com.meituan.demo.backend.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseBootstrap implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseBootstrap(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        createTables();
        patchLegacyTables();
        seedIfNeeded();
    }

    private void createTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS users (
                  id BIGINT PRIMARY KEY,
                  role VARCHAR(32) NOT NULL,
                  username VARCHAR(64) NOT NULL,
                  display_name VARCHAR(64) NOT NULL,
                  phone VARCHAR(32),
                  level VARCHAR(32),
                  shop_id BIGINT NULL,
                  active BOOLEAN NOT NULL DEFAULT TRUE,
                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS shops (
                  id BIGINT PRIMARY KEY,
                  merchant_id BIGINT NOT NULL,
                  name VARCHAR(128) NOT NULL,
                  category VARCHAR(64) NOT NULL,
                  score DECIMAL(3,2) NOT NULL,
                  monthly_sales INT NOT NULL,
                  delivery_fee DECIMAL(10,2) NOT NULL,
                  delivery_minutes INT NOT NULL,
                  average_price DECIMAL(10,2) NOT NULL,
                  distance_km DOUBLE NOT NULL,
                  tags VARCHAR(255),
                  announcement VARCHAR(255),
                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS products (
                  id BIGINT PRIMARY KEY,
                  shop_id BIGINT NOT NULL,
                  name VARCHAR(128) NOT NULL,
                  category VARCHAR(64) NOT NULL,
                  price DECIMAL(10,2) NOT NULL,
                  original_price DECIMAL(10,2) NOT NULL,
                  stock INT NOT NULL,
                  monthly_sales INT NOT NULL,
                  description VARCHAR(255),
                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS addresses (
                  id BIGINT PRIMARY KEY,
                  user_id BIGINT NOT NULL,
                  label VARCHAR(32) NOT NULL,
                  detail VARCHAR(255) NOT NULL,
                  contact_name VARCHAR(64) NOT NULL,
                  phone VARCHAR(32) NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS coupons (
                  id BIGINT PRIMARY KEY,
                  scope VARCHAR(32) NOT NULL,
                  title VARCHAR(128) NOT NULL,
                  description VARCHAR(255),
                  discount_amount DECIMAL(10,2) NOT NULL,
                  minimum_spend DECIMAL(10,2) NOT NULL,
                  stock INT NOT NULL,
                  valid_until TIMESTAMP NOT NULL,
                  shop_id BIGINT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_coupons (
                  user_id BIGINT NOT NULL,
                  coupon_id BIGINT NOT NULL,
                  PRIMARY KEY (user_id, coupon_id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS member_profiles (
                  user_id BIGINT PRIMARY KEY,
                  level VARCHAR(32) NOT NULL,
                  growth_points INT NOT NULL,
                  reward_points INT NOT NULL,
                  benefits_text TEXT NOT NULL,
                  tasks_text TEXT NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS carts (
                  user_id BIGINT NOT NULL,
                  product_id BIGINT NOT NULL,
                  quantity INT NOT NULL,
                  PRIMARY KEY (user_id, product_id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS orders (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  user_id BIGINT NOT NULL,
                  shop_id BIGINT NOT NULL,
                  rider_id BIGINT NULL,
                  status VARCHAR(64) NOT NULL,
                  total_amount DECIMAL(10,2) NOT NULL,
                  payable_amount DECIMAL(10,2) NOT NULL,
                  coupon_id BIGINT NULL,
                  address_id BIGINT NULL,
                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS order_items (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  order_id BIGINT NOT NULL,
                  product_id BIGINT NOT NULL,
                  product_name VARCHAR(128) NOT NULL,
                  quantity INT NOT NULL,
                  unit_price DECIMAL(10,2) NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS order_status_logs (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  order_id BIGINT NOT NULL,
                  status VARCHAR(64) NOT NULL,
                  note VARCHAR(255) NOT NULL,
                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS conversations (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  scene VARCHAR(32) NOT NULL,
                  order_id BIGINT NULL,
                  title VARCHAR(128) NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS conversation_participants (
                  conversation_id BIGINT NOT NULL,
                  user_id BIGINT NOT NULL,
                  user_role VARCHAR(32) NOT NULL,
                  PRIMARY KEY (conversation_id, user_id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  conversation_id BIGINT NOT NULL,
                  sender_id BIGINT NOT NULL,
                  sender_role VARCHAR(32) NOT NULL,
                  message_type VARCHAR(32) NOT NULL,
                  content TEXT NOT NULL,
                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS system_configs (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  config_group VARCHAR(64) NOT NULL,
                  config_key VARCHAR(64) NOT NULL,
                  config_value VARCHAR(255) NOT NULL
                )
                """);
    }

    private void seedIfNeeded() {
        if (isEmpty("users")) {
            seedUsers();
        }
        if (isEmpty("shops")) {
            seedShops();
        }
        if (isEmpty("products")) {
            seedProducts();
        }
        if (isEmpty("addresses")) {
            seedAddresses();
        }
        if (isEmpty("coupons")) {
            seedCoupons();
        } else if (isEmpty("user_coupons")) {
            seedUserCouponsOnly();
        }
        if (isEmpty("member_profiles")) {
            seedMembershipProfiles();
        }
        if (isEmpty("system_configs")) {
            seedConfigs();
        }
        if (isEmpty("orders")) {
            seedOrders();
        } else {
            if (isEmpty("order_items")) {
                seedOrderItemsOnly();
            }
            if (isEmpty("order_status_logs")) {
                seedOrderStatusLogsOnly();
            }
        }
        if (isEmpty("conversations")) {
            seedConversations();
        } else {
            if (isEmpty("conversation_participants")) {
                seedConversationParticipantsOnly();
            }
            if (isEmpty("messages")) {
                seedMessagesOnly();
            }
        }
    }

    private boolean isEmpty(String tableName) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
        return count == null || count == 0;
    }

    private void patchLegacyTables() {
        // Keep startup tolerant of older demo schemas so an existing MySQL volume can be upgraded in place.
        safeExecute("ALTER TABLE users ADD COLUMN IF NOT EXISTS display_name VARCHAR(64) NOT NULL DEFAULT ''");
        safeExecute("ALTER TABLE users ADD COLUMN IF NOT EXISTS shop_id BIGINT NULL");
        safeExecute("ALTER TABLE users ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE");

        safeExecute("ALTER TABLE shops ADD COLUMN IF NOT EXISTS merchant_id BIGINT NOT NULL DEFAULT 0");
        safeExecute("ALTER TABLE shops ADD COLUMN IF NOT EXISTS average_price DECIMAL(10,2) NOT NULL DEFAULT 0");
        safeExecute("ALTER TABLE shops ADD COLUMN IF NOT EXISTS distance_km DOUBLE NOT NULL DEFAULT 0");
        safeExecute("ALTER TABLE shops ADD COLUMN IF NOT EXISTS announcement VARCHAR(255) NULL");

        safeExecute("ALTER TABLE products ADD COLUMN IF NOT EXISTS original_price DECIMAL(10,2) NOT NULL DEFAULT 0");
        safeExecute("ALTER TABLE products ADD COLUMN IF NOT EXISTS monthly_sales INT NOT NULL DEFAULT 0");
        safeExecute("ALTER TABLE products ADD COLUMN IF NOT EXISTS description VARCHAR(255) NULL");

        safeExecute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS coupon_id BIGINT NULL");
        safeExecute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS address_id BIGINT NULL");
        safeExecute("ALTER TABLE orders MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT");
        safeExecute("ALTER TABLE order_items MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT");
        safeExecute("ALTER TABLE conversations MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT");
        safeExecute("ALTER TABLE messages MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT");
    }

    private void safeExecute(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ignored) {
            // Engines and existing schemas differ slightly; unsupported patch steps should not block boot.
        }
    }

    private void seedUsers() {
        jdbcTemplate.batchUpdate("""
                INSERT INTO users (id, role, username, display_name, phone, level, shop_id, active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, List.of(
                new Object[] {1001L, "CUSTOMER", "customer1001", "李雷", "13800000001", "金卡", null, true},
                new Object[] {1002L, "CUSTOMER", "customer1002", "韩梅梅", "13800000002", "普通", null, true},
                new Object[] {2001L, "MERCHANT", "merchant2001", "川味小馆", "13900000001", null, 3001L, true},
                new Object[] {2002L, "MERCHANT", "merchant2002", "轻食厨房", "13900000002", null, 3002L, true},
                new Object[] {2003L, "MERCHANT", "merchant2003", "深夜烧烤铺", "13900000003", null, 3003L, true},
                new Object[] {3001L, "RIDER", "rider3001", "王师傅", "13700000001", null, null, true},
                new Object[] {3002L, "RIDER", "rider3002", "张师傅", "13700000002", null, null, true},
                new Object[] {4001L, "ADMIN", "admin4001", "平台管理员", "13600000001", null, null, true},
                new Object[] {5001L, "SUPPORT", "support5001", "在线客服", "13500000001", null, null, true}));
    }

    private void seedShops() {
        jdbcTemplate.batchUpdate("""
                INSERT INTO shops
                (id, merchant_id, name, category, score, monthly_sales, delivery_fee, delivery_minutes, average_price, distance_km, tags, announcement)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, List.of(
                new Object[] {3001L, 2001L, "川味小馆", "川湘菜", new BigDecimal("4.80"), 2560, new BigDecimal("4.00"), 32,
                        new BigDecimal("32.00"), 1.2D, "满30减15,回头客多", "招牌口水鸡今日特价"},
                new Object[] {3002L, 2002L, "轻食厨房", "轻食沙拉", new BigDecimal("4.70"), 1680, new BigDecimal("3.00"), 28,
                        new BigDecimal("28.00"), 0.9D, "低脂健康,新客立减", "夏日蛋白碗上新"},
                new Object[] {3003L, 2003L, "深夜烧烤铺", "烧烤夜宵", new BigDecimal("4.60"), 3120, new BigDecimal("5.00"), 40,
                        new BigDecimal("45.00"), 2.3D, "夜宵热门,配送快", "羊肉串第二份半价"}));
    }

    private void seedProducts() {
        jdbcTemplate.batchUpdate("""
                INSERT INTO products
                (id, shop_id, name, category, price, original_price, stock, monthly_sales, description)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, List.of(
                new Object[] {5001L, 3001L, "口水鸡饭", "招牌主食", new BigDecimal("26.00"), new BigDecimal("30.00"), 99, 860, "经典麻辣口味，配时蔬"},
                new Object[] {5002L, 3001L, "冒椒肥牛饭", "热销主食", new BigDecimal("29.00"), new BigDecimal("34.00"), 88, 740, "鲜香麻辣，肥牛量足"},
                new Object[] {5003L, 3001L, "酸梅汤", "饮品", new BigDecimal("6.00"), new BigDecimal("8.00"), 160, 520, "手工熬制酸甜解腻"},
                new Object[] {5101L, 3002L, "鸡胸肉能量碗", "能量碗", new BigDecimal("24.00"), new BigDecimal("28.00"), 120, 670, "高蛋白低脂"},
                new Object[] {5102L, 3002L, "牛油果沙拉", "沙拉", new BigDecimal("22.00"), new BigDecimal("26.00"), 100, 540, "清爽低卡"},
                new Object[] {5201L, 3003L, "羊肉串 10 串", "烧烤", new BigDecimal("38.00"), new BigDecimal("42.00"), 80, 930, "炭火现烤"},
                new Object[] {5202L, 3003L, "蒜香烤茄子", "烧烤", new BigDecimal("16.00"), new BigDecimal("18.00"), 75, 610, "宵夜必点"}));
    }

    private void seedAddresses() {
        jdbcTemplate.batchUpdate("""
                INSERT INTO addresses (id, user_id, label, detail, contact_name, phone)
                VALUES (?, ?, ?, ?, ?, ?)
                """, List.of(
                new Object[] {6001L, 1001L, "公司", "朝阳区建国路 88 号 A 座 1201", "李雷", "13800000001"},
                new Object[] {6002L, 1001L, "家", "望京街道花园小区 7 号楼 503", "李雷", "13800000001"},
                new Object[] {6003L, 1002L, "学校", "海淀区学院路 99 号 3 号宿舍", "韩梅梅", "13800000002"}));
    }

    private void seedCoupons() {
        Instant now = Instant.now();
        jdbcTemplate.batchUpdate("""
                INSERT INTO coupons
                (id, scope, title, description, discount_amount, minimum_spend, stock, valid_until, shop_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, List.of(
                new Object[] {6101L, "PLATFORM", "平台满30减12", "全平台午餐券", new BigDecimal("12.00"), new BigDecimal("30.00"), 999,
                        Timestamp.from(now.plus(10, ChronoUnit.DAYS)), null},
                new Object[] {6102L, "SHOP", "川味小馆满40减15", "店铺专属券", new BigDecimal("15.00"), new BigDecimal("40.00"), 300,
                        Timestamp.from(now.plus(7, ChronoUnit.DAYS)), 3001L},
                new Object[] {6103L, "SHOP", "轻食厨房运费券", "限轻食厨房使用", new BigDecimal("3.00"), new BigDecimal("20.00"), 500,
                        Timestamp.from(now.plus(5, ChronoUnit.DAYS)), 3002L}));
        seedUserCouponsOnly();
    }

    private void seedUserCouponsOnly() {
        jdbcTemplate.batchUpdate("""
                INSERT INTO user_coupons (user_id, coupon_id) VALUES (?, ?)
                """, List.of(
                new Object[] {1001L, 6101L},
                new Object[] {1001L, 6102L},
                new Object[] {1002L, 6101L},
                new Object[] {1002L, 6103L}));
    }

    private void seedMembershipProfiles() {
        jdbcTemplate.batchUpdate("""
                INSERT INTO member_profiles
                (user_id, level, growth_points, reward_points, benefits_text, tasks_text)
                VALUES (?, ?, ?, ?, ?, ?)
                """, List.of(
                new Object[] {1001L, "金卡", 1280, 430, "每月专属券包|积分加速 1.5x|会员日优先领券", "完成一次评价 +20 成长值|连续 3 天下单奖励 50 积分"},
                new Object[] {1002L, "普通", 240, 80, "新客券包|节日专属提醒", "下单满 2 次可升级银卡|邀请好友得 30 积分"}));
    }

    private void seedConfigs() {
        jdbcTemplate.batchUpdate("""
                INSERT INTO system_configs (config_group, config_key, config_value)
                VALUES (?, ?, ?)
                """, List.of(
                new Object[] {"membership_levels", "normal", "普通"},
                new Object[] {"membership_levels", "silver", "银卡"},
                new Object[] {"membership_levels", "gold", "金卡"},
                new Object[] {"membership_levels", "black", "黑金"},
                new Object[] {"point_rules", "pay", "支付每 1 元获得 1 积分"},
                new Object[] {"point_rules", "review", "评价订单奖励 20 积分"},
                new Object[] {"point_rules", "day", "会员日积分翻倍"},
                new Object[] {"growth_rules", "pay", "支付每 1 元获得 2 成长值"},
                new Object[] {"growth_rules", "active", "连续活跃额外奖励 50 成长值"},
                new Object[] {"growth_rules", "task", "活动任务奖励成长值"},
                new Object[] {"recommendation_channels", "hot", "热门召回"},
                new Object[] {"recommendation_channels", "nearby", "附近召回"},
                new Object[] {"recommendation_channels", "rebuy", "复购召回"},
                new Object[] {"recommendation_channels", "promotion", "活动召回"},
                new Object[] {"recommendation_weights", "activity_boost", "1.2"},
                new Object[] {"recommendation_weights", "distance_boost", "1.1"}));
    }

    private void seedOrders() {
        Timestamp orderOneTime = Timestamp.from(Instant.now().minus(20, ChronoUnit.MINUTES));
        Timestamp orderTwoTime = Timestamp.from(Instant.now().minus(40, ChronoUnit.MINUTES));
        jdbcTemplate.batchUpdate("""
                INSERT INTO orders
                (id, user_id, shop_id, rider_id, status, total_amount, payable_amount, coupon_id, address_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, List.of(
                new Object[] {9001L, 1001L, 3001L, null, "PAID_WAITING_MERCHANT", new BigDecimal("55.00"), new BigDecimal("43.00"), 6101L, 6001L, orderOneTime},
                new Object[] {9002L, 1002L, 3002L, 3001L, "DELIVERING", new BigDecimal("46.00"), new BigDecimal("43.00"), 6103L, 6003L, orderTwoTime}));
        seedOrderItemsOnly();
        seedOrderStatusLogsOnly();
    }

    private void seedOrderItemsOnly() {
        jdbcTemplate.batchUpdate("""
                INSERT INTO order_items
                (order_id, product_id, product_name, quantity, unit_price)
                VALUES (?, ?, ?, ?, ?)
                """, List.of(
                new Object[] {9001L, 5001L, "口水鸡饭", 1, new BigDecimal("26.00")},
                new Object[] {9001L, 5002L, "冒椒肥牛饭", 1, new BigDecimal("29.00")},
                new Object[] {9002L, 5101L, "鸡胸肉能量碗", 1, new BigDecimal("24.00")},
                new Object[] {9002L, 5102L, "牛油果沙拉", 1, new BigDecimal("22.00")}));
    }

    private void seedOrderStatusLogsOnly() {
        Timestamp orderOneTime = Timestamp.from(Instant.now().minus(20, ChronoUnit.MINUTES));
        jdbcTemplate.batchUpdate("""
                INSERT INTO order_status_logs
                (order_id, status, note, created_at)
                VALUES (?, ?, ?, ?)
                """, List.of(
                new Object[] {9001L, "PENDING_PAYMENT", "用户提交订单", Timestamp.from(Instant.now().minus(23, ChronoUnit.MINUTES))},
                new Object[] {9001L, "PAID_WAITING_MERCHANT", "支付成功，等待商家接单", orderOneTime},
                new Object[] {9002L, "PENDING_PAYMENT", "用户提交订单", Timestamp.from(Instant.now().minus(43, ChronoUnit.MINUTES))},
                new Object[] {9002L, "PAID_WAITING_MERCHANT", "支付成功", Timestamp.from(Instant.now().minus(41, ChronoUnit.MINUTES))},
                new Object[] {9002L, "RIDER_PENDING", "商家已接单，等待骑手", Timestamp.from(Instant.now().minus(34, ChronoUnit.MINUTES))},
                new Object[] {9002L, "DELIVERING", "骑手已取餐，配送中", Timestamp.from(Instant.now().minus(16, ChronoUnit.MINUTES))}));
    }

    private void seedConversations() {
        jdbcTemplate.update("""
                INSERT INTO conversations (id, scene, order_id, title)
                VALUES (?, ?, ?, ?)
                """, 7001L, "ORDER", 9002L, "订单配送沟通");
        seedConversationParticipantsOnly();
        seedMessagesOnly();
    }

    private void seedConversationParticipantsOnly() {
        jdbcTemplate.batchUpdate("""
                INSERT INTO conversation_participants (conversation_id, user_id, user_role)
                VALUES (?, ?, ?)
                """, List.of(
                new Object[] {7001L, 1002L, "CUSTOMER"},
                new Object[] {7001L, 3001L, "RIDER"},
                new Object[] {7001L, 5001L, "SUPPORT"}));
    }

    private void seedMessagesOnly() {
        jdbcTemplate.batchUpdate("""
                INSERT INTO messages
                (id, conversation_id, sender_id, sender_role, message_type, content, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, List.of(
                new Object[] {8001L, 7001L, 5001L, "SUPPORT", "SYSTEM", "客服已加入会话，如需帮助可直接留言。",
                        Timestamp.from(Instant.now().minus(35, ChronoUnit.MINUTES))},
                new Object[] {8002L, 7001L, 1002L, "CUSTOMER", "TEXT", "请问骑手还有多久到？",
                        Timestamp.from(Instant.now().minus(10, ChronoUnit.MINUTES))},
                new Object[] {8003L, 7001L, 3001L, "RIDER", "TEXT", "大约 8 分钟，我已经到小区门口。",
                        Timestamp.from(Instant.now().minus(8, ChronoUnit.MINUTES))}));
    }
}

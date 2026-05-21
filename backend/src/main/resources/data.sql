INSERT INTO users (id, role, username, display_name, phone, email, password_hash, level, shop_id, active, avatar_url, created_at, last_login_at)
VALUES
  (1001, 'CUSTOMER', 'customer1001', '李雷', '13800000001', 'customer1001@example.com', '$2y$10$MXx1mauaIEYu4AIh/7zR0.UvOWt2lMc1/ISi7bSHnf3IFFdiIgCce', '金卡', NULL, TRUE, NULL, NOW(), NOW()),
  (1002, 'CUSTOMER', 'customer1002', '韩梅梅', '13800000002', 'customer1002@example.com', '$2y$10$MXx1mauaIEYu4AIh/7zR0.UvOWt2lMc1/ISi7bSHnf3IFFdiIgCce', '普通', NULL, TRUE, NULL, NOW(), NOW()),
  (2001, 'MERCHANT', 'merchant2001', '川味小馆', '13900000001', 'merchant2001@example.com', '$2y$10$MXx1mauaIEYu4AIh/7zR0.UvOWt2lMc1/ISi7bSHnf3IFFdiIgCce', NULL, 3001, TRUE, NULL, NOW(), NOW()),
  (2002, 'MERCHANT', 'merchant2002', '轻食厨房', '13900000002', 'merchant2002@example.com', '$2y$10$MXx1mauaIEYu4AIh/7zR0.UvOWt2lMc1/ISi7bSHnf3IFFdiIgCce', NULL, 3002, TRUE, NULL, NOW(), NOW()),
  (2003, 'MERCHANT', 'merchant2003', '深夜烧烤铺', '13900000003', 'merchant2003@example.com', '$2y$10$MXx1mauaIEYu4AIh/7zR0.UvOWt2lMc1/ISi7bSHnf3IFFdiIgCce', NULL, 3003, TRUE, NULL, NOW(), NOW()),
  (3001, 'RIDER', 'rider3001', '王师傅', '13700000001', 'rider3001@example.com', '$2y$10$MXx1mauaIEYu4AIh/7zR0.UvOWt2lMc1/ISi7bSHnf3IFFdiIgCce', NULL, NULL, TRUE, NULL, NOW(), NOW()),
  (3002, 'RIDER', 'rider3002', '张师傅', '13700000002', 'rider3002@example.com', '$2y$10$MXx1mauaIEYu4AIh/7zR0.UvOWt2lMc1/ISi7bSHnf3IFFdiIgCce', NULL, NULL, TRUE, NULL, NOW(), NOW()),
  (4001, 'ADMIN', 'admin4001', '平台管理员', '13600000001', 'admin4001@example.com', '$2y$10$MXx1mauaIEYu4AIh/7zR0.UvOWt2lMc1/ISi7bSHnf3IFFdiIgCce', NULL, NULL, TRUE, NULL, NOW(), NOW()),
  (5001, 'SUPPORT', 'support5001', '在线客服', '13500000001', 'support5001@example.com', '$2y$10$MXx1mauaIEYu4AIh/7zR0.UvOWt2lMc1/ISi7bSHnf3IFFdiIgCce', NULL, NULL, TRUE, NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  display_name = VALUES(display_name),
  email = VALUES(email),
  password_hash = VALUES(password_hash),
  level = VALUES(level),
  shop_id = VALUES(shop_id),
  active = VALUES(active);

INSERT INTO shops (id, merchant_id, name, category, score, monthly_sales, delivery_fee, delivery_minutes, average_price, distance_km, tags, announcement, status, service_modes, min_order_amount, created_at)
VALUES
  (3001, 2001, '川味小馆', '川湘菜', 4.80, 2560, 4.00, 32, 32.00, 1.2, '满30减15,回头客多,准时达', '招牌口水鸡今日特价', 'OPEN', 'DELIVERY,PICKUP', 20.00, NOW()),
  (3002, 2002, '轻食厨房', '轻食沙拉', 4.70, 1680, 3.00, 28, 28.00, 0.9, '低脂健康,新客立减,减脂推荐', '夏日蛋白碗上新', 'OPEN', 'DELIVERY,PICKUP', 18.00, NOW()),
  (3003, 2003, '深夜烧烤铺', '烧烤夜宵', 4.60, 3120, 5.00, 40, 45.00, 2.3, '夜宵热门,配送快,回头客多', '羊肉串第二份半价', 'OPEN', 'DELIVERY', 25.00, NOW())
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  category = VALUES(category),
  score = VALUES(score),
  monthly_sales = VALUES(monthly_sales),
  delivery_fee = VALUES(delivery_fee),
  delivery_minutes = VALUES(delivery_minutes),
  average_price = VALUES(average_price),
  distance_km = VALUES(distance_km),
  tags = VALUES(tags),
  announcement = VALUES(announcement),
  status = VALUES(status),
  service_modes = VALUES(service_modes),
  min_order_amount = VALUES(min_order_amount);

INSERT INTO products (id, shop_id, name, category, price, original_price, stock, monthly_sales, description, enabled, created_at)
VALUES
  (5001, 3001, '口水鸡饭', '招牌主食', 26.00, 30.00, 99, 860, '经典麻辣口味，配时蔬', TRUE, NOW()),
  (5002, 3001, '冒椒肥牛饭', '热销主食', 29.00, 34.00, 88, 740, '鲜香麻辣，肥牛量足', TRUE, NOW()),
  (5003, 3001, '酸梅汤', '饮品', 6.00, 8.00, 160, 520, '手工熬制酸甜解腻', TRUE, NOW()),
  (5101, 3002, '鸡胸肉能量碗', '能量碗', 24.00, 28.00, 120, 670, '高蛋白低脂', TRUE, NOW()),
  (5102, 3002, '牛油果沙拉', '沙拉', 22.00, 26.00, 100, 540, '清爽低卡', TRUE, NOW()),
  (5201, 3003, '羊肉串 10 串', '烧烤', 38.00, 42.00, 80, 930, '炭火现烤', TRUE, NOW()),
  (5202, 3003, '蒜香烤茄子', '烧烤', 16.00, 18.00, 75, 610, '宵夜必点', TRUE, NOW())
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  price = VALUES(price),
  original_price = VALUES(original_price),
  stock = VALUES(stock),
  monthly_sales = VALUES(monthly_sales),
  description = VALUES(description),
  enabled = VALUES(enabled);

INSERT INTO addresses (id, user_id, label, detail, contact_name, phone, is_default)
VALUES
  (6001, 1001, '公司', '朝阳区建国路 88 号 A 座 1201', '李雷', '13800000001', TRUE),
  (6002, 1001, '家', '望京街道花园小区 7 号楼 503', '李雷', '13800000001', FALSE),
  (6003, 1002, '学校', '海淀区学院路 99 号 3 号宿舍', '韩梅梅', '13800000002', TRUE)
ON DUPLICATE KEY UPDATE
  label = VALUES(label),
  detail = VALUES(detail),
  contact_name = VALUES(contact_name),
  phone = VALUES(phone),
  is_default = VALUES(is_default);

INSERT INTO coupons (id, scope, title, description, discount_amount, minimum_spend, stock, valid_until, shop_id)
VALUES
  (6101, 'PLATFORM', '平台满30减12', '全平台午餐券', 12.00, 30.00, 999, DATE_ADD(NOW(), INTERVAL 10 DAY), NULL),
  (6102, 'SHOP', '川味小馆满40减15', '店铺专属券', 15.00, 40.00, 300, DATE_ADD(NOW(), INTERVAL 7 DAY), 3001),
  (6103, 'SHOP', '轻食厨房运费券', '限轻食厨房使用', 3.00, 20.00, 500, DATE_ADD(NOW(), INTERVAL 5 DAY), 3002)
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  description = VALUES(description),
  discount_amount = VALUES(discount_amount),
  minimum_spend = VALUES(minimum_spend),
  stock = VALUES(stock),
  valid_until = VALUES(valid_until),
  shop_id = VALUES(shop_id);

INSERT INTO user_coupons (user_id, coupon_id, claimed_at)
VALUES
  (1001, 6101, NOW()),
  (1001, 6102, NOW()),
  (1002, 6101, NOW()),
  (1002, 6103, NOW())
ON DUPLICATE KEY UPDATE claimed_at = VALUES(claimed_at);

INSERT INTO member_profiles (user_id, level, growth_points, reward_points, benefits_text, tasks_text)
VALUES
  (1001, '金卡', 1280, 430, '每月专属券包|积分加速 1.5x|会员日优先领券', '完成一次评价 +20 成长值|连续 3 天下单奖励 50 积分'),
  (1002, '普通', 240, 80, '新客券包|节日专属提醒', '下单满 2 次可升级银卡|邀请好友得 30 积分')
ON DUPLICATE KEY UPDATE
  level = VALUES(level),
  growth_points = VALUES(growth_points),
  reward_points = VALUES(reward_points),
  benefits_text = VALUES(benefits_text),
  tasks_text = VALUES(tasks_text);

INSERT INTO system_configs (id, config_group, config_key, config_value)
VALUES
  (1, 'membership_levels', 'normal', '普通'),
  (2, 'membership_levels', 'silver', '银卡'),
  (3, 'membership_levels', 'gold', '金卡'),
  (4, 'membership_levels', 'black', '黑金'),
  (5, 'point_rules', 'pay', '支付每 1 元获得 1 积分'),
  (6, 'point_rules', 'review', '评价订单奖励 20 积分'),
  (7, 'point_rules', 'day', '会员日积分翻倍'),
  (8, 'growth_rules', 'pay', '支付每 1 元获得 2 成长值'),
  (9, 'growth_rules', 'active', '连续活跃额外奖励 50 成长值'),
  (10, 'growth_rules', 'task', '活动任务奖励成长值'),
  (11, 'recommendation_channels', 'hot', '热门召回'),
  (12, 'recommendation_channels', 'nearby', '附近召回'),
  (13, 'recommendation_channels', 'rebuy', '复购召回'),
  (14, 'recommendation_channels', 'promotion', '活动召回'),
  (15, 'recommendation_weights', 'activity_boost', '1.2'),
  (16, 'recommendation_weights', 'distance_boost', '1.1')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

INSERT INTO rider_profiles (user_id, online, capacity, vehicle_type)
VALUES
  (3001, TRUE, 3, '电动车'),
  (3002, TRUE, 2, '摩托车')
ON DUPLICATE KEY UPDATE
  online = VALUES(online),
  capacity = VALUES(capacity),
  vehicle_type = VALUES(vehicle_type);

INSERT INTO orders (id, user_id, shop_id, rider_id, status, total_amount, payable_amount, coupon_id, address_id, created_at, review_score, review_content, reviewed_at)
VALUES
  (9001, 1001, 3001, NULL, 'PAID_WAITING_MERCHANT', 55.00, 43.00, 6101, 6001, DATE_SUB(NOW(), INTERVAL 20 MINUTE), NULL, NULL, NULL),
  (9002, 1002, 3002, 3001, 'DELIVERING', 46.00, 43.00, 6103, 6003, DATE_SUB(NOW(), INTERVAL 40 MINUTE), NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE
  rider_id = VALUES(rider_id),
  status = VALUES(status),
  total_amount = VALUES(total_amount),
  payable_amount = VALUES(payable_amount),
  coupon_id = VALUES(coupon_id),
  address_id = VALUES(address_id);

INSERT INTO order_items (id, order_id, product_id, product_name, quantity, unit_price)
VALUES
  (1, 9001, 5001, '口水鸡饭', 1, 26.00),
  (2, 9001, 5002, '冒椒肥牛饭', 1, 29.00),
  (3, 9002, 5101, '鸡胸肉能量碗', 1, 24.00),
  (4, 9002, 5102, '牛油果沙拉', 1, 22.00)
ON DUPLICATE KEY UPDATE
  quantity = VALUES(quantity),
  unit_price = VALUES(unit_price);

INSERT INTO order_status_logs (id, order_id, status, note, created_at)
VALUES
  (1, 9001, 'PENDING_PAYMENT', '用户提交订单', DATE_SUB(NOW(), INTERVAL 23 MINUTE)),
  (2, 9001, 'PAID_WAITING_MERCHANT', '支付成功，等待商家接单', DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
  (3, 9002, 'PENDING_PAYMENT', '用户提交订单', DATE_SUB(NOW(), INTERVAL 43 MINUTE)),
  (4, 9002, 'PAID_WAITING_MERCHANT', '支付成功', DATE_SUB(NOW(), INTERVAL 41 MINUTE)),
  (5, 9002, 'RIDER_PENDING', '商家已接单，等待骑手', DATE_SUB(NOW(), INTERVAL 34 MINUTE)),
  (6, 9002, 'DELIVERING', '骑手已取餐，配送中', DATE_SUB(NOW(), INTERVAL 16 MINUTE))
ON DUPLICATE KEY UPDATE
  note = VALUES(note),
  created_at = VALUES(created_at);

INSERT INTO conversations (id, scene, order_id, title)
VALUES
  (7001, 'ORDER', 9002, '订单配送沟通')
ON DUPLICATE KEY UPDATE
  scene = VALUES(scene),
  order_id = VALUES(order_id),
  title = VALUES(title);

INSERT INTO conversation_participants (conversation_id, user_id, user_role)
VALUES
  (7001, 1002, 'CUSTOMER'),
  (7001, 3001, 'RIDER'),
  (7001, 5001, 'SUPPORT')
ON DUPLICATE KEY UPDATE user_role = VALUES(user_role);

INSERT INTO messages (id, conversation_id, sender_id, sender_role, message_type, content, created_at)
VALUES
  (8001, 7001, 5001, 'SUPPORT', 'SYSTEM', '客服已加入会话，如需帮助可直接留言。', DATE_SUB(NOW(), INTERVAL 35 MINUTE)),
  (8002, 7001, 1002, 'CUSTOMER', 'TEXT', '请问骑手还有多久到？', DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
  (8003, 7001, 3001, 'RIDER', 'TEXT', '大约 8 分钟，我已经到小区门口。', DATE_SUB(NOW(), INTERVAL 8 MINUTE))
ON DUPLICATE KEY UPDATE
  content = VALUES(content),
  created_at = VALUES(created_at);

INSERT INTO favorite_shops (user_id, shop_id, created_at)
VALUES
  (1001, 3001, NOW()),
  (1001, 3002, NOW())
ON DUPLICATE KEY UPDATE created_at = VALUES(created_at);

INSERT INTO search_history (id, user_id, keyword, created_at)
VALUES
  (1, 1001, '轻食', DATE_SUB(NOW(), INTERVAL 2 DAY)),
  (2, 1001, '烧烤', DATE_SUB(NOW(), INTERVAL 1 DAY)),
  (3, 1002, '川味', DATE_SUB(NOW(), INTERVAL 6 HOUR))
ON DUPLICATE KEY UPDATE
  keyword = VALUES(keyword),
  created_at = VALUES(created_at);

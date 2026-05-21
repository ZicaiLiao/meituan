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
);

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
);

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
);

CREATE TABLE IF NOT EXISTS addresses (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  label VARCHAR(32) NOT NULL,
  detail VARCHAR(255) NOT NULL,
  contact_name VARCHAR(64) NOT NULL,
  phone VARCHAR(32) NOT NULL
);

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
);

CREATE TABLE IF NOT EXISTS user_coupons (
  user_id BIGINT NOT NULL,
  coupon_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, coupon_id)
);

CREATE TABLE IF NOT EXISTS member_profiles (
  user_id BIGINT PRIMARY KEY,
  level VARCHAR(32) NOT NULL,
  growth_points INT NOT NULL,
  reward_points INT NOT NULL,
  benefits_text TEXT NOT NULL,
  tasks_text TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS carts (
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  PRIMARY KEY (user_id, product_id)
);

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
);

CREATE TABLE IF NOT EXISTS order_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  product_name VARCHAR(128) NOT NULL,
  quantity INT NOT NULL,
  unit_price DECIMAL(10,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS order_status_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  status VARCHAR(64) NOT NULL,
  note VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS conversations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  scene VARCHAR(32) NOT NULL,
  order_id BIGINT NULL,
  title VARCHAR(128) NOT NULL
);

CREATE TABLE IF NOT EXISTS conversation_participants (
  conversation_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  user_role VARCHAR(32) NOT NULL,
  PRIMARY KEY (conversation_id, user_id)
);

CREATE TABLE IF NOT EXISTS messages (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  conversation_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  sender_role VARCHAR(32) NOT NULL,
  message_type VARCHAR(32) NOT NULL,
  content TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS system_configs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  config_group VARCHAR(64) NOT NULL,
  config_key VARCHAR(64) NOT NULL,
  config_value VARCHAR(255) NOT NULL
);

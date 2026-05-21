#!/usr/bin/env bash

set -euo pipefail

API="${API_BASE:-http://127.0.0.1:8080}"

login() {
  local role="$1"
  local username="$2"
  local password="$3"
  curl -s -X POST "$API/api/auth/$role/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$username\",\"password\":\"$password\"}" | jq -r '.accessToken'
}

customer_token="$(login customer customer1001 123456)"
merchant_token="$(login merchant merchant2001 123456)"
rider_token="$(login rider rider3001 123456)"
admin_token="$(login admin admin4001 123456)"

coupon_center="$(curl -s "$API/api/coupon-center" -H "Authorization: Bearer $customer_token")"
claim_coupon="$(curl -s -X POST "$API/api/coupons/6103/claim" -H "Authorization: Bearer $customer_token")"

favorite_add="$(curl -s -X POST "$API/api/favorites/3003" -H "Authorization: Bearer $customer_token")"
favorites="$(curl -s "$API/api/favorites" -H "Authorization: Bearer $customer_token")"

search="$(curl -s "$API/api/search?q=%E7%83%A7%E7%83%A4" -H "Authorization: Bearer $customer_token")"
search_history="$(curl -s "$API/api/search/history" -H "Authorization: Bearer $customer_token")"

new_address="$(curl -s -X POST "$API/api/addresses" \
  -H "Authorization: Bearer $customer_token" \
  -H "Content-Type: application/json" \
  -d '{"label":"测试地址","detail":"上海市静安区测试路 8 号","contactName":"李雷","phone":"13800000001","isDefault":false}')"
address_id="$(printf '%s' "$new_address" | jq -r '.id')"

curl -s -X DELETE "$API/api/cart" -H "Authorization: Bearer $customer_token" >/dev/null
curl -s -X POST "$API/api/cart/items" \
  -H "Authorization: Bearer $customer_token" \
  -H "Content-Type: application/json" \
  -d '{"productId":5001,"quantity":1}' >/dev/null

order="$(curl -s -X POST "$API/api/orders" \
  -H "Authorization: Bearer $customer_token" \
  -H "Content-Type: application/json" \
  -d "{\"couponId\":6101,\"addressId\":$address_id}")"
order_id="$(printf '%s' "$order" | jq -r '.id')"

paid="$(curl -s -X POST "$API/api/orders/$order_id/pay" \
  -H "Authorization: Bearer $customer_token" \
  -H "Content-Type: application/json" \
  -d '{"paymentChannel":"MOCK_PAY"}')"

merchant_accept="$(curl -s -X POST "$API/api/merchant/orders/$order_id/accept" \
  -H "Authorization: Bearer $merchant_token")"

availability="$(curl -s -X POST "$API/api/rider/profile/availability" \
  -H "Authorization: Bearer $rider_token" \
  -H "Content-Type: application/json" \
  -d '{"online":true,"capacity":3}')"

available="$(curl -s "$API/api/rider/orders/available" -H "Authorization: Bearer $rider_token")"
rider_accept="$(curl -s -X POST "$API/api/rider/orders/$order_id/accept" -H "Authorization: Bearer $rider_token")"
rider_deliver="$(curl -s -X POST "$API/api/rider/orders/$order_id/deliver" -H "Authorization: Bearer $rider_token")"
review="$(curl -s -X POST "$API/api/orders/$order_id/review" \
  -H "Authorization: Bearer $customer_token" \
  -H "Content-Type: application/json" \
  -d '{"score":5,"content":"联调测试，履约完整。"}')"

timeline="$(curl -s "$API/api/orders/$order_id/timeline" -H "Authorization: Bearer $customer_token")"
membership="$(curl -s "$API/api/membership" -H "Authorization: Bearer $customer_token")"
merchant_dashboard="$(curl -s "$API/api/merchant/dashboard" -H "Authorization: Bearer $merchant_token")"

support_conversation="$(curl -s -X POST "$API/api/chat/conversations" \
  -H "Authorization: Bearer $customer_token" \
  -H "Content-Type: application/json" \
  -d '{"scene":"CONSULTING","title":"联调客服会话","participantIds":[5001]}')"
conversation_id="$(printf '%s' "$support_conversation" | jq -r '.id')"
chat_msg="$(curl -s -X POST "$API/api/chat/messages" \
  -H "Authorization: Bearer $customer_token" \
  -H "Content-Type: application/json" \
  -d "{\"conversationId\":$conversation_id,\"content\":\"联调自测消息\",\"type\":\"TEXT\"}")"
messages="$(curl -s "$API/api/chat/messages?conversationId=$conversation_id" -H "Authorization: Bearer $customer_token")"

admin_orders="$(curl -s "$API/api/admin/orders" -H "Authorization: Bearer $admin_token")"
admin_rebuild="$(curl -s -X POST "$API/api/admin/search/rebuild" -H "Authorization: Bearer $admin_token")"

printf 'coupon_center_count=%s\n' "$(printf '%s' "$coupon_center" | jq 'length')"
printf 'claim_coupon_success=%s\n' "$(printf '%s' "$claim_coupon" | jq -r '.success')"
printf 'favorite_add=%s\n' "$(printf '%s' "$favorite_add" | jq -r '.favorite')"
printf 'favorites_count=%s\n' "$(printf '%s' "$favorites" | jq 'length')"
printf 'search_shop_count=%s\n' "$(printf '%s' "$search" | jq '.shops | length')"
printf 'search_history_count=%s\n' "$(printf '%s' "$search_history" | jq 'length')"
printf 'new_address_id=%s\n' "$address_id"
printf 'order_status_after_create=%s\n' "$(printf '%s' "$order" | jq -r '.status')"
printf 'order_status_after_pay=%s\n' "$(printf '%s' "$paid" | jq -r '.status')"
printf 'merchant_status=%s\n' "$(printf '%s' "$merchant_accept" | jq -r '.status')"
printf 'rider_online=%s\n' "$(printf '%s' "$availability" | jq -r '.online')"
printf 'rider_pool_contains_order=%s\n' "$(printf '%s' "$available" | jq --argjson orderId "$order_id" 'map(select(.id == $orderId)) | length > 0')"
printf 'rider_accept_status=%s\n' "$(printf '%s' "$rider_accept" | jq -r '.status')"
printf 'rider_deliver_status=%s\n' "$(printf '%s' "$rider_deliver" | jq -r '.status')"
printf 'review_score=%s\n' "$(printf '%s' "$review" | jq -r '.reviewScore')"
printf 'timeline_statuses=%s\n' "$(printf '%s' "$timeline" | jq -r 'map(.status) | join(",")')"
printf 'membership_level=%s\n' "$(printf '%s' "$membership" | jq -r '.level')"
printf 'merchant_dashboard_orders=%s\n' "$(printf '%s' "$merchant_dashboard" | jq -r '.orderCount')"
printf 'conversation_id=%s\n' "$conversation_id"
printf 'chat_message_type=%s\n' "$(printf '%s' "$chat_msg" | jq -r '.type')"
printf 'message_count=%s\n' "$(printf '%s' "$messages" | jq 'length')"
printf 'admin_completed_count=%s\n' "$(printf '%s' "$admin_orders" | jq -r '.completedCount')"
printf 'admin_rebuild_success=%s\n' "$(printf '%s' "$admin_rebuild" | jq -r '.success')"

#!/usr/bin/env bash

set -euo pipefail

API="${API_BASE:-http://127.0.0.1:8080}"

customer_token="$(curl -s -X POST "$API/api/auth/customer/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"customer1001"}' | jq -r '.accessToken')"

merchant_token="$(curl -s -X POST "$API/api/auth/merchant/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"merchant2001"}' | jq -r '.accessToken')"

rider_token="$(curl -s -X POST "$API/api/auth/rider/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"rider3001"}' | jq -r '.accessToken')"

admin_token="$(curl -s -X POST "$API/api/auth/admin/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin4001"}' | jq -r '.accessToken')"

shops="$(curl -s "$API/api/shops" -H "Authorization: Bearer $customer_token")"
search="$(curl -s "$API/api/search?q=%E5%B7%9D" -H "Authorization: Bearer $customer_token")"
cart_before="$(curl -s "$API/api/cart" -H "Authorization: Bearer $customer_token")"

curl -s -X POST "$API/api/cart/items" \
  -H "Authorization: Bearer $customer_token" \
  -H "Content-Type: application/json" \
  -d '{"productId":5001,"quantity":1}' >/dev/null

cart_after="$(curl -s "$API/api/cart" -H "Authorization: Bearer $customer_token")"

order="$(curl -s -X POST "$API/api/orders" \
  -H "Authorization: Bearer $customer_token" \
  -H "Content-Type: application/json" \
  -d '{"couponId":6101,"addressId":6001}')"
order_id="$(printf '%s' "$order" | jq -r '.id')"

paid="$(curl -s -X POST "$API/api/orders/$order_id/pay" \
  -H "Authorization: Bearer $customer_token" \
  -H "Content-Type: application/json" \
  -d '{"paymentChannel":"MOCK_PAY"}')"

merchant_accept="$(curl -s -X POST "$API/api/merchant/orders/$order_id/accept" \
  -H "Authorization: Bearer $merchant_token")"

available="$(curl -s "$API/api/rider/orders/available" \
  -H "Authorization: Bearer $rider_token")"

rider_accept="$(curl -s -X POST "$API/api/rider/orders/$order_id/accept" \
  -H "Authorization: Bearer $rider_token")"

rider_deliver="$(curl -s -X POST "$API/api/rider/orders/$order_id/deliver" \
  -H "Authorization: Bearer $rider_token")"

timeline="$(curl -s "$API/api/orders/$order_id/timeline" \
  -H "Authorization: Bearer $customer_token")"

conversations="$(curl -s "$API/api/chat/conversations" \
  -H "Authorization: Bearer $customer_token")"
if [ "$(printf '%s' "$conversations" | jq 'length')" -eq 0 ]; then
  conversations="$(curl -s -X POST "$API/api/chat/conversations" \
    -H "Authorization: Bearer $customer_token" \
    -H "Content-Type: application/json" \
    -d "{\"scene\":\"ORDER\",\"orderId\":$order_id,\"title\":\"订单 #$order_id 配送沟通\",\"participantIds\":[2001]}")"
fi
conversation_id="$(printf '%s' "$conversations" | jq -r 'if type == "array" then .[0].id else .id end')"

chat_msg="$(curl -s -X POST "$API/api/chat/messages" \
  -H "Authorization: Bearer $customer_token" \
  -H "Content-Type: application/json" \
  -d "{\"conversationId\":$conversation_id,\"content\":\"联调自测消息\",\"type\":\"TEXT\"}")"

merchant_conversations="$(curl -s "$API/api/merchant/chat/conversations" \
  -H "Authorization: Bearer $merchant_token")"

messages="$(curl -s "$API/api/chat/messages?conversationId=$conversation_id" \
  -H "Authorization: Bearer $customer_token")"

admin_orders="$(curl -s "$API/api/admin/orders" \
  -H "Authorization: Bearer $admin_token")"

printf 'customer_token=%s\n' "$customer_token"
printf 'shops_count=%s\n' "$(printf '%s' "$shops" | jq 'length')"
printf 'search_shop_count=%s\n' "$(printf '%s' "$search" | jq '.shops | length')"
printf 'cart_before=%s\n' "$cart_before"
printf 'cart_after=%s\n' "$cart_after"
printf 'raw_order=%s\n' "$order"
printf 'raw_paid=%s\n' "$paid"
printf 'raw_merchant_accept=%s\n' "$merchant_accept"
printf 'raw_available=%s\n' "$available"
printf 'raw_rider_accept=%s\n' "$rider_accept"
printf 'raw_rider_deliver=%s\n' "$rider_deliver"
printf 'raw_timeline=%s\n' "$timeline"
printf 'raw_conversations=%s\n' "$conversations"
printf 'raw_chat_msg=%s\n' "$chat_msg"
printf 'raw_merchant_conversations=%s\n' "$merchant_conversations"
printf 'raw_messages=%s\n' "$messages"
printf 'raw_admin_orders=%s\n' "$admin_orders"
printf 'order_id=%s\n' "$order_id"
printf 'order_status_after_create=%s\n' "$(printf '%s' "$order" | jq -r '.status')"
printf 'order_status_after_pay=%s\n' "$(printf '%s' "$paid" | jq -r '.status')"
printf 'merchant_status=%s\n' "$(printf '%s' "$merchant_accept" | jq -r '.status')"
printf 'rider_pool_contains_order=%s\n' "$(printf '%s' "$available" | jq --argjson orderId "$order_id" 'map(select(.id == $orderId)) | length > 0')"
printf 'rider_accept_status=%s\n' "$(printf '%s' "$rider_accept" | jq -r '.status')"
printf 'rider_deliver_status=%s\n' "$(printf '%s' "$rider_deliver" | jq -r '.status')"
printf 'timeline_statuses=%s\n' "$(printf '%s' "$timeline" | jq -r 'map(.status) | join(",")')"
printf 'conversation_count=%s\n' "$(printf '%s' "$conversations" | jq 'if type == "array" then length else 1 end')"
printf 'chat_message_type=%s\n' "$(printf '%s' "$chat_msg" | jq -r '.type')"
printf 'message_count=%s\n' "$(printf '%s' "$messages" | jq 'length')"
printf 'merchant_conversation_count=%s\n' "$(printf '%s' "$merchant_conversations" | jq 'length')"
printf 'admin_completed_count=%s\n' "$(printf '%s' "$admin_orders" | jq -r '.completedCount')"
printf 'admin_status_keys=%s\n' "$(printf '%s' "$admin_orders" | jq -r '.statusBreakdown | keys | join(",")')"

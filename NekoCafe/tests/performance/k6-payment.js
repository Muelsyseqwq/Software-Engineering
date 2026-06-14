// k6 performance test: Sandbox payment
// Usage: k6 run --env BASE_URL=http://localhost:8080 k6-payment.js

import http from 'k6/http'
import { check, sleep } from 'k6'

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'

export const options = {
  vus: 50,
  duration: '30s',
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.05'],
  },
}

function getToken(vuIndex) {
  const payload = JSON.stringify({
    account: `demo_customer_bj_${(vuIndex % 2) + 1}`,
    password: 'demo123456',
  })
  const res = http.post(`${BASE_URL}/api/auth/login`, payload, {
    headers: { 'Content-Type': 'application/json' },
  })
  if (res.status === 200) {
    try {
      const body = JSON.parse(res.body)
      return body.data?.token || ''
    } catch { return '' }
  }
  return ''
}

export default function () {
  const token = getToken(__VU)
  if (!token) {
    sleep(1)
    return
  }

  const storeId = (__VU % 3) + 1

  // Create order first
  const orderPayload = JSON.stringify({
    storeId: storeId,
    items: [{ dishId: 1, quantity: 1 }],
    remark: `k6-pay-${__VU}`,
  })
  const orderRes = http.post(`${BASE_URL}/api/order`, orderPayload, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  })

  let orderId = null
  try {
    const body = JSON.parse(orderRes.body)
    if (body.data && body.data.id) {
      orderId = body.data.id
    }
  } catch { /* ignore */ }

  if (!orderId) {
    sleep(1)
    return
  }

  // Pay with idempotency key
  const idempotencyKey = `k6-pay-${__VU}-${Date.now()}`
  const payPayload = JSON.stringify({
    orderId: orderId,
    idempotencyKey: idempotencyKey,
  })

  const payRes = http.post(`${BASE_URL}/api/payment/sandbox`, payPayload, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  })

  check(payRes, {
    'payment status 200': (r) => r.status === 200,
    'payment success': (r) => {
      try {
        const body = JSON.parse(r.body)
        return body.code === 0 && body.data?.status === 'SUCCESS'
      } catch { return false }
    },
  })

  sleep(1)
}

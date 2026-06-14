// k6 performance test: Create order
// Usage: k6 run --env BASE_URL=http://localhost:8080 k6-order-create.js

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

  const payload = JSON.stringify({
    storeId: storeId,
    items: [
      { dishId: 1, quantity: 1 },
      { dishId: 2, quantity: 1 },
    ],
    remark: `k6压测订单-${__VU}`,
  })

  const res = http.post(`${BASE_URL}/api/order`, payload, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  })

  check(res, {
    'order create status 200': (r) => r.status === 200,
    'order created': (r) => {
      try {
        const body = JSON.parse(r.body)
        return body.code === 0
      } catch { return false }
    },
  })

  sleep(1)
}

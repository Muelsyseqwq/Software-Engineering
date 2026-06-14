// k6 performance test: Create reservation
// Usage: k6 run --env BASE_URL=http://localhost:8080 k6-reservation-create.js

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

// Pre-login to get tokens (simplified — in real scenarios use setup stage)
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

  const today = new Date().toISOString().slice(0, 10)
  const storeId = (__VU % 3) + 1

  // First get available slots
  const slotsRes = http.get(`${BASE_URL}/api/store/${storeId}/reservation-slots`, {
    params: { date: today, partySize: 2 },
    headers: { 'Content-Type': 'application/json' },
  })

  let slotId = null
  let tableId = null
  try {
    const body = JSON.parse(slotsRes.body)
    if (body.data && body.data.length > 0) {
      slotId = body.data[0].id
      tableId = body.data[0].tableId
    }
  } catch { /* ignore parse error */ }

  if (!slotId) {
    sleep(1)
    return
  }

  const payload = JSON.stringify({
    storeId: storeId,
    tableId: tableId,
    slotId: slotId,
    partySize: 2,
    contactName: `测试用户${__VU}`,
    contactPhone: `1380000${String(__VU).padStart(4, '0')}`,
  })

  const res = http.post(`${BASE_URL}/api/reservation`, payload, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  })

  check(res, {
    'reservation create status 200': (r) => r.status === 200,
    'reservation created': (r) => {
      try {
        const body = JSON.parse(r.body)
        return body.code === 0 || body.code === 2105 // 2105 = slot full, acceptable
      } catch { return false }
    },
  })

  sleep(1)
}

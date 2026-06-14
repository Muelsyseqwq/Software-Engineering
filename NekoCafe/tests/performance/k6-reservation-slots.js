// k6 performance test: Reservation slots query
// Usage: k6 run --env BASE_URL=http://localhost:8080 k6-reservation-slots.js

import http from 'k6/http'
import { check, sleep } from 'k6'

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'

export const options = {
  vus: 80,
  duration: '30s',
  thresholds: {
    http_req_duration: ['p(95)<350'],
    http_req_failed: ['rate<0.02'],
  },
}

export default function () {
  // Rotate across stores 1-3
  const storeId = (__VU % 3) + 1
  const today = new Date().toISOString().slice(0, 10)

  const res = http.get(`${BASE_URL}/api/store/${storeId}/reservation-slots`, {
    params: { date: today, partySize: 2 },
    headers: { 'Content-Type': 'application/json' },
  })

  check(res, {
    'slots status 200': (r) => r.status === 200,
    'slots response ok': (r) => {
      try {
        const body = JSON.parse(r.body)
        return body.code === 0
      } catch { return false }
    },
  })

  sleep(0.5)
}

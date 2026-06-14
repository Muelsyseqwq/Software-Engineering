// k6 performance test: Store list endpoint
// Usage: k6 run --env BASE_URL=http://localhost:8080 k6-store-list.js

import http from 'k6/http'
import { check, sleep } from 'k6'

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'

export const options = {
  vus: 100,
  duration: '30s',
  thresholds: {
    http_req_duration: ['p(95)<350'],
    http_req_failed: ['rate<0.01'],
  },
}

export default function () {
  const res = http.get(`${BASE_URL}/api/store`, {
    headers: { 'Content-Type': 'application/json' },
  })

  check(res, {
    'store list status 200': (r) => r.status === 200,
    'store list has data': (r) => {
      try {
        const body = JSON.parse(r.body)
        return body.code === 0 && Array.isArray(body.data)
      } catch { return false }
    },
  })

  sleep(0.5)
}

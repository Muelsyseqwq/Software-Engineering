// k6 performance test: Login endpoint
// Usage: k6 run --env BASE_URL=http://localhost:8080 k6-login.js

import http from 'k6/http'
import { check, sleep } from 'k6'

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'

export const options = {
  vus: 50,
  duration: '30s',
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
}

export default function () {
  const payload = JSON.stringify({
    account: `demo_customer_bj_${__VU % 2 + 1}`,
    password: 'demo123456',
  })

  const res = http.post(`${BASE_URL}/api/auth/login`, payload, {
    headers: { 'Content-Type': 'application/json' },
  })

  check(res, {
    'login status 200': (r) => r.status === 200,
    'login has token': (r) => {
      try {
        const body = JSON.parse(r.body)
        return body.code === 0 && body.data && body.data.token
      } catch { return false }
    },
  })

  sleep(1)
}

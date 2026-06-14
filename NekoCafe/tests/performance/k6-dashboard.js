// k6 performance test: Dashboard / metrics APIs
// Usage: k6 run --env BASE_URL=http://localhost:8080 k6-dashboard.js

import http from 'k6/http'
import { check, sleep, group } from 'k6'

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'

export const options = {
  vus: 30,
  duration: '30s',
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.05'],
  },
}

function getManagerToken() {
  const payload = JSON.stringify({
    account: 'demo_manager_bj_1',
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
  const token = getManagerToken()
  if (!token) {
    sleep(1)
    return
  }

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  }

  group('dashboard metrics', () => {
    // Store metrics
    const metricsRes = http.get(`${BASE_URL}/api/manager/metrics`, { headers })
    check(metricsRes, {
      'metrics status 200': (r) => r.status === 200,
    })

    // Orders
    const ordersRes = http.get(`${BASE_URL}/api/manager/orders`, { headers })
    check(ordersRes, {
      'orders status 200': (r) => r.status === 200,
    })

    // Tables
    const tablesRes = http.get(`${BASE_URL}/api/manager/tables`, { headers })
    check(tablesRes, {
      'tables status 200': (r) => r.status === 200,
    })
  })

  sleep(2)
}

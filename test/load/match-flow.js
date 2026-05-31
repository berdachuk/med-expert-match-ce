import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 5 },
    { duration: '30s', target: 10 },
    { duration: '60s', target: 10 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<30000'],
    http_req_failed: ['rate<0.05'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const health = http.get(`${BASE_URL}/actuator/health`);
  check(health, { 'health OK': (r) => r.status === 200 });

  const search = http.get(`${BASE_URL}/api/v1/documents/search?q=cardiology&limit=5`);
  check(search, { 'search OK': (r) => r.status === 200 });

  const prometheus = http.get(`${BASE_URL}/actuator/prometheus`);
  check(prometheus, { 'metrics OK': (r) => r.status === 200 && r.body.includes('jvm_memory') });

  sleep(2);
}

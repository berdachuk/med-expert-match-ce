import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '20s', target: 5 },
    { duration: '40s', target: 10 },
    { duration: '40s', target: 10 },
    { duration: '20s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const QUERIES = [
  'cardiology',
  'diabetes management',
  'stroke rehabilitation',
  'pediatric asthma',
  'oncology guidelines',
];

export default function () {
  const query = QUERIES[Math.floor(Math.random() * QUERIES.length)];
  const search = http.get(`${BASE_URL}/api/v1/documents/search?q=${encodeURIComponent(query)}&limit=10`);
  check(search, {
    'search OK': (r) => r.status === 200,
    'search has body': (r) => r.body && r.body.length > 0,
  });

  const faceted = http.get(
    `${BASE_URL}/api/v2/documents/search?q=${encodeURIComponent(query)}&limit=5&category=clinical`);
  check(faceted, { 'faceted search OK': (r) => r.status === 200 || r.status === 404 });

  sleep(1);
}

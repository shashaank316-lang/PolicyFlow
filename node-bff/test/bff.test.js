const test = require('node:test');
const assert = require('node:assert/strict');
const request = require('supertest');

process.env.APP_SECURITY_USERNAME ||= 'test-user';
process.env.APP_SECURITY_PASSWORD ||= 'test-password';

const app = require('../index');

const customer = {
  id: 1,
  customerNumber: 'CUST-K8S-001',
  firstName: 'PolicyFlow',
  lastName: 'KubernetesTest',
  email: 'policyflow.k8s.test@example.com',
  status: 'ACTIVE'
};

const policy = {
  id: 1,
  customerId: 'CUST-K8S-001',
  policyNumber: 'POL-K8S-001',
  policyType: 'AUTO',
  premium: 1250.50,
  status: 'ACTIVE'
};

const claim = {
  id: 1,
  claimNumber: 'CLM-K8S-001',
  customerId: 'CUST-K8S-001',
  policyNumber: 'POL-K8S-001',
  claimType: 'AUTO_ACCIDENT',
  claimAmount: 750,
  status: 'SUBMITTED'
};

function installBackendMock(data) {
  global.fetch = async url => {
    const path = new URL(url).pathname;

    if (Object.prototype.hasOwnProperty.call(data, path)) {
      return {
        ok: true,
        status: 200,
        text: async () => JSON.stringify(data[path])
      };
    }

    return {
      ok: false,
      status: 404,
      text: async () => JSON.stringify({ error: 'Not found' })
    };
  };
}

function installSuccessfulBackend() {
  installBackendMock({
    '/api/customers': [customer],
    '/api/policies': [policy],
    '/api/claims': [claim]
  });
}

test('GET /health returns service status', async () => {
  const response = await request(app).get('/health');

  assert.equal(response.status, 200);
  assert.deepEqual(response.body, {
    status: 'UP',
    service: 'policyflow-bff'
  });
});

test('customer overview aggregates customer, policies, and claims', async () => {
  const originalFetch = global.fetch;
  installSuccessfulBackend();

  try {
    const response = await request(app)
      .get('/api/customers/number/CUST-K8S-001/overview');

    assert.equal(response.status, 200);
    assert.equal(response.body.customer.customerNumber, 'CUST-K8S-001');
    assert.equal(response.body.policies.length, 1);
    assert.equal(response.body.claims.length, 1);
    assert.deepEqual(response.body.summary, {
      policyCount: 1,
      claimCount: 1,
      totalClaimAmount: 750
    });
  } finally {
    global.fetch = originalFetch;
  }
});

test('customer overview returns 404 when customer does not exist', async () => {
  const originalFetch = global.fetch;
  installSuccessfulBackend();

  try {
    const response = await request(app)
      .get('/api/customers/number/CUST-DOES-NOT-EXIST/overview');

    assert.equal(response.status, 404);
    assert.equal(response.body.error, 'Customer not found');
    assert.equal(
      response.body.customerNumber,
      'CUST-DOES-NOT-EXIST'
    );
  } finally {
    global.fetch = originalFetch;
  }
});

test('customer overview returns 502 when a backend is unavailable', async () => {
  const originalFetch = global.fetch;
  global.fetch = async () => {
    throw new Error('Simulated backend outage');
  };

  try {
    const response = await request(app)
      .get('/api/customers/number/CUST-K8S-001/overview');

    assert.equal(response.status, 502);
    assert.equal(
      response.body.error,
      'Unable to retrieve customer overview from backend services'
    );
  } finally {
    global.fetch = originalFetch;
  }
});

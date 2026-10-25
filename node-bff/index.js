require('dotenv').config({
  path: require('path').resolve(__dirname, '../.env')
});

const express = require('express');

const app = express();
const PORT = process.env.PORT || 3000;

const services = {
  policy: process.env.POLICY_SERVICE_URL || 'http://127.0.0.1:18080',
  customer: process.env.CUSTOMER_SERVICE_URL || 'http://127.0.0.1:18081',
  claims: process.env.CLAIMS_SERVICE_URL || 'http://127.0.0.1:18082'
};

app.use(express.json());

app.get('/health', (req, res) => {
  res.status(200).json({
    status: 'UP',
    service: 'policyflow-bff'
  });
});

async function proxyGet(req, res, baseUrl, backendPath) {
  const username = process.env.APP_SECURITY_USERNAME;
  const password = process.env.APP_SECURITY_PASSWORD;

  if (!username || !password) {
    return res.status(500).json({
      error: 'BFF authentication configuration is missing'
    });
  }

  const authorization = Buffer
    .from(`${username}:${password}`)
    .toString('base64');

  try {
    const response = await fetch(
      `${baseUrl.replace(/\/$/, '')}${backendPath}`,
      {
        method: 'GET',
        headers: {
          Authorization: `Basic ${authorization}`,
          Accept: 'application/json'
        },
        signal: AbortSignal.timeout(5000)
      }
    );

    const body = await response.text();
    const contentType = response.headers.get('content-type');

    res.status(response.status);

    if (contentType) {
      res.set('Content-Type', contentType);
    }

    return res.send(body);
  } catch (error) {
    console.error('Backend request failed:', error.message);

    return res.status(502).json({
      error: 'Backend service is unavailable'
    });
  }
}

app.get('/api/policies/number/:policyNumber', (req, res) => {
  const path =
    `/api/policies/number/${encodeURIComponent(req.params.policyNumber)}`;

  return proxyGet(req, res, services.policy, path);
});

app.get('/api/customers/number/:customerNumber', (req, res) => {
  const path =
    `/api/customers/number/${encodeURIComponent(req.params.customerNumber)}`;

  return proxyGet(req, res, services.customer, path);
});

app.get('/api/claims/:id', (req, res) => {
  const path = `/api/claims/${encodeURIComponent(req.params.id)}`;

  return proxyGet(req, res, services.claims, path);
});


async function fetchBackendJson(baseUrl, backendPath, authorization) {
  const response = await fetch(
    `${baseUrl.replace(/\/$/, '')}${backendPath}`,
    {
      method: 'GET',
      headers: {
        Authorization: authorization,
        Accept: 'application/json'
      },
      signal: AbortSignal.timeout(5000)
    }
  );

  const body = await response.text();

  if (!response.ok) {
    throw new Error(`Backend returned HTTP ${response.status}`);
  }

  try {
    return JSON.parse(body);
  } catch {
    throw new Error('Backend returned an invalid JSON response');
  }
}

app.get('/api/customers/number/:customerNumber/overview', async (req, res) => {
  const { customerNumber } = req.params;
  const username = process.env.APP_SECURITY_USERNAME;
  const password = process.env.APP_SECURITY_PASSWORD;

  if (!username || !password) {
    return res.status(500).json({
      error: 'BFF authentication configuration is missing'
    });
  }

  const authorization = `Basic ${Buffer
    .from(`${username}:${password}`)
    .toString('base64')}`;

  try {
    const [customers, policies, claims] = await Promise.all([
      fetchBackendJson(services.customer, '/api/customers', authorization),
      fetchBackendJson(services.policy, '/api/policies', authorization),
      fetchBackendJson(services.claims, '/api/claims', authorization)
    ]);

    if (
      !Array.isArray(customers) ||
      !Array.isArray(policies) ||
      !Array.isArray(claims)
    ) {
      throw new Error('Backend returned an unexpected data structure');
    }

    const customer = customers.find(
      item => item.customerNumber === customerNumber
    );

    if (!customer) {
      return res.status(404).json({
        error: 'Customer not found',
        customerNumber
      });
    }

    const customerPolicies = policies.filter(
      policy => policy.customerId === customerNumber
    );

    const customerPolicyNumbers = new Set(
      customerPolicies.map(policy => policy.policyNumber)
    );

    const customerClaims = claims.filter(
      claim =>
        claim.customerId === customerNumber ||
        customerPolicyNumbers.has(claim.policyNumber)
    );

    const totalClaimAmount = customerClaims.reduce(
      (total, claim) => total + Number(claim.claimAmount || 0),
      0
    );

    return res.status(200).json({
      customer,
      policies: customerPolicies,
      claims: customerClaims,
      summary: {
        policyCount: customerPolicies.length,
        claimCount: customerClaims.length,
        totalClaimAmount: Number(totalClaimAmount.toFixed(2))
      }
    });
  } catch (error) {
    console.error('Customer overview aggregation failed:', error.message);

    return res.status(502).json({
      error: 'Unable to retrieve customer overview from backend services'
    });
  }
});

if (require.main === module) {
  app.listen(PORT, '0.0.0.0', () => {
    console.log(`PolicyFlow BFF listening on port ${PORT}`);
  });
}

module.exports = app;

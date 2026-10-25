import { useCallback, useEffect, useState } from "react";
import "./App.css";

const API_BASE = "http://localhost:8080/api/policies";

const emptyForm = {
  customerId: "",
  policyNumber: "",
  policyType: "AUTO",
  premium: "",
  status: "ACTIVE",
};

function App() {
  const [policies, setPolicies] = useState([]);
  const [status, setStatus] = useState("Checking backend...");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [searchNumber, setSearchNumber] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const loadPolicies = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const response = await fetch(API_BASE);

      if (!response.ok) {
        throw new Error(`Could not load policies (HTTP ${response.status}).`);
      }

      const data = await response.json();
      setPolicies(Array.isArray(data) ? data : []);
      setStatus(`Backend connected: HTTP ${response.status}`);
    } catch (err) {
      setError(err.message || "Unable to connect to the policy API.");
      setStatus("Connection failed");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadPolicies();
  }, [loadPolicies]);

  function updateField(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  async function testBackend() {
    setMessage("");
    setError("");

    try {
      const response = await fetch(API_BASE);
      if (!response.ok) {
        throw new Error(`Backend responded with HTTP ${response.status}.`);
      }

      setStatus(`Backend connected: HTTP ${response.status}`);
      setMessage("Backend connection successful.");
      await loadPolicies();
    } catch (err) {
      setStatus("Connection failed");
      setError(err.message || "Check that the backend is running.");
    }
  }

  async function searchPolicy(event) {
    event.preventDefault();
    setMessage("");
    setError("");

    const number = searchNumber.trim();

    if (!number) {
      await loadPolicies();
      return;
    }

    setLoading(true);

    try {
      const response = await fetch(
          `${API_BASE}/number/${encodeURIComponent(number)}`
      );

      if (!response.ok) {
        if (response.status === 404) {
          setPolicies([]);
          setMessage("No policy found with that policy number.");
          return;
        }

        throw new Error(`Search failed (HTTP ${response.status}).`);
      }

      const data = await response.json();
      setPolicies(Array.isArray(data) ? data : [data]);
      setMessage("Search completed.");
    } catch (err) {
      setError(err.message || "Could not search for the policy.");
    } finally {
      setLoading(false);
    }
  }

  async function createPolicy(event) {
    event.preventDefault();
    setMessage("");
    setError("");

    const premium = Number(form.premium);

    if (!form.customerId.trim() || !form.policyNumber.trim()) {
      setError("Customer ID and policy number are required.");
      return;
    }

    if (form.premium === "" || !Number.isFinite(premium) || premium <= 0) {
      setError("Premium must be a number greater than zero.");
      return;
    }

    setSaving(true);

    try {
      const response = await fetch(API_BASE, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          customerId: form.customerId.trim(),
          policyNumber: form.policyNumber.trim(),
          policyType: form.policyType,
          premium,
          status: form.status,
        }),
      });

      if (!response.ok) {
        const details = await response.text();
        throw new Error(
            details || `Policy creation failed (HTTP ${response.status}).`
        );
      }

      setForm(emptyForm);
      setSearchNumber("");
      setMessage("Policy created successfully.");
      await loadPolicies();
    } catch (err) {
      setError(err.message || "Unable to create the policy.");
    } finally {
      setSaving(false);
    }
  }

  const money = (value) =>
      new Intl.NumberFormat("en-US", {
        style: "currency",
        currency: "USD",
      }).format(Number(value) || 0);

  const activeCount = policies.filter(
      (policy) => policy.status === "ACTIVE"
  ).length;

  return (
      <main className="app-shell">
        <header className="topbar">
          <a className="brand" href="#home">
            <span className="brand-mark">P</span>
            <span>PolicyFlow</span>
          </a>
          <span className="environment">Insurance Operations</span>
        </header>

        <div className="page-content" id="home">
          <section className="page-heading">
            <div>
              <p className="eyebrow">POLICY MANAGEMENT</p>
              <h1>Insurance policies</h1>
              <p className="subtitle">
                Manage policy records from one place.
              </p>
            </div>
            <button className="button button-secondary" onClick={testBackend}>
              Test backend
            </button>
          </section>

          <section className="stats-grid" aria-label="Policy statistics">
            <article className="stat-card">
              <span className="stat-label">Policies displayed</span>
              <strong className="stat-number">{policies.length}</strong>
              <span className="stat-footnote">Current result set</span>
            </article>
            <article className="stat-card">
              <span className="stat-label">Active policies</span>
              <strong className="stat-number">{activeCount}</strong>
              <span className="stat-footnote">Status: ACTIVE</span>
            </article>
            <article className="stat-card connection-card">
              <span className="stat-label">Backend status</span>
              <strong className="connection-value">
              <span
                  className={`status-dot ${
                      status.includes("connected") ? "online" : ""
                  }`}
              />
                {status}
              </strong>
              <span className="stat-footnote">Policy service · Port 8080</span>
            </article>
          </section>

          {message && (
              <div className="notice success-notice" role="status">
                {message}
              </div>
          )}
          {error && (
              <div className="notice error-notice" role="alert">
                {error}
              </div>
          )}

          <section className="panel">
            <div className="panel-heading">
              <div>
                <h2>Policy records</h2>
                <p>Data retrieved from the policy service.</p>
              </div>
              <button
                  className="button button-secondary"
                  onClick={loadPolicies}
                  disabled={loading}
              >
                {loading ? "Loading..." : "Refresh"}
              </button>
            </div>

            <form className="search-form" onSubmit={searchPolicy}>
              <label className="search-field">
                <span>Search by policy number</span>
                <input
                    value={searchNumber}
                    onChange={(event) => setSearchNumber(event.target.value)}
                    placeholder="e.g. POL-2026-1004"
                />
              </label>
              <button className="button button-primary" type="submit">
                Search
              </button>
              <button
                  className="button button-secondary"
                  type="button"
                  onClick={() => {
                    setSearchNumber("");
                    setMessage("");
                    loadPolicies();
                  }}
              >
                Clear
              </button>
            </form>

            <div className="table-wrapper">
              <table>
                <thead>
                <tr>
                  <th>Policy number</th>
                  <th>Customer ID</th>
                  <th>Type</th>
                  <th>Premium</th>
                  <th>Status</th>
                </tr>
                </thead>
                <tbody>
                {policies.map((policy) => (
                    <tr key={policy.id ?? policy.policyNumber}>
                      <td className="policy-number">{policy.policyNumber}</td>
                      <td>{policy.customerId}</td>
                      <td>{policy.policyType}</td>
                      <td>{money(policy.premium)}</td>
                      <td>
                      <span
                          className={`status-badge ${
                              policy.status === "ACTIVE" ? "badge-active" : ""
                          }`}
                      >
                        {policy.status}
                      </span>
                      </td>
                    </tr>
                ))}
                {!loading && policies.length === 0 && (
                    <tr>
                      <td colSpan="5" className="empty-state">
                        No policies to display. Refresh the list or create a
                        policy below.
                      </td>
                    </tr>
                )}
                </tbody>
              </table>
            </div>
            <div className="table-footer">
              Showing {policies.length} {policies.length === 1 ? "policy" : "policies"}
            </div>
          </section>

          <section className="panel create-panel">
            <div className="panel-heading">
              <div>
                <h2>Create a policy</h2>
                <p>Submit a new policy record to the backend.</p>
              </div>
            </div>

            <form className="policy-form" onSubmit={createPolicy}>
              <label>
                Customer ID
                <input
                    name="customerId"
                    value={form.customerId}
                    onChange={updateField}
                    placeholder="CUS-10002"
                    required
                />
              </label>

              <label>
                Policy number
                <input
                    name="policyNumber"
                    value={form.policyNumber}
                    onChange={updateField}
                    placeholder="POL-2026-1005"
                    required
                />
              </label>

              <label>
                Policy type
                <select
                    name="policyType"
                    value={form.policyType}
                    onChange={updateField}
                >
                  <option value="AUTO">Auto</option>
                  <option value="HOME">Home</option>
                  <option value="LIFE">Life</option>
                  <option value="HEALTH">Health</option>
                </select>
              </label>

              <label>
                Premium (USD)
                <input
                    name="premium"
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={form.premium}
                    onChange={updateField}
                    placeholder="1250.00"
                    required
                />
              </label>

              <label>
                Status
                <select
                    name="status"
                    value={form.status}
                    onChange={updateField}
                >
                  <option value="ACTIVE">Active</option>
                  <option value="PENDING">Pending</option>
                  <option value="CANCELLED">Cancelled</option>
                </select>
              </label>

              <div className="form-actions">
                <button
                    className="button button-primary"
                    type="submit"
                    disabled={saving}
                >
                  {saving ? "Creating..." : "Create policy"}
                </button>
              </div>
            </form>
          </section>

          <footer className="page-footer">
            PolicyFlow · React + Spring Boot + PostgreSQL
          </footer>
        </div>
      </main>
  );
}

export default App;

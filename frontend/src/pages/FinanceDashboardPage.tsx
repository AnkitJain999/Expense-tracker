import { useEffect, useState } from 'react';
import { fetchDashboard } from '../api/dashboard';
import { apiErrorMessage } from '../api/client';
import type { DashboardResponse } from '../types';

function money(value: number): string {
  return value.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

export function FinanceDashboardPage() {
  const [data, setData] = useState<DashboardResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchDashboard()
      .then(setData)
      .catch((err) => setError(apiErrorMessage(err)));
  }, []);

  if (error) return <div className="page"><div className="error">{error}</div></div>;
  if (!data) return <div className="page">Loading…</div>;

  const maxAmount = Math.max(1, ...data.departments.map((d) => d.totalPendingAmount));

  return (
    <div className="page">
      <h2>Finance Dashboard</h2>
      <div className="summary-cards">
        <div className="card stat">
          <div className="stat-label">Total pending</div>
          <div className="stat-value">${money(data.grandTotalPending)}</div>
        </div>
        <div className="card stat">
          <div className="stat-label">Pending items</div>
          <div className="stat-value">{data.grandTotalCount}</div>
        </div>
        <div className="card stat">
          <div className="stat-label">Departments</div>
          <div className="stat-value">{data.departments.length}</div>
        </div>
      </div>

      <h3>Pending amount by department</h3>
      <div className="card">
        {data.departments.length === 0 ? (
          <p className="muted">No departments found.</p>
        ) : (
          <div className="bar-chart">
            {data.departments.map((d) => (
              <div className="bar-row" key={d.departmentId}>
                <div className="bar-label">{d.departmentName}</div>
                <div className="bar-track">
                  <div
                    className="bar-fill"
                    style={{ width: `${(d.totalPendingAmount / maxAmount) * 100}%` }}
                  />
                </div>
                <div className="bar-value">${money(d.totalPendingAmount)}</div>
              </div>
            ))}
          </div>
        )}
      </div>

      <h3>Breakdown</h3>
      <table className="table">
        <thead>
          <tr>
            <th>Department</th>
            <th>Pending items</th>
            <th>Awaiting Team Lead</th>
            <th>Awaiting Finance</th>
            <th>Total pending</th>
          </tr>
        </thead>
        <tbody>
          {data.departments.map((d) => (
            <tr key={d.departmentId}>
              <td>{d.departmentName}</td>
              <td>{d.pendingCount}</td>
              <td>${money(d.pendingTeamLeadAmount)}</td>
              <td>${money(d.pendingFinanceAmount)}</td>
              <td>
                <strong>${money(d.totalPendingAmount)}</strong>
              </td>
            </tr>
          ))}
        </tbody>
        <tfoot>
          <tr>
            <td>All departments</td>
            <td>{data.grandTotalCount}</td>
            <td></td>
            <td></td>
            <td>
              <strong>${money(data.grandTotalPending)}</strong>
            </td>
          </tr>
        </tfoot>
      </table>
    </div>
  );
}

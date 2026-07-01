import { Fragment, useEffect, useState } from 'react';
import { fetchMyExpenses } from '../api/expenses';
import { apiErrorMessage } from '../api/client';
import type { ExpenseResponse } from '../types';
import { StatusBadge } from '../components/StatusBadge';
import { ExpenseDetail } from '../components/ExpenseDetail';

export function MyExpensesPage() {
  const [expenses, setExpenses] = useState<ExpenseResponse[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [expandedId, setExpandedId] = useState<number | null>(null);

  useEffect(() => {
    fetchMyExpenses()
      .then(setExpenses)
      .catch((err) => setError(apiErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="page">Loading…</div>;

  return (
    <div className="page">
      <h2>My Expenses</h2>
      {error && <div className="error">{error}</div>}
      {expenses.length === 0 ? (
        <p className="muted">You haven’t submitted any expenses yet.</p>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>#</th>
              <th>Category</th>
              <th>Amount</th>
              <th>Description</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {expenses.map((e) => (
              <Fragment key={e.id}>
                <tr>
                  <td>{e.id}</td>
                  <td>{e.category}</td>
                  <td>
                    {e.amount.toFixed(2)} {e.currency}
                  </td>
                  <td>{e.description}</td>
                  <td>
                    <StatusBadge status={e.status} />
                  </td>
                  <td>
                    <button
                      className="link-btn"
                      onClick={() => setExpandedId(expandedId === e.id ? null : e.id)}
                    >
                      {expandedId === e.id ? 'Hide' : 'Details'}
                    </button>
                  </td>
                </tr>
                {expandedId === e.id && (
                  <tr>
                    <td colSpan={6} className="detail-cell">
                      <ExpenseDetail expenseId={e.id} />
                    </td>
                  </tr>
                )}
              </Fragment>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

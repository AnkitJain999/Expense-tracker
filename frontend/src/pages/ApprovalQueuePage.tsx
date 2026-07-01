import { Fragment, useEffect, useState } from 'react';
import { approveExpense, fetchPending, rejectExpense } from '../api/approvals';
import { apiErrorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import type { ExpenseResponse } from '../types';
import { StatusBadge } from '../components/StatusBadge';
import { ExpenseDetail } from '../components/ExpenseDetail';

export function ApprovalQueuePage() {
  const { user } = useAuth();
  const [pending, setPending] = useState<ExpenseResponse[]>([]);
  const [comments, setComments] = useState<Record<number, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [expandedId, setExpandedId] = useState<number | null>(null);

  const stageLabel = user?.role === 'FINANCE_MANAGER' ? 'Finance approval' : 'Team Lead approval';

  function reload() {
    setLoading(true);
    fetchPending()
      .then(setPending)
      .catch((err) => setError(apiErrorMessage(err)))
      .finally(() => setLoading(false));
  }

  useEffect(reload, []);

  async function handleApprove(id: number) {
    setError(null);
    setBusyId(id);
    try {
      await approveExpense(id, comments[id]?.trim() || undefined);
      setPending((prev) => prev.filter((e) => e.id !== id));
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setBusyId(null);
    }
  }

  async function handleReject(id: number) {
    setError(null);
    const comment = comments[id]?.trim();
    if (!comment) {
      setError('A comment is required to reject an expense.');
      return;
    }
    setBusyId(id);
    try {
      await rejectExpense(id, comment);
      setPending((prev) => prev.filter((e) => e.id !== id));
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setBusyId(null);
    }
  }

  if (loading) return <div className="page">Loading…</div>;

  return (
    <div className="page">
      <h2>Approval Queue</h2>
      <p className="muted">Awaiting {stageLabel}</p>
      {error && <div className="error">{error}</div>}
      {pending.length === 0 ? (
        <p className="muted">Nothing awaiting your approval. 🎉</p>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>#</th>
              <th>Submitter</th>
              <th>Department</th>
              <th>Category</th>
              <th>Amount</th>
              <th>Description</th>
              <th>Status</th>
              <th>Comment &amp; action</th>
            </tr>
          </thead>
          <tbody>
            {pending.map((e) => (
              <Fragment key={e.id}>
                <tr>
                  <td>{e.id}</td>
                  <td>{e.submitterName}</td>
                  <td>{e.departmentName}</td>
                  <td>{e.category}</td>
                  <td>
                    {e.amount.toFixed(2)} {e.currency}
                  </td>
                  <td>{e.description}</td>
                  <td>
                    <StatusBadge status={e.status} />
                  </td>
                  <td className="action-cell">
                    <input
                      type="text"
                      placeholder="Comment (required to reject)"
                      value={comments[e.id] ?? ''}
                      onChange={(ev) =>
                        setComments((prev) => ({ ...prev, [e.id]: ev.target.value }))
                      }
                    />
                    <div className="action-buttons">
                      <button
                        className="btn-approve"
                        disabled={busyId === e.id}
                        onClick={() => handleApprove(e.id)}
                      >
                        Approve
                      </button>
                      <button
                        className="btn-reject"
                        disabled={busyId === e.id}
                        onClick={() => handleReject(e.id)}
                      >
                        Reject
                      </button>
                      <button
                        className="link-btn"
                        onClick={() => setExpandedId(expandedId === e.id ? null : e.id)}
                      >
                        {expandedId === e.id ? 'Hide' : 'Details'}
                      </button>
                    </div>
                  </td>
                </tr>
                {expandedId === e.id && (
                  <tr>
                    <td colSpan={8} className="detail-cell">
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

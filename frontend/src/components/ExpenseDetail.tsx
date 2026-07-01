import { useEffect, useState } from 'react';
import { fetchExpenseDetail } from '../api/expenses';
import { apiErrorMessage } from '../api/client';
import type { ExpenseDetailResponse } from '../types';
import { ApprovalHistory } from './ApprovalHistory';
import { ReceiptViewer } from './ReceiptViewer';

/** Loads and shows an expense's approval history and receipt. */
export function ExpenseDetail({ expenseId }: { expenseId: number }) {
  const [detail, setDetail] = useState<ExpenseDetailResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    fetchExpenseDetail(expenseId)
      .then((d) => active && setDetail(d))
      .catch((err) => active && setError(apiErrorMessage(err)));
    return () => {
      active = false;
    };
  }, [expenseId]);

  if (error) return <div className="error">{error}</div>;
  if (!detail) return <p className="muted">Loading…</p>;

  return (
    <div className="detail-grid">
      <div>
        <h4>Approval history</h4>
        <ApprovalHistory history={detail.history} />
      </div>
      <div>
        <h4>Receipt</h4>
        {detail.expense.hasReceipt ? (
          <ReceiptViewer expenseId={expenseId} />
        ) : (
          <p className="muted">No receipt attached.</p>
        )}
      </div>
    </div>
  );
}

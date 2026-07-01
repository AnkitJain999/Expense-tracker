import type { ExpenseStatus } from '../types';

const LABELS: Record<ExpenseStatus, string> = {
  PENDING_TEAM_LEAD: 'Pending Team Lead',
  PENDING_FINANCE: 'Pending Finance',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
};

export function StatusBadge({ status }: { status: ExpenseStatus }) {
  return <span className={`badge badge-${status.toLowerCase()}`}>{LABELS[status]}</span>;
}

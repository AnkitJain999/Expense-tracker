import type { ApprovalAction, ApprovalEventResponse } from '../types';

const ACTION_LABELS: Record<ApprovalAction, string> = {
  SUBMITTED: 'Submitted',
  TEAM_LEAD_APPROVED: 'Approved by Team Lead',
  TEAM_LEAD_REJECTED: 'Rejected by Team Lead',
  FINANCE_APPROVED: 'Approved by Finance Manager',
  FINANCE_REJECTED: 'Rejected by Finance Manager',
};

export function ApprovalHistory({ history }: { history: ApprovalEventResponse[] }) {
  if (history.length === 0) {
    return <p className="muted">No history yet.</p>;
  }
  return (
    <ol className="timeline">
      {history.map((event) => (
        <li key={event.id}>
          <div className="timeline-action">{ACTION_LABELS[event.action]}</div>
          <div className="muted">
            {event.actorName} · {new Date(event.createdAt).toLocaleString()}
          </div>
          {event.comment && <div className="timeline-comment">“{event.comment}”</div>}
        </li>
      ))}
    </ol>
  );
}

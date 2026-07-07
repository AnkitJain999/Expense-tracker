import { useState, type FormEvent } from 'react';
import { submitExpense } from '../api/expenses';
import { apiErrorMessage } from '../api/client';
import { CATEGORIES, type Category } from '../types';

export function SubmitExpensePage() {
  const [category, setCategory] = useState<Category>('TRAVEL');
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [receipt, setReceipt] = useState<File | null>(null);
  const [fileInputKey, setFileInputKey] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setMessage(null);
    const parsedAmount = Number(amount);
    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) {
      setError('Amount must be greater than 0');
      return;
    }
    setSubmitting(true);
    try {
      const created = await submitExpense({ category, amount: parsedAmount, description, receipt });
      const pendingStage = created.status === 'PENDING_FINANCE' ? 'Finance Manager' : 'Team Lead';
      setMessage(`Expense #${created.id} submitted and is pending ${pendingStage} approval.`);
      setAmount('');
      setDescription('');
      setReceipt(null);
      setFileInputKey((k) => k + 1); // remount the file input to clear it
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="page">
      <h2>Submit an Expense</h2>
      <form className="card form" onSubmit={handleSubmit}>
        <label>
          Category
          <select value={category} onChange={(e) => setCategory(e.target.value as Category)}>
            {CATEGORIES.map((c) => (
              <option key={c} value={c}>
                {c.charAt(0) + c.slice(1).toLowerCase()}
              </option>
            ))}
          </select>
        </label>
        <label>
          Amount (USD)
          <input
            type="number"
            step="0.01"
            min="0.01"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            required
          />
        </label>
        <label>
          Description
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            maxLength={1000}
            rows={3}
            required
          />
        </label>
        <label>
          Receipt (PNG, JPEG, or PDF — max 5MB)
          <input
            key={fileInputKey}
            type="file"
            accept="image/png,image/jpeg,application/pdf"
            onChange={(e) => setReceipt(e.target.files?.[0] ?? null)}
          />
        </label>
        {message && <div className="success">{message}</div>}
        {error && <div className="error">{error}</div>}
        <button type="submit" disabled={submitting}>
          {submitting ? 'Submitting…' : 'Submit expense'}
        </button>
      </form>
    </div>
  );
}

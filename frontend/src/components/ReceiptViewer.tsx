import { useEffect, useState } from 'react';
import { api } from '../api/client';

/**
 * Fetches a receipt through the authenticated API (so the JWT header is sent)
 * and renders it inline as an image or PDF via an object URL.
 */
export function ReceiptViewer({ expenseId }: { expenseId: number }) {
  const [url, setUrl] = useState<string | null>(null);
  const [contentType, setContentType] = useState<string>('');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let objectUrl: string | null = null;
    let active = true;
    async function load() {
      try {
        const response = await api.get(`/receipts/${expenseId}`, { responseType: 'blob' });
        if (!active) return;
        objectUrl = URL.createObjectURL(response.data as Blob);
        setContentType((response.data as Blob).type);
        setUrl(objectUrl);
      } catch {
        if (active) setError('Could not load receipt');
      }
    }
    void load();
    return () => {
      active = false;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [expenseId]);

  if (error) return <p className="muted">{error}</p>;
  if (!url) return <p className="muted">Loading receipt…</p>;

  if (contentType === 'application/pdf') {
    return <iframe title="Receipt" src={url} className="receipt-frame" />;
  }
  return <img src={url} alt="Receipt" className="receipt-image" />;
}

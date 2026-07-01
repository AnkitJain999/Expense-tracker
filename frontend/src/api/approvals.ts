import { api } from './client';
import type { ExpenseResponse } from '../types';

export async function fetchPending(): Promise<ExpenseResponse[]> {
  const { data } = await api.get<ExpenseResponse[]>('/approvals/pending');
  return data;
}

export async function approveExpense(expenseId: number, comment?: string): Promise<ExpenseResponse> {
  const { data } = await api.post<ExpenseResponse>(`/approvals/${expenseId}/approve`, { comment });
  return data;
}

export async function rejectExpense(expenseId: number, comment: string): Promise<ExpenseResponse> {
  const { data } = await api.post<ExpenseResponse>(`/approvals/${expenseId}/reject`, { comment });
  return data;
}

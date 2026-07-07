import { api } from './client';
import type { Category, ExpenseDetailResponse, ExpenseResponse } from '../types';

export interface SubmitExpenseInput {
  category: Category;
  amount: number;
  currency?: string;
  description: string;
  receipt?: File | null;
}

export async function submitExpense(input: SubmitExpenseInput): Promise<ExpenseResponse> {
  const form = new FormData();
  const data = {
    category: input.category,
    amount: input.amount,
    currency: input.currency ?? 'INR',
    description: input.description,
  };
  // The "data" part is JSON; the "receipt" part is the file.
  form.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
  if (input.receipt) {
    form.append('receipt', input.receipt);
  }
  const { data: response } = await api.post<ExpenseResponse>('/expenses', form);
  return response;
}

export async function fetchMyExpenses(): Promise<ExpenseResponse[]> {
  const { data } = await api.get<ExpenseResponse[]>('/expenses/mine');
  return data;
}

export async function fetchExpenseDetail(id: number): Promise<ExpenseDetailResponse> {
  const { data } = await api.get<ExpenseDetailResponse>(`/expenses/${id}`);
  return data;
}

export function receiptUrl(expenseId: number): string {
  return `/api/v1/receipts/${expenseId}`;
}

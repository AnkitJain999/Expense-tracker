import { api } from './client';
import type { DashboardResponse } from '../types';

export async function fetchDashboard(): Promise<DashboardResponse> {
  const { data } = await api.get<DashboardResponse>('/dashboard/pending-by-department');
  return data;
}

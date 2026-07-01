import { api } from './client';
import type { AdminUserResponse, DepartmentSummary, Role } from '../types';

export interface CreateUserInput {
  email: string;
  password: string;
  fullName: string;
  role: Role;
  departmentId?: number | null;
}

export async function fetchUsers(): Promise<AdminUserResponse[]> {
  const { data } = await api.get<AdminUserResponse[]>('/admin/users');
  return data;
}

export async function createUser(input: CreateUserInput): Promise<AdminUserResponse> {
  const { data } = await api.post<AdminUserResponse>('/admin/users', input);
  return data;
}

export async function fetchDepartments(): Promise<DepartmentSummary[]> {
  const { data } = await api.get<DepartmentSummary[]>('/departments');
  return data;
}

import { api } from './client';
import type { AdminUserResponse, DepartmentSummary, Role } from '../types';

export interface CreateUserInput {
  email: string;
  password: string;
  fullName: string;
  role: Role;
  departmentId?: number | null;
}

export interface UpdateUserInput {
  email: string;
  password?: string;
  fullName: string;
  role: Role;
  departmentId?: number | null;
  enabled: boolean;
}

export async function fetchUsers(): Promise<AdminUserResponse[]> {
  const { data } = await api.get<AdminUserResponse[]>('/admin/users');
  return data;
}

export async function createUser(input: CreateUserInput): Promise<AdminUserResponse> {
  const { data } = await api.post<AdminUserResponse>('/admin/users', input);
  return data;
}

export async function updateUser(id: number, input: UpdateUserInput): Promise<AdminUserResponse> {
  const { data } = await api.put<AdminUserResponse>(`/admin/users/${id}`, input);
  return data;
}

export async function deleteUser(id: number): Promise<void> {
  await api.delete(`/admin/users/${id}`);
}

export interface DepartmentInput {
  name: string;
  teamLeadId?: number | null;
  financeManagerId?: number | null;
}

export async function fetchDepartments(): Promise<DepartmentSummary[]> {
  const { data } = await api.get<DepartmentSummary[]>('/departments');
  return data;
}

export async function createDepartment(input: DepartmentInput): Promise<DepartmentSummary> {
  const { data } = await api.post<DepartmentSummary>('/departments', input);
  return data;
}

export async function updateDepartment(id: number, input: DepartmentInput): Promise<DepartmentSummary> {
  const { data } = await api.put<DepartmentSummary>(`/departments/${id}`, input);
  return data;
}

export async function deleteDepartment(id: number): Promise<void> {
  await api.delete(`/departments/${id}`);
}

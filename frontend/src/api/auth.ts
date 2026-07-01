import { api } from './client';
import type { AuthResponse, UserProfile } from '../types';

export async function login(email: string, password: string): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>('/auth/login', { email, password });
  return data;
}

export async function fetchMe(): Promise<UserProfile> {
  const { data } = await api.get<UserProfile>('/auth/me');
  return data;
}

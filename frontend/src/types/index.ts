export type Role = 'EMPLOYEE' | 'TEAM_LEAD' | 'FINANCE_MANAGER';

export type Category = 'TRAVEL' | 'MEALS' | 'SUPPLIES' | 'SOFTWARE' | 'OTHER';

export type ExpenseStatus =
  | 'PENDING_TEAM_LEAD'
  | 'PENDING_FINANCE'
  | 'APPROVED'
  | 'REJECTED';

export type ApprovalAction =
  | 'SUBMITTED'
  | 'TEAM_LEAD_APPROVED'
  | 'TEAM_LEAD_REJECTED'
  | 'FINANCE_APPROVED'
  | 'FINANCE_REJECTED';

export interface UserProfile {
  id: number;
  email: string;
  fullName: string;
  role: Role;
  departmentId: number | null;
}

export interface AuthResponse {
  token: string;
  expiresInMinutes: number;
  user: UserProfile;
}

export interface ExpenseResponse {
  id: number;
  category: Category;
  amount: number;
  currency: string;
  description: string;
  status: ExpenseStatus;
  submitterId: number;
  submitterName: string;
  departmentId: number;
  departmentName: string;
  hasReceipt: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ApprovalEventResponse {
  id: number;
  action: ApprovalAction;
  actorId: number;
  actorName: string;
  comment: string | null;
  createdAt: string;
}

export interface ExpenseDetailResponse {
  expense: ExpenseResponse;
  history: ApprovalEventResponse[];
}

export interface PendingByDepartmentRow {
  departmentId: number;
  departmentName: string;
  totalPendingAmount: number;
  pendingCount: number;
  pendingTeamLeadAmount: number;
  pendingFinanceAmount: number;
}

export interface DashboardResponse {
  departments: PendingByDepartmentRow[];
  grandTotalPending: number;
  grandTotalCount: number;
}

export const CATEGORIES: Category[] = ['TRAVEL', 'MEALS', 'SUPPLIES', 'SOFTWARE', 'OTHER'];

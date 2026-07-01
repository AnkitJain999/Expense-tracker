import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { Layout } from './components/Layout';
import { LoginPage } from './pages/LoginPage';
import { SubmitExpensePage } from './pages/SubmitExpensePage';
import { MyExpensesPage } from './pages/MyExpensesPage';
import { ApprovalQueuePage } from './pages/ApprovalQueuePage';
import { FinanceDashboardPage } from './pages/FinanceDashboardPage';
import { AdminUsersPage } from './pages/AdminUsersPage';

/** Sends an authenticated user to the landing page appropriate for their role. */
function RoleHome() {
  const { user, loading } = useAuth();
  if (loading) return <div className="centered">Loading…</div>;
  if (!user) return <Navigate to="/login" replace />;
  switch (user.role) {
    case 'EMPLOYEE':
      return <Navigate to="/submit" replace />;
    case 'TEAM_LEAD':
      return <Navigate to="/approvals" replace />;
    case 'FINANCE_MANAGER':
      return <Navigate to="/dashboard" replace />;
    case 'ADMIN':
      return <Navigate to="/admin/users" replace />;
    default:
      return <Navigate to="/login" replace />;
  }
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route path="/" element={<RoleHome />} />
          <Route element={<ProtectedRoute roles={['EMPLOYEE']} />}>
            <Route path="/submit" element={<SubmitExpensePage />} />
            <Route path="/my-expenses" element={<MyExpensesPage />} />
          </Route>
          <Route element={<ProtectedRoute roles={['TEAM_LEAD', 'FINANCE_MANAGER']} />}>
            <Route path="/approvals" element={<ApprovalQueuePage />} />
          </Route>
          <Route element={<ProtectedRoute roles={['FINANCE_MANAGER']} />}>
            <Route path="/dashboard" element={<FinanceDashboardPage />} />
          </Route>
          <Route element={<ProtectedRoute roles={['ADMIN']} />}>
            <Route path="/admin/users" element={<AdminUsersPage />} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

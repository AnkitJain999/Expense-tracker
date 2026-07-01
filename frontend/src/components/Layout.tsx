import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export function Layout() {
  const { user, logout, hasRole } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login');
  }

  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="brand">Expense Tracker</div>
        <nav className="nav">
          {hasRole('EMPLOYEE') && (
            <>
              <NavLink to="/submit">Submit</NavLink>
              <NavLink to="/my-expenses">My Expenses</NavLink>
            </>
          )}
          {hasRole('TEAM_LEAD', 'FINANCE_MANAGER') && <NavLink to="/approvals">Approvals</NavLink>}
          {hasRole('FINANCE_MANAGER') && <NavLink to="/dashboard">Dashboard</NavLink>}
        </nav>
        <div className="user-box">
          {user && (
            <span className="muted">
              {user.fullName} · {user.role.replace('_', ' ')}
            </span>
          )}
          <button className="link-btn" onClick={handleLogout}>
            Log out
          </button>
        </div>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  );
}

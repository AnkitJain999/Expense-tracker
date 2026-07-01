import { useEffect, useState, type FormEvent } from 'react';
import { createUser, fetchDepartments, fetchUsers } from '../api/admin';
import { apiErrorMessage } from '../api/client';
import { ROLES, type AdminUserResponse, type DepartmentSummary, type Role } from '../types';

export function AdminUsersPage() {
  const [users, setUsers] = useState<AdminUserResponse[]>([]);
  const [departments, setDepartments] = useState<DepartmentSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [listError, setListError] = useState<string | null>(null);

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [role, setRole] = useState<Role>('EMPLOYEE');
  const [departmentId, setDepartmentId] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  function reload() {
    setLoading(true);
    Promise.all([fetchUsers(), fetchDepartments()])
      .then(([userList, departmentList]) => {
        setUsers(userList);
        setDepartments(departmentList);
      })
      .catch((err) => setListError(apiErrorMessage(err)))
      .finally(() => setLoading(false));
  }

  useEffect(reload, []);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setFormError(null);
    setMessage(null);
    setSubmitting(true);
    try {
      const created = await createUser({
        email,
        password,
        fullName,
        role,
        departmentId: departmentId ? Number(departmentId) : null,
      });
      setMessage(`User "${created.fullName}" created.`);
      setEmail('');
      setPassword('');
      setFullName('');
      setRole('EMPLOYEE');
      setDepartmentId('');
      setUsers((prev) => [...prev, created].sort((a, b) => a.email.localeCompare(b.email)));
    } catch (err) {
      setFormError(apiErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="page">
      <h2>User Management</h2>

      <h3>Create user</h3>
      <form className="card form" onSubmit={handleSubmit}>
        <label>
          Full name
          <input type="text" value={fullName} onChange={(e) => setFullName(e.target.value)} required />
        </label>
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            minLength={8}
            required
          />
        </label>
        <label>
          Role
          <select value={role} onChange={(e) => setRole(e.target.value as Role)}>
            {ROLES.map((r) => (
              <option key={r} value={r}>
                {r.replace('_', ' ')}
              </option>
            ))}
          </select>
        </label>
        <label>
          Department (optional)
          <select value={departmentId} onChange={(e) => setDepartmentId(e.target.value)}>
            <option value="">None</option>
            {departments.map((d) => (
              <option key={d.id} value={d.id}>
                {d.name}
              </option>
            ))}
          </select>
        </label>
        {message && <div className="success">{message}</div>}
        {formError && <div className="error">{formError}</div>}
        <button type="submit" disabled={submitting}>
          {submitting ? 'Creating…' : 'Create user'}
        </button>
      </form>

      <h3>Existing users</h3>
      {listError && <div className="error">{listError}</div>}
      {loading ? (
        <p className="muted">Loading…</p>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>#</th>
              <th>Name</th>
              <th>Email</th>
              <th>Role</th>
              <th>Department</th>
              <th>Enabled</th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id}>
                <td>{u.id}</td>
                <td>{u.fullName}</td>
                <td>{u.email}</td>
                <td>{u.role.replace('_', ' ')}</td>
                <td>{u.departmentName ?? '—'}</td>
                <td>{u.enabled ? 'Yes' : 'No'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

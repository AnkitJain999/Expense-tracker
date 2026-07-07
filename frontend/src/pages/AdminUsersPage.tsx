import { useEffect, useState, type FormEvent } from 'react';
import { createUser, deleteUser, fetchDepartments, fetchUsers, updateUser } from '../api/admin';
import { apiErrorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { ROLES, type AdminUserResponse, type DepartmentSummary, type Role } from '../types';

export function AdminUsersPage() {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState<AdminUserResponse[]>([]);
  const [departments, setDepartments] = useState<DepartmentSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [listError, setListError] = useState<string | null>(null);

  const [editingId, setEditingId] = useState<number | null>(null);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [role, setRole] = useState<Role>('EMPLOYEE');
  const [departmentId, setDepartmentId] = useState('');
  const [enabled, setEnabled] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  const [busyId, setBusyId] = useState<number | null>(null);
  const [rowError, setRowError] = useState<string | null>(null);

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

  function resetForm() {
    setEditingId(null);
    setEmail('');
    setPassword('');
    setFullName('');
    setRole('EMPLOYEE');
    setDepartmentId('');
    setEnabled(true);
  }

  function startEdit(u: AdminUserResponse) {
    setMessage(null);
    setFormError(null);
    setEditingId(u.id);
    setEmail(u.email);
    setPassword('');
    setFullName(u.fullName);
    setRole(u.role);
    setDepartmentId(u.departmentId ? String(u.departmentId) : '');
    setEnabled(u.enabled);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setFormError(null);
    setMessage(null);
    setSubmitting(true);
    try {
      if (editingId) {
        const updated = await updateUser(editingId, {
          email,
          password: password || undefined,
          fullName,
          role,
          departmentId: departmentId ? Number(departmentId) : null,
          enabled,
        });
        setMessage(`User "${updated.fullName}" updated.`);
        setUsers((prev) =>
          prev.map((u) => (u.id === updated.id ? updated : u)).sort((a, b) => a.email.localeCompare(b.email)),
        );
        resetForm();
      } else {
        const created = await createUser({
          email,
          password,
          fullName,
          role,
          departmentId: departmentId ? Number(departmentId) : null,
        });
        setMessage(`User "${created.fullName}" created.`);
        setUsers((prev) => [...prev, created].sort((a, b) => a.email.localeCompare(b.email)));
        resetForm();
      }
    } catch (err) {
      setFormError(apiErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(u: AdminUserResponse) {
    if (!window.confirm(`Delete user "${u.fullName}"? This cannot be undone.`)) {
      return;
    }
    setRowError(null);
    setBusyId(u.id);
    try {
      await deleteUser(u.id);
      setUsers((prev) => prev.filter((x) => x.id !== u.id));
      if (editingId === u.id) {
        resetForm();
      }
    } catch (err) {
      setRowError(apiErrorMessage(err));
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="page">
      <h2>User Management</h2>

      <h3>{editingId ? 'Edit user' : 'Create user'}</h3>
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
          {editingId ? 'Password (leave blank to keep current)' : 'Password'}
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            minLength={8}
            required={!editingId}
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
        {editingId && (
          <label>
            <input type="checkbox" checked={enabled} onChange={(e) => setEnabled(e.target.checked)} />
            Enabled
          </label>
        )}
        {message && <div className="success">{message}</div>}
        {formError && <div className="error">{formError}</div>}
        <div className="action-buttons">
          <button type="submit" disabled={submitting}>
            {submitting ? 'Saving…' : editingId ? 'Update user' : 'Create user'}
          </button>
          {editingId && (
            <button type="button" className="link-btn" onClick={resetForm} disabled={submitting}>
              Cancel
            </button>
          )}
        </div>
      </form>

      <h3>Existing users</h3>
      {listError && <div className="error">{listError}</div>}
      {rowError && <div className="error">{rowError}</div>}
      {loading ? (
        <p className="muted">Loading…</p>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Role</th>
              <th>Department</th>
              <th>Enabled</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id}>
                <td>{u.fullName}</td>
                <td>{u.email}</td>
                <td>{u.role.replace('_', ' ')}</td>
                <td>{u.departmentName ?? '—'}</td>
                <td>{u.enabled ? 'Yes' : 'No'}</td>
                <td className="action-cell">
                  <div className="action-buttons">
                    <button className="link-btn" onClick={() => startEdit(u)} disabled={busyId === u.id}>
                      Edit
                    </button>
                    <button
                      className="btn-reject"
                      onClick={() => handleDelete(u)}
                      disabled={busyId === u.id || u.id === currentUser?.id}
                    >
                      Delete
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

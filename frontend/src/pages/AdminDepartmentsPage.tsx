import { useEffect, useState, type FormEvent } from 'react';
import {
  createDepartment,
  deleteDepartment,
  fetchDepartments,
  fetchUsers,
  updateDepartment,
} from '../api/admin';
import { apiErrorMessage } from '../api/client';
import type { AdminUserResponse, DepartmentSummary } from '../types';

export function AdminDepartmentsPage() {
  const [departments, setDepartments] = useState<DepartmentSummary[]>([]);
  const [users, setUsers] = useState<AdminUserResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [listError, setListError] = useState<string | null>(null);

  const [editingId, setEditingId] = useState<number | null>(null);
  const [name, setName] = useState('');
  const [teamLeadId, setTeamLeadId] = useState('');
  const [financeManagerId, setFinanceManagerId] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  const [busyId, setBusyId] = useState<number | null>(null);
  const [rowError, setRowError] = useState<string | null>(null);

  const teamLeads = users.filter((u) => u.role === 'TEAM_LEAD');
  const financeManagers = users.filter((u) => u.role === 'FINANCE_MANAGER');

  function reload() {
    setLoading(true);
    Promise.all([fetchDepartments(), fetchUsers()])
      .then(([departmentList, userList]) => {
        setDepartments(departmentList);
        setUsers(userList);
      })
      .catch((err) => setListError(apiErrorMessage(err)))
      .finally(() => setLoading(false));
  }

  useEffect(reload, []);

  function resetForm() {
    setEditingId(null);
    setName('');
    setTeamLeadId('');
    setFinanceManagerId('');
  }

  function startEdit(d: DepartmentSummary) {
    setMessage(null);
    setFormError(null);
    setEditingId(d.id);
    setName(d.name);
    setTeamLeadId(d.teamLeadId ? String(d.teamLeadId) : '');
    setFinanceManagerId(d.financeManagerId ? String(d.financeManagerId) : '');
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setFormError(null);
    setMessage(null);
    setSubmitting(true);
    const input = {
      name,
      teamLeadId: teamLeadId ? Number(teamLeadId) : null,
      financeManagerId: financeManagerId ? Number(financeManagerId) : null,
    };
    try {
      if (editingId) {
        const updated = await updateDepartment(editingId, input);
        setMessage(`Department "${updated.name}" updated.`);
        setDepartments((prev) =>
          prev.map((d) => (d.id === updated.id ? updated : d)).sort((a, b) => a.name.localeCompare(b.name)),
        );
      } else {
        const created = await createDepartment(input);
        setMessage(`Department "${created.name}" created.`);
        setDepartments((prev) => [...prev, created].sort((a, b) => a.name.localeCompare(b.name)));
      }
      resetForm();
    } catch (err) {
      setFormError(apiErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(d: DepartmentSummary) {
    if (!window.confirm(`Delete department "${d.name}"? This cannot be undone.`)) {
      return;
    }
    setRowError(null);
    setBusyId(d.id);
    try {
      await deleteDepartment(d.id);
      setDepartments((prev) => prev.filter((x) => x.id !== d.id));
      if (editingId === d.id) {
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
      <h2>Department Management</h2>

      <h3>{editingId ? 'Edit department' : 'Create department'}</h3>
      <form className="card form" onSubmit={handleSubmit}>
        <label>
          Name
          <input type="text" value={name} onChange={(e) => setName(e.target.value)} required />
        </label>
        <label>
          Team Lead (optional)
          <select value={teamLeadId} onChange={(e) => setTeamLeadId(e.target.value)}>
            <option value="">None</option>
            {teamLeads.map((u) => (
              <option key={u.id} value={u.id}>
                {u.fullName}
              </option>
            ))}
          </select>
        </label>
        <label>
          Finance Manager (optional)
          <select value={financeManagerId} onChange={(e) => setFinanceManagerId(e.target.value)}>
            <option value="">None</option>
            {financeManagers.map((u) => (
              <option key={u.id} value={u.id}>
                {u.fullName}
              </option>
            ))}
          </select>
        </label>
        {message && <div className="success">{message}</div>}
        {formError && <div className="error">{formError}</div>}
        <div className="action-buttons">
          <button type="submit" disabled={submitting}>
            {submitting ? 'Saving…' : editingId ? 'Update department' : 'Create department'}
          </button>
          {editingId && (
            <button type="button" className="link-btn" onClick={resetForm} disabled={submitting}>
              Cancel
            </button>
          )}
        </div>
      </form>

      <h3>Existing departments</h3>
      {listError && <div className="error">{listError}</div>}
      {rowError && <div className="error">{rowError}</div>}
      {loading ? (
        <p className="muted">Loading…</p>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Team Lead</th>
              <th>Finance Manager</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {departments.map((d) => (
              <tr key={d.id}>
                <td>{d.name}</td>
                <td>{d.teamLeadName ?? '—'}</td>
                <td>{d.financeManagerName ?? '—'}</td>
                <td className="action-cell">
                  <div className="action-buttons">
                    <button className="link-btn" onClick={() => startEdit(d)} disabled={busyId === d.id}>
                      Edit
                    </button>
                    <button className="btn-reject" onClick={() => handleDelete(d)} disabled={busyId === d.id}>
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

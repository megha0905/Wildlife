import { useState } from 'react';
import { authApi } from '../api/endpoints';
import type { Role, UserResponse } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { DataTable } from '../components/DataTable';
import { FormModal } from '../components/Form';
import { useToast } from '../components/Toasts';
import { Empty, ErrorBanner, PageHeader, fmtDateTime, label, useLoad } from '../components/ui';

const ROLES: Role[] = ['ADMIN', 'FOREST_OFFICER', 'RANGER', 'VET', 'ANALYST'];

/** Admin-only user management (auth-service /api/users). */
export function UsersPage() {
  const toast = useToast();
  const { user: me } = useAuth();
  const [q, setQ] = useState('');
  const [modal, setModal] = useState<'new' | UserResponse | null>(null);
  const [resetting, setResetting] = useState<UserResponse | null>(null);
  const list = useLoad(() => authApi.users({ q, size: 50 }), [q]);

  const toggle = async (u: UserResponse) => {
    try {
      await authApi.setActive(u.id, !u.active);
      toast(`${u.username} ${u.active ? 'deactivated' : 'activated'}`);
      list.reload();
    } catch (e) {
      toast(e instanceof Error ? e.message : 'Failed', 'error');
    }
  };

  return (
    <>
      <PageHeader title="Users" intro="Accounts that can sign in to WildGuard and their roles."
        actions={<button className="btn btn-primary" onClick={() => setModal('new')}>Add user</button>} />
      <div className="toolbar">
        <input className="input search" placeholder="Search username, name or email" value={q} onChange={(e) => setQ(e.target.value)} aria-label="Search users" />
      </div>
      <ErrorBanner error={list.error} />
      <DataTable<UserResponse>
        rows={list.data?.content ?? []} rowKey={(u) => u.id} onRowClick={setModal}
        empty={<Empty title="No users found" />}
        columns={[
          { header: 'Username', render: (u) => <strong>{u.username}</strong> },
          { header: 'Name', render: (u) => u.fullName },
          { header: 'Roles', render: (u) => u.roles.map(label).join(', ') },
          { header: 'Staff ID', render: (u) => u.staffId ?? '—' },
          { header: 'Last login', render: (u) => fmtDateTime(u.lastLoginAt) },
          { header: 'Active', render: (u) => (u.active ? 'Yes' : <span className="muted">No</span>) },
          { header: '', render: (u) => (
            <span className="row-actions" onClick={(e) => e.stopPropagation()}>
              <button className="link-btn" onClick={() => setResetting(u)}>Reset password</button>
              {u.id !== me?.id && <button className={`link-btn ${u.active ? 'danger' : ''}`} onClick={() => toggle(u)}>{u.active ? 'Deactivate' : 'Activate'}</button>}
            </span>
          ) },
        ]}
      />
      <FormModal
        open={modal !== null} onClose={() => setModal(null)}
        title={modal && modal !== 'new' ? `Edit ${modal.username}` : 'Add user'}
        submitLabel={modal && modal !== 'new' ? 'Save changes' : 'Add user'}
        initial={modal && modal !== 'new' ? { ...modal, role: modal.roles[0] } : { role: 'RANGER' }}
        fields={[
          ...(modal === 'new' ? [
            { name: 'username', label: 'Username', required: true },
            { name: 'password', label: 'Temporary password', required: true, hint: 'At least 8 characters.' },
          ] : []),
          { name: 'fullName', label: 'Full name', required: true },
          { name: 'email', label: 'Email', required: true },
          { name: 'role', label: 'Role', type: 'select' as const, required: true, enumValues: ROLES },
          { name: 'staffId', label: 'Linked staff ID', type: 'number' as const, hint: 'The staff record in Staff & resources, if any.' },
        ]}
        onSubmit={async (v) => {
          const { role, ...rest } = v;
          const body = { ...rest, roles: [role] };
          if (modal && modal !== 'new') await authApi.updateUser(modal.id, body);
          else await authApi.createUser(body);
          toast('User saved');
          list.reload();
        }}
      />
      <FormModal
        open={resetting !== null} onClose={() => setResetting(null)}
        title={`Reset password for ${resetting?.username ?? ''}`} submitLabel="Reset password"
        fields={[{ name: 'newPassword', label: 'New password', required: true, hint: 'At least 8 characters.' }]}
        onSubmit={async (v) => { await authApi.resetPassword(resetting!.id, String(v.newPassword)); toast('Password reset'); }}
      />
    </>
  );
}

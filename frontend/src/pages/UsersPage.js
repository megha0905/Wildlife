// Admin-only user management (auth-service /api/users).
import { authApi } from '../api/endpoints.js';
import { getUser } from '../auth/auth.js';
import { h } from '../components/dom.js';
import { openFormModal } from '../components/form.js';
import { dataTable } from '../components/table.js';
import { toast } from '../components/toasts.js';
import { empty, fmtDateTime, label, pageHeader, region, searchInput } from '../components/ui.js';

const ROLES = ['ADMIN', 'FOREST_OFFICER', 'RANGER', 'VET', 'ANALYST'];

export function UsersPage() {
  const me = getUser();
  let q = '';

  const toggle = async (u) => {
    try {
      await authApi.setActive(u.id, !u.active);
      toast(`${u.username} ${u.active ? 'deactivated' : 'activated'}`);
      list.reload();
    } catch (e) {
      toast(e instanceof Error ? e.message : 'Failed', 'error');
    }
  };

  const list = region(() => authApi.users({ q, size: 50 }), (data) => dataTable({
    rows: data.content,
    onRowClick: (u) => userForm(u),
    empty: empty('No users found'),
    columns: [
      { header: 'Username', render: (u) => h('strong', null, u.username) },
      { header: 'Name', render: (u) => u.fullName },
      { header: 'Roles', render: (u) => u.roles.map(label).join(', ') },
      { header: 'Staff ID', render: (u) => u.staffId ?? '—' },
      { header: 'Last login', render: (u) => fmtDateTime(u.lastLoginAt) },
      { header: 'Active', render: (u) => (u.active ? 'Yes' : h('span', { class: 'muted' }, 'No')) },
      { header: '', render: (u) => h('span', { class: 'row-actions', onClick: (e) => e.stopPropagation() },
        h('button', { class: 'link-btn', onClick: () => resetPassword(u) }, 'Reset password'),
        u.id !== me?.id && h('button', { class: `link-btn ${u.active ? 'danger' : ''}`, onClick: () => toggle(u) }, u.active ? 'Deactivate' : 'Activate')) },
    ],
  }));

  /** Add (user = null) or edit a user. */
  const userForm = (user) => openFormModal({
    title: user ? `Edit ${user.username}` : 'Add user',
    submitLabel: user ? 'Save changes' : 'Add user',
    initial: user ? { ...user, role: user.roles[0] } : { role: 'RANGER' },
    fields: [
      ...(user ? [] : [
        { name: 'username', label: 'Username', required: true },
        { name: 'password', label: 'Temporary password', required: true, hint: 'At least 8 characters.' },
      ]),
      { name: 'fullName', label: 'Full name', required: true },
      { name: 'email', label: 'Email', required: true },
      { name: 'role', label: 'Role', type: 'select', required: true, enumValues: ROLES },
      { name: 'staffId', label: 'Linked staff ID', type: 'number', hint: 'The staff record in Staff & resources, if any.' },
    ],
    onSubmit: async (v) => {
      const { role, ...rest } = v;
      const body = { ...rest, roles: [role] };
      if (user) await authApi.updateUser(user.id, body);
      else await authApi.createUser(body);
      toast('User saved');
      list.reload();
    },
  });

  const resetPassword = (u) => openFormModal({
    title: `Reset password for ${u.username}`,
    submitLabel: 'Reset password',
    fields: [{ name: 'newPassword', label: 'New password', required: true, hint: 'At least 8 characters.' }],
    onSubmit: async (v) => { await authApi.resetPassword(u.id, String(v.newPassword)); toast('Password reset'); },
  });

  return h('div', null,
    pageHeader('Users', 'Accounts that can sign in to WildGuard and their roles.',
      h('button', { class: 'btn btn-primary', onClick: () => userForm(null) }, 'Add user')),
    h('div', { class: 'toolbar' },
      searchInput({ placeholder: 'Search username, name or email', ariaLabel: 'Search users', onSearch: (v) => { q = v; list.reload(); } })),
    list.el);
}

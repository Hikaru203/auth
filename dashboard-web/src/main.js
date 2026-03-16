import './style.css'
import Chart from 'chart.js/auto'
import { api } from './api.js'

/** 
 * VIEW TEMPLATES
 */

const LoginTemplate = () => `
  <div class="login-page animate-in">
    <div class="login-card">
      <div class="logo"><span>SecurityHub</span></div>
      <p style="text-align: center; color: var(--text-muted); margin-bottom: 2rem;">Gateway to security mainframe</p>
      <form id="loginForm">
        <div class="form-group">
          <label class="label">Username</label>
          <input type="text" id="loginUser" class="input" placeholder="admin" required />
        </div>
        <div class="form-group">
          <label class="label">Password</label>
          <input type="password" id="loginPass" class="input" placeholder="••••••••" required />
        </div>
        <div class="form-group">
          <label class="label">Tenant Slug</label>
          <input type="text" id="loginTenant" class="input" placeholder="default" value="default" />
        </div>
        <button type="submit" class="btn" style="width: 100%; margin-top: 1rem;">Authenticate Identity</button>
      </form>
    </div>
  </div>
`;

const DashboardTemplate = () => `
  <div class="header-row animate-in">
    <div class="welcome-msg">
      <h1>System Overview</h1>
      <p>Real-time security analytics and management.</p>
    </div>
  </div>
  <div class="dashboard-grid">
    <div class="stat-card animate-in">
      <div class="stat-header"><span class="stat-label">Total Users</span><span>👤</span></div>
      <div id="statUsers" class="stat-value">...</div>
      <div class="stat-change up">↑ Active</div>
    </div>
    <div class="stat-card animate-in" style="animation-delay: 0.1s">
      <div class="stat-header"><span class="stat-label">System Performance</span><span>⚡</span></div>
      <div class="stat-value">99.9%</div>
      <div class="stat-change up">↑ Optimized</div>
    </div>
    <div class="stat-card animate-in" style="animation-delay: 0.2s">
      <div class="stat-header"><span class="stat-label">Active Tenants</span><span>🏢</span></div>
      <div id="statTenants" class="stat-value">...</div>
      <div class="stat-change up">Operational</div>
    </div>
  </div>
  <div class="charts-grid">
    <div class="chart-card animate-in" style="animation-delay: 0.3s">
      <div class="chart-header"><h3 class="modal-title">Authentication Traffic</h3><span class="text-muted">Last 7 Days</span></div>
      <div class="chart-container"><canvas id="authChart"></canvas></div>
    </div>
    <div class="chart-card animate-in" style="animation-delay: 0.4s">
      <div class="chart-header"><h3 class="modal-title">Role Distribution</h3></div>
      <div class="chart-container"><canvas id="roleChart"></canvas></div>
    </div>
  </div>
`;

const UsersTemplate = (usersPage) => `
  <div class="header-row animate-in">
    <div class="welcome-msg"><h1>User Management</h1><p>${usersPage.totalElements} security identities found.</p></div>
    <button class="btn" onclick="document.dispatchEvent(new CustomEvent('action:create-user'))">+ New Identity</button>
  </div>
  <div class="table-container animate-in">
    <table>
      <thead><tr><th>Username</th><th>Email</th><th>Status</th><th>Roles</th><th>Action</th></tr></thead>
      <tbody>
        ${usersPage.content.map(u => `
          <tr>
            <td><b>${u.username}</b></td>
            <td>${u.email}</td>
            <td><span class="badge ${u.status === 'ACTIVE' ? 'badge-success' : 'badge-danger'}">${u.status}</span></td>
            <td>${u.roles?.map(r => `<span class="badge badge-info" style="margin-right: 4px;">${r}</span>`).join('') || 'USER'}</td>
            <td>
              <div style="display: flex; gap: 4px;">
                <button class="btn" style="padding: 0.25rem 0.5rem; font-size: 0.8rem;" onclick="document.dispatchEvent(new CustomEvent('action:lock-user', {detail: '${u.id}'}))">Lock</button>
                <button class="btn" style="padding: 0.25rem 0.5rem; font-size: 0.8rem; background: var(--danger);" onclick="document.dispatchEvent(new CustomEvent('action:delete-user', {detail: '${u.id}'}))">Kill</button>
              </div>
            </td>
          </tr>`).join('')}
      </tbody>
    </table>
  </div>
`;

const RolesTemplate = (roles) => `
  <div class="header-row animate-in">
    <div class="welcome-msg"><h1>Role Definitions</h1><p>${roles.length} permission sets discovered.</p></div>
    <button class="btn" onclick="document.dispatchEvent(new CustomEvent('action:create-role'))">+ Define Role</button>
  </div>
  <div class="dashboard-grid animate-in">
    ${roles.map(r => `
      <div class="stat-card">
        <div class="stat-header"><b>${r.name}</b><span>🛡️</span></div>
        <p style="color: var(--text-muted); font-size: 0.85rem;">Permissions: ${r.permissions?.length || 0}</p>
        <div class="stat-change up" style="margin-top: 1rem;">Active Scope</div>
      </div>`).join('')}
  </div>
`;

const ProfileTemplate = (user) => `
  <div class="header-row animate-in">
    <div class="welcome-msg"><h1>My Security Profile</h1><p>Identity verification and credentials.</p></div>
  </div>
  <div class="profile-grid animate-in">
    <div class="settings-card" style="text-align: center;">
      <div class="profile-avatar" style="margin: 0 auto 1.5rem;">${user.username[0].toUpperCase()}</div>
      <h2>${user.username}</h2>
      <p class="text-muted">${user.email}</p>
      <div style="margin-top: 2rem;">
        <span class="badge badge-success">ID: ${user.id.substring(0, 8)}...</span>
      </div>
    </div>
    <div>
      <div class="settings-card">
        <h3>Security Settings</h3>
        <p class="text-muted" style="margin-bottom: 1.5rem;">Manage your credentials and MFA status.</p>
        <div class="form-group">
          <button class="btn" style="width: 100%; border: 1px solid var(--glass-border); background: transparent;">Change Password</button>
        </div>
        <div class="form-group">
          <button class="btn" style="width: 100%;" onclick="document.dispatchEvent(new CustomEvent('action:setup-2fa'))">Setup TOTP 2FA</button>
        </div>
      </div>
    </div>
  </div>
`;

const ApiKeysTemplate = (keysPage) => `
  <div class="header-row animate-in">
    <div class="welcome-msg"><h1>API Key Registry</h1><p>${keysPage.totalElements} tokens found.</p></div>
    <button class="btn" onclick="document.dispatchEvent(new CustomEvent('action:create-key'))">+ New Token</button>
  </div>
  <div class="table-container animate-in">
    <table>
      <thead><tr><th>Name</th><th>Prefix</th><th>Created</th><th>Status</th><th>Action</th></tr></thead>
      <tbody>
        ${keysPage.content.map(k => `
          <tr>
            <td><b>${k.name}</b></td>
            <td><code>${k.keyPrefix}...</code></td>
            <td>${new Date(k.createdAt).toLocaleDateString()}</td>
            <td><span class="badge ${!k.revoked ? 'badge-success' : 'badge-danger'}">${!k.revoked ? 'Valid' : 'Revoked'}</span></td>
            <td>
              <div style="display: flex; gap: 4px;">
                <button class="btn" style="padding: 0.25rem 0.5rem; font-size: 0.8rem;" onclick="document.dispatchEvent(new CustomEvent('action:refresh-key', {detail: '${k.id}'}))">Refresh</button>
                <button class="btn" style="padding: 0.25rem 0.5rem; font-size: 0.8rem; background: var(--danger);" onclick="document.dispatchEvent(new CustomEvent('action:revoke-key', {detail: '${k.id}'}))">Revoke</button>
              </div>
            </td>
          </tr>`).join('')}
      </tbody>
    </table>
  </div>
`;

const LogsTemplate = (logsPage) => `
  <div class="header-row animate-in">
    <div class="welcome-msg"><h1>Audit Logs</h1><p>${logsPage.totalElements} security events recorded.</p></div>
  </div>
  <div class="table-container animate-in">
    <table>
      <thead><tr><th>Timestamp</th><th>User</th><th>Action</th><th>IP</th><th>Status</th></tr></thead>
      <tbody>
        ${logsPage.content.map(l => `
          <tr>
            <td style="font-size: 0.8rem; color: var(--text-muted);">${new Date(l.timestamp).toLocaleString()}</td>
            <td><b>${l.username || 'system'}</b></td>
            <td style="color: var(--primary); font-weight: 500;">${l.action}</td>
            <td><code>${l.ipAddress}</code></td>
            <td><span class="badge ${l.status < 400 ? 'badge-success' : 'badge-danger'}">${l.status}</span></td>
          </tr>`).join('')}
      </tbody>
    </table>
  </div>
`;

const TenantsTemplate = (tenants) => `
  <div class="header-row animate-in">
    <div class="welcome-msg"><h1>Tenants</h1><p>${tenants.length} organization boundaries.</p></div>
    <button class="btn" onclick="document.dispatchEvent(new CustomEvent('action:create-tenant'))">+ Add Tenant</button>
  </div>
  <div class="dashboard-grid animate-in">
    ${tenants.map(t => `
      <div class="stat-card">
        <div class="stat-header"><b>${t.name}</b><span>🏢</span></div>
        <p style="color: var(--text-muted); font-size: 0.85rem;">Slug: ${t.slug}</p>
        <div class="stat-change ${t.active ? 'up' : 'down'}" style="margin-top: 1rem;">${t.active ? 'Active' : 'Inactive'}</div>
      </div>`).join('')}
  </div>
`;

const QrModalTemplate = (res) => `
  <div class="modal-overlay" id="qrModal">
    <div class="modal-content animate-in" onclick="event.stopPropagation()">
      <div class="modal-header">
        <h3 class="modal-title">Two-Factor Authentication Setup</h3>
        <button class="btn" style="padding: 0.2rem 0.5rem;" onclick="this.closest('.modal-overlay').remove()">✕</button>
      </div>
      <div style="text-align: center; padding: 1.5rem;">
        <p class="text-muted" style="margin-bottom: 1.5rem;">Scan this QR code with your authenticator app.</p>
        <div style="background: white; padding: 1rem; display: inline-block; border-radius: 1rem; margin-bottom: 1.5rem;">
          <img src="${res.qrCodeUrl}" alt="2FA QR Code" style="display: block;" />
        </div>
        <div class="form-group" style="text-align: left;">
          <label class="label">Manual Entry Key</label>
          <code style="display: block; padding: 0.75rem; background: var(--glass-bg); border-radius: 0.5rem; border: 1px solid var(--glass-border); word-break: break-all;">${res.secret}</code>
        </div>
        <div class="form-group" style="text-align: left; margin-top: 1rem;">
          <label class="label">Verification Code</label>
          <input type="text" id="totpVerifyCode" class="input" placeholder="000000" maxlength="6" />
        </div>
        <button class="btn" id="confirm2faBtn" style="width: 100%; margin-top: 1.5rem;">Verify & Enable</button>
      </div>
    </div>
  </div>
`;

const FormModalTemplate = (title, fields, actionId) => `
  <div class="modal-overlay" onclick="this.remove()">
    <div class="modal-content animate-in" onclick="event.stopPropagation()">
      <div class="modal-header">
        <h3 class="modal-title">${title}</h3>
        <button class="btn" style="padding: 0.2rem 0.5rem;" onclick="this.closest('.modal-overlay').remove()">✕</button>
      </div>
      <form id="${actionId}Form" style="padding: 1.5rem;">
        ${fields.map(f => `
          <div class="form-group">
            <label class="label">${f.label}</label>
            <input type="${f.type || 'text'}" id="${f.id}" class="input" placeholder="${f.placeholder || ''}" ${f.required ? 'required' : ''} value="${f.value || ''}" />
          </div>
        `).join('')}
        <button type="submit" class="btn" style="width: 100%; margin-top: 1rem;">Save Changes</button>
      </form>
    </div>
  </div>
`;

/**
 * CORE LOGIC
 */

const app = document.getElementById('app');
const mainContainer = document.getElementById('mainViewContainer');

let currentView = 'dashboard';

const showLoading = () => {
  if (mainContainer) {
    mainContainer.innerHTML = `<div class="loading-state animate-in"><div class="spinner"></div><p>Syncing with security mainframe...</p></div>`;
  }
};

const renderCharts = (trafficData = [], roleData = []) => {
  const canvasAuth = document.getElementById('authChart');
  if (canvasAuth) {
    const ctxAuth = canvasAuth.getContext('2d');
    
    // Sort and format data
    const labels = trafficData.map(d => new Date(d.date).toLocaleDateString(undefined, { weekday: 'short' }));
    const successData = trafficData.map(d => d.success);
    const failureData = trafficData.map(d => d.failure);

    new Chart(ctxAuth, {
      type: 'line',
      data: {
        labels: labels.length ? labels : ['None'],
        datasets: [
          { label: 'Success', data: successData.length ? successData : [0], borderColor: '#10b981', backgroundColor: 'rgba(16, 185, 129, 0.1)', fill: true, tension: 0.4 },
          { label: 'Failure', data: failureData.length ? failureData : [0], borderColor: '#f43f5e', backgroundColor: 'rgba(244, 63, 94, 0.1)', fill: true, tension: 0.4 }
        ]
      },
      options: {
        responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } },
        scales: { y: { beginAtZero: true, grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: '#94a3b8' } }, x: { grid: { display: false }, ticks: { color: '#94a3b8' } } }
      }
    });
  }

  const canvasRole = document.getElementById('roleChart');
  if (canvasRole) {
    const ctxRole = canvasRole.getContext('2d');
    
    const labels = roleData.map(d => d.roleName);
    const counts = roleData.map(d => d.count);

    new Chart(ctxRole, {
      type: 'doughnut',
      data: {
        labels: labels.length ? labels : ['Unknown'],
        datasets: [{ data: counts.length ? counts : [1], backgroundColor: ['#c084fc', '#818cf8', '#6366f1', '#10b981', '#f59e0b'], borderWidth: 0, hoverOffset: 10 }]
      },
      options: { responsive: true, maintainAspectRatio: false, cutout: '75%', plugins: { legend: { position: 'bottom', labels: { color: '#94a3b8', boxWidth: 12, padding: 15 } } } }
    });
  }
};

const switchView = async (view) => {
  if (!api.token && view !== 'login') {
    switchView('login');
    return;
  }

  currentView = view;
  if (view === 'login') {
    document.body.classList.add('auth-mode');
    app.innerHTML = LoginTemplate();
    handleLoginEvents();
    return;
  }

  document.body.classList.remove('auth-mode');
  if (!document.querySelector('.sidebar')) {
    location.reload();
    return;
  }

  showLoading();
  const navItems = document.querySelectorAll('.nav-item');
  navItems.forEach(item => item.classList.toggle('active', item.dataset.view === view));

  try {
    switch (view) {
      case 'dashboard':
        mainContainer.innerHTML = DashboardTemplate();
        // Concurrently fetch counts and chart data
        const [usersPage, tenants, traffic, roles] = await Promise.all([
            api.getUsers(), 
            api.getTenants(),
            api.getTrafficStats(7),
            api.getRoleDistribution()
        ]);
        
        document.getElementById('statUsers').innerText = usersPage.totalElements || usersPage.length;
        document.getElementById('statTenants').innerText = tenants.length;
        
        renderCharts(traffic.points, roles.distribution);
        break;
      case 'users':
        mainContainer.innerHTML = UsersTemplate(await api.getUsers());
        break;
      case 'roles':
        mainContainer.innerHTML = RolesTemplate(await api.getRoles());
        break;
      case 'api-keys':
        mainContainer.innerHTML = ApiKeysTemplate(await api.getApiKeys());
        break;
      case 'audit-logs':
        mainContainer.innerHTML = LogsTemplate(await api.getAuditLogs());
        break;
      case 'tenants':
        mainContainer.innerHTML = TenantsTemplate(await api.getTenants());
        break;
      case 'profile':
        mainContainer.innerHTML = ProfileTemplate(await api.getMe());
        break;
    }
  } catch (err) {
    console.error('SwitchView Error:', err);
    mainContainer.innerHTML = `<div class="loading-state"><p style="color: var(--danger)">Connection Failure: ${err.message}</p></div>`;
  }
};

const handleLoginEvents = () => {
  const form = document.getElementById('loginForm');
  form?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const credentials = {
      username: document.getElementById('loginUser').value,
      password: document.getElementById('loginPass').value,
      tenantSlug: document.getElementById('loginTenant').value,
      totpCode: "000000"
    };
    try {
      const res = await api.login(credentials);
      api.setToken(res.accessToken);
      window.location.reload();
    } catch (err) {
      alert(`Authentication Error: ${err.message}`);
    }
  });
};

// Global Listeners
window.addEventListener('unauthorized', () => switchView('login'));

document.addEventListener('action:setup-2fa', async () => {
  try {
    const res = await api.setup2fa();
    const modalDiv = document.createElement('div');
    modalDiv.innerHTML = QrModalTemplate(res);
    const modal = modalDiv.firstElementChild;
    document.body.appendChild(modal);

    modal.querySelector('#confirm2faBtn').onclick = async () => {
      const code = modal.querySelector('#totpVerifyCode').value;
      try {
        await api.verify2fa({ secret: res.secret, code });
        alert('2FA Enabled Successfully!');
        modal.remove();
      } catch (err) { alert(err.message); }
    };
  } catch (err) { alert(err.message); }
});

document.addEventListener('action:create-user', () => {
  const fields = [
    { id: 'uUser', label: 'Username', required: true },
    { id: 'uEmail', label: 'Email', type: 'email', required: true },
    { id: 'uPass', label: 'Password', type: 'password', required: true },
    { id: 'uFirst', label: 'First Name' },
    { id: 'uLast', label: 'Last Name' }
  ];
  const modalDiv = document.createElement('div');
  modalDiv.innerHTML = FormModalTemplate('Create Security Identity', fields, 'createUser');
  document.body.appendChild(modalDiv.firstElementChild);

  document.getElementById('createUserForm').onsubmit = async (e) => {
    e.preventDefault();
    const data = {
      username: document.getElementById('uUser').value,
      email: document.getElementById('uEmail').value,
      password: document.getElementById('uPass').value,
      firstName: document.getElementById('uFirst').value,
      lastName: document.getElementById('uLast').value
    };
    try {
      await api.createUser(data);
      alert('User created!');
      switchView('users');
      document.querySelector('.modal-overlay').remove();
    } catch (err) { alert(err.message); }
  };
});

document.addEventListener('action:lock-user', async (e) => {
  if (confirm('Toggle account lock?')) {
    try {
      await api.lockUser(e.detail);
      switchView('users');
    } catch (err) { 
      try { await api.unlockUser(e.detail); switchView('users'); } catch (e2) { alert(err.message); }
    }
  }
});

document.addEventListener('action:delete-user', async (e) => {
  if (confirm('DEACTIVATE THIS IDENTITY? This is a soft-delete.')) {
    try {
      await api.deleteUser(e.detail);
      switchView('users');
    } catch (err) { alert(err.message); }
  }
});

document.addEventListener('action:refresh-key', async (e) => {
  if (confirm('Refresh this key? The current one will be REVOKED and a new one generated.')) {
    try {
      const res = await api.refreshApiKey(e.detail);
      alert(`KEY REFRESHED!\nNew Prefix: ${res.keyPrefix}\n\nSAVE THIS NEW KEY:\n${res.secretKey}`);
      switchView('api-keys');
    } catch (err) { alert(err.message); }
  }
});

document.addEventListener('action:revoke-key', async (e) => {
  if (confirm('Revoke this API token permanently?')) {
    try {
      await api.revokeApiKey(e.detail);
      switchView('api-keys');
    } catch (err) { alert(err.message); }
  }
});

document.addEventListener('action:create-key', () => {
  const fields = [
    { id: 'kName', label: 'Key Name', placeholder: 'e.g. Production API', required: true },
    { id: 'kExpire', label: 'Expires (Days)', type: 'number', value: '30' }
  ];
  const modalDiv = document.createElement('div');
  modalDiv.innerHTML = FormModalTemplate('Issue New API Token', fields, 'createKey');
  document.body.appendChild(modalDiv.firstElementChild);

  document.getElementById('createKeyForm').onsubmit = async (e) => {
    e.preventDefault();
    const data = {
      name: document.getElementById('kName').value,
      expiresInDays: parseInt(document.getElementById('kExpire').value)
    };
    try {
      const res = await api.generateApiKey(data);
      alert(`KEY ISSUED!\nPrefix: ${res.keyPrefix}\n\nSAVE THIS KEY NOW:\n${res.secretKey}\n\nIt will never be shown again.`);
      switchView('api-keys');
      document.querySelector('.modal-overlay').remove();
    } catch (err) { alert(err.message); }
  };
});

document.addEventListener('action:create-role', () => {
  const fields = [
    { id: 'rName', label: 'Role Name', placeholder: 'e.g. ROLE_AUDITOR', required: true },
    { id: 'rDesc', label: 'Description' }
  ];
  const modalDiv = document.createElement('div');
  modalDiv.innerHTML = FormModalTemplate('Define New Security Role', fields, 'createRole');
  document.body.appendChild(modalDiv.firstElementChild);

  document.getElementById('createRoleForm').onsubmit = async (e) => {
    e.preventDefault();
    const data = {
      name: document.getElementById('rName').value,
      description: document.getElementById('rDesc').value
    };
    try {
      await api.createRole(data);
      alert('Role defined!');
      switchView('roles');
      document.querySelector('.modal-overlay').remove();
    } catch (err) { alert(err.message); }
  };
});

document.addEventListener('action:create-tenant', () => {
  const fields = [
    { id: 'tName', label: 'Organization Name', required: true },
    { id: 'tSlug', label: 'System Slug', placeholder: 'e.g. acme-corp', required: true },
    { id: 'tContact', label: 'Contact Email', type: 'email' }
  ];
  const modalDiv = document.createElement('div');
  modalDiv.innerHTML = FormModalTemplate('Onboard New Tenant', fields, 'createTenant');
  document.body.appendChild(modalDiv.firstElementChild);

  document.getElementById('createTenantForm').onsubmit = async (e) => {
    e.preventDefault();
    const data = {
      name: document.getElementById('tName').value,
      slug: document.getElementById('tSlug').value,
      contactEmail: document.getElementById('tContact').value
    };
    try {
      await api.createTenant(data);
      alert('Tenant onboarded!');
      switchView('tenants');
      document.querySelector('.modal-overlay').remove();
    } catch (err) { alert(err.message); }
  };
});

const initMenu = () => {
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
        if (item.dataset.view) {
            item.addEventListener('click', () => switchView(item.dataset.view));
        }
    });

    document.getElementById('logoutBtn')?.addEventListener('click', () => {
      api.setToken(null);
      window.location.reload();
    });

    document.querySelector('.user-profile')?.addEventListener('click', () => switchView('profile'));
};

if (!api.token) {
  switchView('login');
} else {
  initMenu();
  switchView('dashboard');
}

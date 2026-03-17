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
        <div class="form-group">
          <label class="label">2FA Code (Optional)</label>
          <input type="text" id="loginTotp" class="input" placeholder="000000" maxlength="6" />
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
    <div class="stat-card animate-in" style="animation-delay: 0.25s">
      <div class="stat-header"><span class="stat-label">2FA Adoption</span><span>🛡️</span></div>
      <div id="stat2fa" class="stat-value">...</div>
      <div class="stat-change" id="stat2faMsg">Security Health</div>
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

const PaginationTemplate = (pageData, viewName) => `
  <div class="pagination animate-in" style="display: flex; gap: 1rem; align-items: center; justify-content: flex-end; margin-top: 1.5rem;">
    <span class="text-muted" style="font-size: 0.85rem;">Page ${pageData.page + 1} of ${pageData.totalPages} (${pageData.totalElements} total items)</span>
    <div style="display: flex; gap: 0.5rem;">
      <button class="btn" style="padding: 0.25rem 0.75rem; background: var(--glass-bg); border: 1px solid var(--glass-border);" 
        ${pageData.page === 0 ? 'disabled' : ''} 
        onclick="document.dispatchEvent(new CustomEvent('action:page-change', {detail: {view: '${viewName}', page: ${pageData.page - 1}}}))">
        Previous
      </button>
      <button class="btn" style="padding: 0.25rem 0.75rem; background: var(--glass-bg); border: 1px solid var(--glass-border);" 
        ${pageData.last ? 'disabled' : ''} 
        onclick="document.dispatchEvent(new CustomEvent('action:page-change', {detail: {view: '${viewName}', page: ${pageData.page + 1}}}))">
        Next
      </button>
    </div>
  </div>
`;

const UsersTemplate = (usersPage) => `
  <div class="header-row animate-in">
    <div class="welcome-msg"><h1>Security Identities</h1><p>${usersPage.totalElements} records managed.</p></div>
    <button class="btn" onclick="document.dispatchEvent(new CustomEvent('action:create-user'))"><span style="margin-right: 4px;">+</span> New Identity</button>
  </div>
  <div class="table-container animate-in">
    <table>
      <thead>
        <tr>
          <th>Identity</th>
          <th>Contact</th>
          <th>MFA Status</th>
          <th>Account Status</th>
          <th>Security Roles</th>
          <th>Operations</th>
        </tr>
      </thead>
      <tbody>
        ${usersPage.content.map(u => `
          <tr>
            <td>
              <div style="font-weight: 600; color: var(--text-primary);">${u.username}</div>
              <div style="font-size: 0.75rem; color: var(--text-muted);">${u.id.substring(0,8)}...</div>
            </td>
            <td>
              <div>${u.email}</div>
              <div style="font-size: 0.75rem; color: var(--text-muted);">${u.phone || 'No phone'}</div>
            </td>
            <td><span class="badge ${u.totpEnabled ? 'badge-success' : 'badge-danger'}">${u.totpEnabled ? 'PROTECTED' : 'VULNERABLE'}</span></td>
            <td><span class="badge ${u.status === 'ACTIVE' ? 'badge-success' : 'badge-danger'}">${u.status}</span></td>
            <td>
              <div style="display: flex; flex-wrap: wrap; gap: 4px; max-width: 200px;">
                ${u.roles?.map(r => `<span class="badge badge-info" style="font-size: 0.7rem;">${r}</span>`).join('') || '<span class="text-muted">No Roles</span>'}
              </div>
            </td>
            <td>
              <div style="display: flex; gap: 6px;">
                <button class="btn" style="padding: 0.3rem 0.6rem; font-size: 0.75rem; background: var(--primary);" 
                  onclick="document.dispatchEvent(new CustomEvent('action:manage-user-roles', {detail: '${u.id}'}))">Roles</button>
                <button class="btn" style="padding: 0.3rem 0.6rem; font-size: 0.75rem; background: var(--glass-bg); border: 1px solid var(--glass-border);" 
                  onclick="document.dispatchEvent(new CustomEvent('action:edit-user', {detail: '${u.id}'}))">Edit</button>
                <button class="btn" style="padding: 0.3rem 0.6rem; font-size: 0.75rem; background: var(--danger);" 
                   onclick="document.dispatchEvent(new CustomEvent('action:delete-user', {detail: '${u.id}'}))">Kill</button>
              </div>
            </td>
          </tr>`).join('')}
      </tbody>
    </table>
  </div>
  ${PaginationTemplate(usersPage, 'users')}
`;

const RolesTemplate = (rolesPage) => `
  <div class="header-row animate-in">
    <div class="welcome-msg"><h1>Role Definitions</h1><p>${rolesPage.totalElements} permission sets discovered.</p></div>
    <button class="btn" onclick="document.dispatchEvent(new CustomEvent('action:create-role'))">+ Define Role</button>
  </div>
  <div class="dashboard-grid animate-in" style="grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));">
    ${rolesPage.content.map(r => `
      <div class="stat-card">
        <div class="stat-header"><b>${r.name}</b><span>🛡️</span></div>
        <p style="color: var(--text-muted); font-size: 0.85rem; min-height: 2rem;">${r.description || 'No description provided.'}</p>
        <p style="color: var(--text-muted); font-size: 0.75rem;">Permissions: ${r.permissions?.length || 0}</p>
        <div style="display: flex; gap: 0.5rem; justify-content: flex-end; margin-top: 1rem;">
          <div class="stat-change up" style="margin-right: auto;">${r.systemRole ? '🛡️ System Scope' : 'Active Scope'}</div>
          <button class="btn" style="padding: 0.25rem 0.5rem; font-size: 0.75rem; background: var(--glass-bg); border: 1px solid var(--glass-border);" 
            onclick="document.dispatchEvent(new CustomEvent('action:edit-role', {detail: '${r.id}'}))">Edit</button>
          <button class="btn" style="padding: 0.25rem 0.5rem; font-size: 0.75rem;" 
            onclick="document.dispatchEvent(new CustomEvent('action:manage-role-permissions', {detail: '${r.id}'}))">Permissions</button>
        </div>
      </div>`).join('')}
  </div>
  ${PaginationTemplate(rolesPage, 'roles')}
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
  ${PaginationTemplate(keysPage, 'api-keys')}
`;

const PermissionsTemplate = (permissions) => `
  <div class="header-row animate-in">
    <div class="welcome-msg"><h1>Permission Scopes</h1><p>${permissions.length} granular access tokens.</p></div>
  </div>
  <div class="dashboard-grid animate-in">
    ${permissions.map(p => `
      <div class="stat-card">
        <div class="stat-header"><b>${p.name}</b><span>🔑</span></div>
        <p style="color: var(--text-muted); font-size: 0.85rem;">${p.description || 'No description'}</p>
        <div class="stat-change up" style="margin-top: 1rem;">System Scope</div>
      </div>`).join('')}
  </div>
`;

const LogsTemplate = (logsPage) => `
  <div class="header-row animate-in">
    <div class="welcome-msg"><h1>Audit Logs</h1><p>${logsPage.totalElements} security events recorded.</p></div>
  </div>
  <div class="dashboard-grid animate-in" style="grid-template-columns: 1fr; margin-bottom: 1rem;">
    <div class="stat-card" style="padding: 1rem;">
      <form id="auditFilterForm" style="display: flex; gap: 1rem; align-items: flex-end;">
        <div class="form-group" style="margin: 0; flex: 1;">
          <label class="label">Username</label>
          <input type="text" id="filterUser" class="input" placeholder="Search user..." />
        </div>
        <div class="form-group" style="margin: 0; flex: 1;">
          <label class="label">Action</label>
          <input type="text" id="filterAction" class="input" placeholder="e.g. LOGIN_SUCCESS" />
        </div>
        <div class="form-group" style="margin: 0; width: 120px;">
          <label class="label">Status</label>
          <input type="number" id="filterStatus" class="input" placeholder="200" />
        </div>
        <button type="submit" class="btn">Filter</button>
        <button type="button" class="btn" style="background: transparent; border: 1px solid var(--glass-border);" onclick="document.dispatchEvent(new CustomEvent('action:clear-audit-filters'))">Clear</button>
      </form>
    </div>
  </div>
  <div class="table-container animate-in">
    <table>
      <thead><tr><th>Timestamp</th><th>User</th><th>Action</th><th>IP</th><th>Status</th></tr></thead>
      <tbody>
        ${logsPage.content.map(l => `
          <tr>
            <td style="font-size: 0.8rem; color: var(--text-muted);">${new Date(l.createdAt).toLocaleString()}</td>
            <td><b>${l.username || 'system'}</b></td>
            <td style="color: var(--primary); font-weight: 500;">${l.action}</td>
            <td><code>${l.ipAddress || '0.0.0.0'}</code></td>
            <td><span class="badge ${l.statusCode < 400 ? 'badge-success' : 'badge-danger'}">${l.statusCode || 'N/A'}</span></td>
          </tr>`).join('')}
      </tbody>
    </table>
  </div>
  ${PaginationTemplate(logsPage, 'audit-logs')}
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
let currentCache = { users: [], roles: [], permissions: [] };

const showLoading = () => {
  if (mainContainer) {
    mainContainer.innerHTML = `<div class="loading-state animate-in"><div class="spinner"></div><p>Syncing with security mainframe...</p></div>`;
  }
};

const notify = (message, type = 'success') => {
  let overlay = document.querySelector('.notify-overlay');
  if (!overlay) {
    overlay = document.createElement('div');
    overlay.className = 'notify-overlay';
    document.body.appendChild(overlay);
  }

  const icon = type === 'success' ? '✅' : type === 'error' ? '❌' : '⚠️';
  const title = type === 'success' ? 'Operational Success' : type === 'error' ? 'Security Breach' : 'System Alert';
  
  const card = document.createElement('div');
  card.className = `notify-card ${type}`;
  card.innerHTML = `
    <div class="notify-icon">${icon}</div>
    <div class="notify-content">
      <div class="notify-title">${title}</div>
      <div class="notify-msg">${message}</div>
    </div>
    <button class="notify-close">✕</button>
  `;

  overlay.appendChild(card);

  const close = () => {
    card.classList.add('notify-out');
    setTimeout(() => card.remove(), 400);
  };

  card.querySelector('.notify-close').onclick = close;

  if (type === 'success') {
    setTimeout(close, 4000);
  }
};

const askConfirm = (title, message) => {
  return new Promise((resolve) => {
    const modalDiv = document.createElement('div');
    modalDiv.className = 'modal-overlay';
    modalDiv.innerHTML = `
      <div class="modal-content animate-in" style="max-width: 400px; text-align: center; border-top: 4px solid var(--danger);">
        <div style="padding: 2rem;">
          <div style="font-size: 2.5rem; margin-bottom: 1rem;">⚠️</div>
          <h3 style="margin-bottom: 0.5rem;">${title}</h3>
          <p class="text-muted" style="font-size: 0.9rem; margin-bottom: 2rem;">${message}</p>
          <div style="display: flex; gap: 1rem;">
            <button class="btn" style="flex: 1; background: var(--glass-bg); border: 1px solid var(--glass-border);" id="confirmCancel">Cancel</button>
            <button class="btn" style="flex: 1; background: var(--danger);" id="confirmProceed">Proceed</button>
          </div>
        </div>
      </div>
    `;
    document.body.appendChild(modalDiv);

    modalDiv.querySelector('#confirmCancel').onclick = () => {
      modalDiv.remove();
      resolve(false);
    };
    modalDiv.querySelector('#confirmProceed').onclick = () => {
      modalDiv.remove();
      resolve(true);
    };
  });
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
    const page = window.pagingState[view] || 0;
    const size = 20;

    switch (view) {
      case 'dashboard': {
        mainContainer.innerHTML = DashboardTemplate();
        // Concurrently fetch counts and chart data
        const [usersPage, tenants, traffic, roles] = await Promise.all([
            api.getUsers(0, 50), 
            api.getTenants(),
            api.getTrafficStats(7),
            api.getRoleDistribution()
        ]);
        
        document.getElementById('statUsers').innerText = usersPage.totalElements || usersPage.length;
        document.getElementById('statTenants').innerText = tenants.length;
        
        // Calculate 2FA Adoption
        const totpEnabledCount = (usersPage.content || []).filter(u => u.totpEnabled).length;
        const totalUsers = (usersPage.content || []).length;
        const adoptionRate = totalUsers > 0 ? Math.round((totpEnabledCount / totalUsers) * 100) : 0;
        const stat2fa = document.getElementById('stat2fa');
        if (stat2fa) {
            stat2fa.innerText = `${adoptionRate}%`;
            const msg = document.getElementById('stat2faMsg');
            msg.className = `stat-change ${adoptionRate > 50 ? 'up' : 'down'}`;
            msg.innerText = adoptionRate > 50 ? '🛡️ Strong' : '⚠️ Critical';
        }
        
        renderCharts(traffic.points, roles.distribution);
        break;
      }
      case 'users': {
        const usersPage = await api.getUsers(page, size);
        currentCache.users = usersPage.content;
        mainContainer.innerHTML = UsersTemplate(usersPage);
        break;
      }
      case 'roles': {
        const rolesPage = await api.getRoles(page, size);
        currentCache.roles = rolesPage.content;
        mainContainer.innerHTML = RolesTemplate(rolesPage);
        break;
      }
      case 'api-keys':
        mainContainer.innerHTML = ApiKeysTemplate(await api.getApiKeys(page, size));
        break;
      case 'audit-logs': {
        const filters = { ...(window.auditFilters || {}), page, size };
        const logsPage = await api.getAuditLogs(filters);
        mainContainer.innerHTML = LogsTemplate(logsPage);
        handleAuditFilterEvents();
        break;
      }
      case 'permissions': {
        const perms = await api.getPermissions();
        currentCache.permissions = perms;
        mainContainer.innerHTML = PermissionsTemplate(perms);
        break;
      }
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

window.pagingState = {};

document.addEventListener('action:page-change', (e) => {
    window.pagingState[e.detail.view] = e.detail.page;
    switchView(e.detail.view);
});

const handleAuditFilterEvents = () => {
    const form = document.getElementById('auditFilterForm');
    if (!form) return;

    // Prefill filters
    if (window.auditFilters) {
        if (window.auditFilters.username) document.getElementById('filterUser').value = window.auditFilters.username;
        if (window.auditFilters.action) document.getElementById('filterAction').value = window.auditFilters.action;
        if (window.auditFilters.statusCode) document.getElementById('filterStatus').value = window.auditFilters.statusCode;
    }

    form.addEventListener('submit', (e) => {
        e.preventDefault();
        window.auditFilters = {
            username: document.getElementById('filterUser').value,
            action: document.getElementById('filterAction').value,
            statusCode: document.getElementById('filterStatus').value
        };
        switchView('audit-logs');
    });
};

document.addEventListener('action:clear-audit-filters', () => {
    window.auditFilters = {};
    switchView('audit-logs');
});

const handleLoginEvents = () => {
  const form = document.getElementById('loginForm');
  form?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const credentials = {
      username: document.getElementById('loginUser').value,
      password: document.getElementById('loginPass').value,
      tenantSlug: document.getElementById('loginTenant').value,
      totpCode: document.getElementById('loginTotp').value || "000000"
    };
    try {
      const res = await api.login(credentials);
      api.setToken(res.accessToken, credentials.tenantSlug);
      window.location.reload();
    } catch (err) {
      notify(`Authentication Error: ${err.message}`, 'error');
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
        notify('2FA Enabled Successfully!');
        modal.remove();
      } catch (err) { notify(err.message, 'error'); }
    };
  } catch (err) { notify(err.message, 'error'); }
});

document.addEventListener('action:create-user', async () => {
  try {
    const rolesPage = await api.getRoles(0, 100);
    const allRoles = rolesPage.content;

    const modalDiv = document.createElement('div');
    modalDiv.className = 'modal-overlay';
    modalDiv.onclick = () => modalDiv.remove();
    modalDiv.innerHTML = `
      <div class="modal-content animate-in" onclick="event.stopPropagation()">
        <div class="modal-header">
          <h3 class="modal-title">Initialize New Identity</h3>
          <button class="btn" style="padding: 0.2rem 0.5rem;" onclick="this.closest('.modal-overlay').remove()">✕</button>
        </div>
        <form id="createUserForm" style="padding: 1.5rem;">
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
            <div class="form-group"><label class="label">Username</label><input type="text" id="uUser" class="input" required /></div>
            <div class="form-group"><label class="label">Email</label><input type="email" id="uEmail" class="input" required /></div>
            <div class="form-group"><label class="label">Password</label><input type="password" id="uPass" class="input" required /></div>
            <div class="form-group"><label class="label">Phone</label><input type="text" id="uPhone" class="input" /></div>
            <div class="form-group"><label class="label">First Name</label><input type="text" id="uFirst" class="input" /></div>
            <div class="form-group"><label class="label">Last Name</label><input type="text" id="uLast" class="input" /></div>
          </div>
          <div class="form-group" style="margin-top: 1rem;">
            <label class="label">Primary Security Roles</label>
            <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 0.5rem; background: var(--glass-bg); padding: 1rem; border-radius: 8px;">
              ${allRoles.map(r => `
                <label style="display: flex; align-items: center; gap: 0.5rem; font-size: 0.85rem; cursor: pointer;">
                  <input type="checkbox" data-role-id="${r.id}" name="roleCheck" />
                  <span>${r.name}</span>
                </label>
              `).join('')}
            </div>
          </div>
          <button type="submit" class="btn" id="submitUserBtn" style="width: 100%; margin-top: 1.5rem;">Create Identity</button>
        </form>
      </div>
    `;
    document.body.appendChild(modalDiv);

    document.getElementById('createUserForm').onsubmit = async (e) => {
      e.preventDefault();
      const btn = document.getElementById('submitUserBtn');
      const originalText = btn.innerText;
      
      const roleIds = Array.from(document.querySelectorAll('input[name="roleCheck"]:checked')).map(i => i.dataset.roleId);
      const data = {
        tenantSlug: localStorage.getItem('tenant_slug') || 'default',
        username: document.getElementById('uUser').value,
        email: document.getElementById('uEmail').value,
        password: document.getElementById('uPass').value,
        firstName: document.getElementById('uFirst').value,
        lastName: document.getElementById('uLast').value,
        phone: document.getElementById('uPhone').value
      };

      try {
        btn.disabled = true;
        btn.innerText = 'Initializing...';
        const user = await api.createUser(data);
        
        // Assign roles
        if (roleIds.length > 0) {
          for (const rid of roleIds) {
            await api.assignUserRole(user.id, rid);
          }
        }

        notify('Success: Identity created and roles assigned.');
        modalDiv.remove();
        switchView('users');
      } catch (err) { 
        notify('Operation Failed: ' + err.message);
        btn.disabled = false;
        btn.innerText = originalText;
      }
    };
  } catch (err) { notify(err.message, 'error'); }
});

document.addEventListener('action:edit-user', async (e) => {
  const userId = e.detail;
  const user = currentCache.users.find(u => u.id === userId);
  if (!user) return;

  const modalDiv = document.createElement('div');
  modalDiv.className = 'modal-overlay';
  modalDiv.onclick = () => modalDiv.remove();
  modalDiv.innerHTML = `
    <div class="modal-content animate-in" onclick="event.stopPropagation()">
      <div class="modal-header">
        <h3 class="modal-title">Edit Identity Profile</h3>
        <button class="btn" style="padding: 0.2rem 0.5rem;" onclick="this.closest('.modal-overlay').remove()">✕</button>
      </div>
      <form id="editUserForm" style="padding: 1.5rem;">
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
          <div class="form-group"><label class="label">First Name</label><input type="text" id="eFirst" class="input" value="${user.firstName || ''}" /></div>
          <div class="form-group"><label class="label">Last Name</label><input type="text" id="eLast" class="input" value="${user.lastName || ''}" /></div>
          <div class="form-group" style="grid-column: span 2;"><label class="label">Phone</label><input type="text" id="ePhone" class="input" value="${user.phone || ''}" /></div>
        </div>
        <p class="text-muted" style="font-size: 0.75rem; margin-top: 1rem;">Note: Username and Email remain immutable for security audit trails.</p>
        <button type="submit" class="btn" id="updateUserBtn" style="width: 100%; margin-top: 1.5rem;">Update Profile</button>
      </form>
    </div>
  `;
  document.body.appendChild(modalDiv);

  document.getElementById('editUserForm').onsubmit = async (evt) => {
    evt.preventDefault();
    const btn = document.getElementById('updateUserBtn');
    const originalText = btn.innerText;

    const data = {
      firstName: document.getElementById('eFirst').value,
      lastName: document.getElementById('eLast').value,
      phone: document.getElementById('ePhone').value
    };

    try {
      btn.disabled = true;
      btn.innerText = 'Updating Security Record...';
      await api.updateUser(userId, data);
      notify('Success: Profile updated.');
      modalDiv.remove();
      switchView('users');
    } catch (err) {
      notify('Update Failed: ' + err.message);
      btn.disabled = false;
      btn.innerText = originalText;
    }
  };
});

document.addEventListener('action:lock-user', async (e) => {
  const proceed = await askConfirm('Security Isolation', 'Toggle security isolation (Lock/Unlock) for this identity?');
  if (proceed) {
    try {
      await api.lockUser(e.detail);
      notify('Security status synchronized.');
      switchView('users');
    } catch (err) { 
      try { 
        await api.unlockUser(e.detail); 
        notify('User identity restored.');
        switchView('users'); 
      } catch (e2) { notify(err.message, 'error'); }
    }
  }
});

document.addEventListener('action:delete-user', async (e) => {
  const proceed = await askConfirm('Identity Deactivation', 'Permanently DEACTIVATE this identity? This will revoke all active sessions.');
  if (proceed) {
    try {
      await api.deleteUser(e.detail);
      notify('Identity deactivated.');
      switchView('users');
    } catch (err) { notify(err.message, 'error'); }
  }
});

document.addEventListener('action:refresh-key', async (e) => {
  const proceed = await askConfirm('Key Regeneration', 'Are you sure? The previous API key will be immediately REVOKED and a new one generated.');
  if (proceed) {
    try {
      const res = await api.refreshApiKey(e.detail);
      notify(`KEY REFRESHED!\nPrefix: ${res.keyPrefix}\n\nNEW SECRET:\n${res.secretKey}`, 'success');
      switchView('api-keys');
    } catch (err) { notify(err.message, 'error'); }
  }
});

document.addEventListener('action:revoke-key', async (e) => {
  const proceed = await askConfirm('Token Revocation', 'Revoke this API token permanently? All systems using this key will be disconnected.');
  if (proceed) {
    try {
      await api.revokeApiKey(e.detail);
      notify('API Key revoked.');
      switchView('api-keys');
    } catch (err) { notify(err.message, 'error'); }
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
      notify(`KEY ISSUED!\nPrefix: ${res.keyPrefix}\n\nSAVE THIS KEY NOW:\n${res.secretKey}\n\nIt will never be shown again.`);
      switchView('api-keys');
      document.querySelector('.modal-overlay').remove();
    } catch (err) { notify(err.message, 'error'); }
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
      notify('Role defined!');
      switchView('roles');
      document.querySelector('.modal-overlay').remove();
    } catch (err) { notify(err.message, 'error'); }
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
      notify('Tenant onboarded!');
      switchView('tenants');
      document.querySelector('.modal-overlay').remove();
    } catch (err) { notify(err.message, 'error'); }
  };
});

document.addEventListener('action:manage-user-roles', async (e) => {
  const id = e.detail;
  const user = currentCache.users.find(u => u.id === id);
  if (!user) return;
  const userRoles = user.roles || [];

  try {
    if (!currentCache.roles.length) {
      const allRolesPage = await api.getRoles(0, 100);
      currentCache.roles = allRolesPage.content;
    }
    const allRoles = currentCache.roles;
    
    const modalDiv = document.createElement('div');
    modalDiv.className = 'modal-overlay';
    modalDiv.onclick = () => modalDiv.remove();
    modalDiv.innerHTML = `
      <div class="modal-content animate-in" onclick="event.stopPropagation()">
        <div class="modal-header">
          <h3 class="modal-title">Manage Roles: ${user.username}</h3>
          <button class="btn" style="padding: 0.2rem 0.5rem;" onclick="this.closest('.modal-overlay').remove()">✕</button>
        </div>
        <div style="padding: 1.5rem;">
          <p class="text-muted" style="margin-bottom: 1rem;">Toggle roles for this security identity.</p>
          <div style="display: flex; flex-direction: column; gap: 0.75rem;">
            ${allRoles.map(r => `
              <label style="display: flex; align-items: center; gap: 0.75rem; cursor: pointer;">
                <input type="checkbox" data-role-id="${r.id}" ${userRoles.includes(r.name) ? 'checked' : ''} />
                <span>${r.name}</span>
              </label>
            `).join('')}
          </div>
          <button class="btn" id="saveUserRolesBtn" style="width: 100%; margin-top: 1.5rem;">Save Changes</button>
        </div>
      </div>
    `;
    document.body.appendChild(modalDiv);

    modalDiv.querySelector('#saveUserRolesBtn').onclick = async (evt) => {
      const btn = evt.target;
      const originalText = btn.innerText;
      const selectedIds = Array.from(modalDiv.querySelectorAll('input:checked')).map(i => i.dataset.roleId);
      const originalRoleIds = allRoles.filter(r => userRoles.includes(r.name)).map(r => r.id);
      
      try {
        btn.disabled = true;
        btn.innerText = 'Saving Changes...';
        
        const toAdd = selectedIds.filter(rid => !originalRoleIds.includes(rid));
        const toRemove = originalRoleIds.filter(rid => !selectedIds.includes(rid));

        for (const rid of toAdd) await api.assignUserRole(id, rid);
        for (const rid of toRemove) await api.removeUserRole(id, rid);

        notify('Success: Roles updated for ' + user.username);
        modalDiv.remove();
        switchView('users');
      } catch (err) { 
        notify('Update Failed: ' + err.message); 
        btn.disabled = false;
        btn.innerText = originalText;
      }
    };
  } catch (err) { notify(err.message, 'error'); }
});

document.addEventListener('action:manage-role-permissions', async (e) => {
  const id = e.detail;
  const role = currentCache.roles.find(r => r.id === id);
  if (!role) return;
  const rolePerms = (role.permissions || []).map(p => p.name);

  try {
    if (!currentCache.permissions.length) {
      currentCache.permissions = await api.getPermissions();
    }
    const allPerms = currentCache.permissions;

    const modalDiv = document.createElement('div');
    modalDiv.className = 'modal-overlay';
    modalDiv.onclick = () => modalDiv.remove();
    modalDiv.innerHTML = `
      <div class="modal-content animate-in" onclick="event.stopPropagation()" style="max-width: 600px;">
        <div class="modal-header">
          <h3 class="modal-title">Permissions: ${role.name}</h3>
          <button class="btn" style="padding: 0.2rem 0.5rem;" onclick="this.closest('.modal-overlay').remove()">✕</button>
        </div>
        <div style="padding: 1.5rem;">
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; max-height: 400px; overflow-y: auto;">
            ${allPerms.map(p => `
              <label style="display: flex; align-items: center; gap: 0.75rem; cursor: pointer;">
                <input type="checkbox" data-perm-id="${p.id}" ${rolePerms.includes(p.name) ? 'checked' : ''} />
                <span style="font-size: 0.9rem;">${p.name}</span>
              </label>
            `).join('')}
          </div>
          <button class="btn" id="saveRolePermsBtn" style="width: 100%; margin-top: 1.5rem;">Update Permissions</button>
        </div>
      </div>
    `;
    document.body.appendChild(modalDiv);

    modalDiv.querySelector('#saveRolePermsBtn').onclick = async (evt) => {
      const btn = evt.target;
      const originalText = btn.innerText;
      const selectedIds = Array.from(modalDiv.querySelectorAll('input:checked')).map(i => i.dataset.permId);
      const originalPermIds = allPerms.filter(p => rolePerms.includes(p.name)).map(p => p.id);
      
      try {
        btn.disabled = true;
        btn.innerText = 'Updating Permissions...';

        const toAdd = selectedIds.filter(pid => !originalPermIds.includes(pid));
        const toRemove = originalPermIds.filter(pid => !selectedIds.includes(pid));

        for (const pid of toAdd) await api.addRolePermission(id, { permissionId: pid });
        for (const pid of toRemove) await api.removeRolePermission(id, pid);

        notify('Success: Permissions updated for ' + role.name);
        modalDiv.remove();
        switchView('roles');
      } catch (err) { 
        notify('Update Failed: ' + err.message); 
        btn.disabled = false;
        btn.innerText = originalText;
      }
    };
  } catch (err) { notify(err.message, 'error'); }
});

document.addEventListener('action:edit-user', async (e) => {
    const id = e.detail;
    const user = currentCache.users.find(u => u.id === id);
    if (!user) return;

    const fields = [
        { id: 'eFirst', label: 'First Name', value: user.firstName || '' },
        { id: 'eLast', label: 'Last Name', value: user.lastName || '' },
        { id: 'ePhone', label: 'Phone', value: user.phone || '' }
    ];
    const modalDiv = document.createElement('div');
    modalDiv.innerHTML = FormModalTemplate(`Edit Identity: ${user.username}`, fields, 'updateUser');
    document.body.appendChild(modalDiv.firstElementChild);

    document.getElementById('updateUserForm').onsubmit = async (evt) => {
        evt.preventDefault();
        const data = {
            firstName: document.getElementById('eFirst').value,
            lastName: document.getElementById('eLast').value,
            phone: document.getElementById('ePhone').value
        };
        try {
            await api.updateUser(id, data);
            notify('User updated!');
            switchView('users');
            document.querySelector('.modal-overlay').remove();
        } catch (err) { notify(err.message, 'error'); }
    };
});

document.addEventListener('action:edit-role', async (e) => {
    const id = e.detail;
    const role = currentCache.roles.find(r => r.id === id);
    if (!role) return;

    const fields = [
        { id: 'erName', label: 'Role Name', value: role.name, required: true },
        { id: 'erDesc', label: 'Description', value: role.description || '' }
    ];
    const modalDiv = document.createElement('div');
    modalDiv.innerHTML = FormModalTemplate(`Edit Role: ${role.name}`, fields, 'updateRole');
    document.body.appendChild(modalDiv.firstElementChild);

    document.getElementById('updateRoleForm').onsubmit = async (evt) => {
        evt.preventDefault();
        const data = {
            name: document.getElementById('erName').value,
            description: document.getElementById('erDesc').value
        };
        try {
            await api.updateRole(id, data);
            notify('Role updated!');
            switchView('roles');
            document.querySelector('.modal-overlay').remove();
        } catch (err) { notify(err.message, 'error'); }
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

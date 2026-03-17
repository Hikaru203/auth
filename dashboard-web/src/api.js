const BASE_URL = 'http://localhost:8080/api/v1';

class ApiService {
    constructor() {
        this.token = localStorage.getItem('auth_token');
    }

    setToken(token, tenantSlug) {
        this.token = token;
        if (token) {
            localStorage.setItem('auth_token', token);
            if (tenantSlug) localStorage.setItem('tenant_slug', tenantSlug);
        } else {
            localStorage.removeItem('auth_token');
            localStorage.removeItem('tenant_slug');
        }
    }

    async request(endpoint, options = {}) {
        const cleanEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
        const url = `${BASE_URL}${cleanEndpoint}`;
        const headers = {
            'Content-Type': 'application/json',
            ...options.headers,
        };

        if (this.token) {
            headers['Authorization'] = `Bearer ${this.token}`;
        }

        try {
            console.log(`[API CALL] ${options.method || 'GET'} ${url}`, options.body ? JSON.parse(options.body) : '');
            const response = await fetch(url, { ...options, headers });
            console.log(`[API RSP] ${response.status} ${cleanEndpoint}`);
            
            if (response.status === 401) {
                this.setToken(null);
                window.dispatchEvent(new CustomEvent('unauthorized'));
            }

            if (!response.ok) {
                const error = await response.json().catch(() => ({ message: 'System error' }));
                throw new Error(error.message || `Error ${response.status}`);
            }

            return response.status !== 204 ? await response.json() : null;
        } catch (error) {
            console.error(`API Error (${endpoint}):`, error);
            throw error;
        }
    }

    // AUTH
    login(data) { return this.request('/auth/login', { method: 'POST', body: JSON.stringify(data) }); }
    logout() { return this.request('/auth/logout', { method: 'POST' }); }
    refreshToken() { return this.request('/auth/refresh', { method: 'POST' }); }
    passwordResetRequest(email) { return this.request('/auth/password/reset-request', { method: 'POST', body: JSON.stringify({ email }) }); }
    completePasswordReset(data) { return this.request('/auth/password/reset', { method: 'POST', body: JSON.stringify(data) }); }
    changePassword(data) { return this.request('/auth/password/change', { method: 'POST', body: JSON.stringify(data) }); }
    setup2fa() { return this.request('/auth/2fa/setup', { method: 'POST' }); }
    verify2fa(data) { return this.request('/auth/2fa/verify', { method: 'POST', body: JSON.stringify(data) }); }
    disable2fa() { return this.request('/auth/2fa/disable', { method: 'DELETE' }); }

    // USERS
    getUsers(page = 0, size = 20) { return this.request(`/users?page=${page}&size=${size}`); }
    getUser(id) { return this.request(`/users/${id}`); }
    createUser(data) { return this.request('/users', { method: 'POST', body: JSON.stringify(data) }); }
    updateUser(id, data) { return this.request(`/users/${id}`, { method: 'PUT', body: JSON.stringify(data) }); }
    deleteUser(id) { return this.request(`/users/${id}`, { method: 'DELETE' }); }
    getMe() { return this.request('/users/me'); }
    lockUser(id) { return this.request(`/users/${id}/lock`, { method: 'POST' }); }
    unlockUser(id) { return this.request(`/users/${id}/unlock`, { method: 'POST' }); }
    assignUserRole(userId, roleId) { return this.request(`/users/${userId}/roles`, { method: 'POST', body: JSON.stringify({ roleId }) }); }
    removeUserRole(userId, roleId) { return this.request(`/users/${userId}/roles/${roleId}`, { method: 'DELETE' }); }

    // ROLES
    getRoles(page = 0, size = 20) { return this.request(`/roles?page=${page}&size=${size}`); }
    getRole(id) { return this.request(`/roles/${id}`); }
    createRole(data) { return this.request('/roles', { method: 'POST', body: JSON.stringify(data) }); }
    updateRole(id, data) { return this.request(`/roles/${id}`, { method: 'PUT', body: JSON.stringify(data) }); }
    deleteRole(id) { return this.request(`/roles/${id}`, { method: 'DELETE' }); }
    addRolePermission(roleId, data) { return this.request(`/roles/${roleId}/permissions`, { method: 'POST', body: JSON.stringify(data) }); }
    removeRolePermission(roleId, permissionId) { return this.request(`/roles/${roleId}/permissions/${permissionId}`, { method: 'DELETE' }); }

    // API KEYS
    getApiKeys(page = 0, size = 20) { return this.request(`/api-keys?page=${page}&size=${size}`); }
    generateApiKey(data) { return this.request('/api-keys', { method: 'POST', body: JSON.stringify(data) }); }
    refreshApiKey(id) { return this.request(`/api-keys/${id}/refresh`, { method: 'POST' }); }
    revokeApiKey(id) { return this.request(`/api-keys/${id}/revoke`, { method: 'POST' }); }

    // AUDIT & TENANTS
    async getAuditLogs(filters = {}) {
        const query = new URLSearchParams();
        const page = filters.page !== undefined ? filters.page : 0;
        const size = filters.size !== undefined ? filters.size : 20;
        
        query.append('page', page);
        query.append('size', size);
        if (filters.username) query.append('username', filters.username);
        if (filters.action) query.append('action', filters.action);
        if (filters.statusCode) query.append('statusCode', filters.statusCode);
        
        const path = `/audit${query.toString() ? '?' + query.toString() : ''}`;
        return this.request(path);
    }
    getMyAuditLogs(page = 0, size = 20) { return this.request(`/audit/me?page=${page}&size=${size}`); }
    
    getTenants() { return this.request('/tenants'); }
    getTenant(id) { return this.request(`/tenants/${id}`); }
    createTenant(data) { return this.request('/tenants', { method: 'POST', body: JSON.stringify(data) }); }
    updateTenant(id, data) { return this.request(`/tenants/${id}`, { method: 'PUT', body: JSON.stringify(data) }); }
    deleteTenant(id) { return this.request(`/tenants/${id}`, { method: 'DELETE' }); }

    // STATISTICS
    getTrafficStats(days = 7) { return this.request(`/stats/traffic?days=${days}`); }
    getRoleDistribution() { return this.request('/stats/roles'); }
    getPermissions() { return this.request('/roles/permissions'); }
}

export const api = new ApiService();

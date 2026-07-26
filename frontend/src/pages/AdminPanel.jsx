import { useState, useEffect } from 'react'
import api from '../api/api'
import { useToast } from '../context/ToastContext'

export default function AdminPanel() {
  const toast = useToast()
  const [users, setUsers] = useState([])
  const [departments, setDepartments] = useState([])
  const [tab, setTab] = useState('users')
  const [newUser, setNewUser] = useState({ name: '', email: '', password: '', role: 'AGENT', departmentId: '' })
  const [userPage, setUserPage] = useState(0)
  const [userTotalPages, setUserTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [confirmAction, setConfirmAction] = useState(null)
  const [slaConfigs, setSlaConfigs] = useState([])
  const [selectedDeptForSla, setSelectedDeptForSla] = useState('')
  const [slaForm, setSlaForm] = useState({ priority: 'LOW', resolutionHours: 48 })

  const loadUsers = async (page = 0) => {
    setLoading(true)
    try {
      const res = await api.get('/admin/users', { params: { page, size: 10 } })
      setUsers(res.data.content || [])
      setUserTotalPages(res.data.totalPages || 0)
      setUserPage(page)
    } catch (err) {
      toast.error('Failed to load users')
    } finally {
      setLoading(false)
    }
  }

  const loadDepartments = () => api.get('/auth/departments').then(res => setDepartments(res.data || [])).catch(() => {})

  const loadSlaConfigs = async (deptId) => {
    if (!deptId) { setSlaConfigs([]); return }
    try {
      const res = await api.get(`/admin/sla/${deptId}`)
      setSlaConfigs(res.data || [])
    } catch (err) {
      toast.error('Failed to load SLA configs')
    }
  }

  const updateSlaConfig = async (e) => {
    e.preventDefault()
    if (!selectedDeptForSla) return
    try {
      await api.put(`/admin/sla/${selectedDeptForSla}`, slaForm)
      toast.success('SLA config updated')
      loadSlaConfigs(selectedDeptForSla)
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to update SLA config')
    }
  }

  useEffect(() => { loadUsers(); loadDepartments() }, [])
  useEffect(() => { if (selectedDeptForSla) loadSlaConfigs(selectedDeptForSla) }, [selectedDeptForSla])

  const createUser = async (e) => {
    e.preventDefault()
    try {
      await api.post('/admin/users', { ...newUser, departmentId: newUser.departmentId ? Number(newUser.departmentId) : null })
      setNewUser({ name: '', email: '', password: '', role: 'AGENT', departmentId: '' })
      loadUsers()
      toast.success('User created successfully')
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || 'Failed to create user'
      toast.error(msg)
    }
  }

  const toggleUser = async (userId, isActive) => {
    try {
      await api.put(`/admin/users/${userId}/${isActive ? 'deactivate' : 'activate'}`)
      loadUsers(userPage)
      toast.success(isActive ? 'User deactivated' : 'User activated')
    } catch (err) {
      toast.error('Failed to update user')
    }
    setConfirmAction(null)
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Admin Panel</h1>

      <div className="flex gap-2">
        {['users', 'create', 'sla'].map(t => (
          <button key={t} onClick={() => setTab(t)}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
              tab === t ? 'bg-indigo-600 text-white' : 'bg-white text-gray-600 border border-gray-200 hover:bg-gray-50'
            }`}>{t === 'sla' ? 'SLA Config' : t === 'create' ? 'Create User' : 'All Users'}</button>
        ))}
      </div>

      {tab === 'create' && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Create New User</h2>
          <form onSubmit={createUser} className="space-y-4 max-w-lg">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Name</label>
              <input type="text" required value={newUser.name} onChange={e => setNewUser({...newUser, name: e.target.value})}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
              <input type="email" required value={newUser.email} onChange={e => setNewUser({...newUser, email: e.target.value})}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Password</label>
              <input type="password" required minLength={6} value={newUser.password} onChange={e => setNewUser({...newUser, password: e.target.value})}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Role</label>
                <select value={newUser.role} onChange={e => setNewUser({...newUser, role: e.target.value})}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm bg-white">
                  <option value="AGENT">Agent</option>
                  <option value="DEPT_HEAD">Dept Head</option>
                  <option value="EMPLOYEE">Employee</option>
                  <option value="SUPER_ADMIN">Super Admin</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Department</label>
                <select value={newUser.departmentId} onChange={e => setNewUser({...newUser, departmentId: e.target.value})}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm bg-white">
                  <option value="">None</option>
                  {departments.map(d => (
                    <option key={d.id} value={d.id}>{d.name}</option>
                  ))}
                </select>
              </div>
            </div>
            <button type="submit" className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-indigo-700">
              Create User
            </button>
          </form>
        </div>
      )}

      {tab === 'users' && (
        <>
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
            <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Name</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Email</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Role</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Dept</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Status</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {loading ? (
                  <tr><td colSpan="6" className="text-center py-8 text-gray-400">Loading...</td></tr>
                ) : users.map(u => (
                  <tr key={u.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-sm font-medium text-gray-900">{u.name}</td>
                    <td className="px-4 py-3 text-sm text-gray-600">{u.email}</td>
                    <td className="px-4 py-3">
                      <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${
                        u.role === 'SUPER_ADMIN' ? 'bg-red-100 text-red-700' :
                        u.role === 'DEPT_HEAD' ? 'bg-purple-100 text-purple-700' :
                        u.role === 'AGENT' ? 'bg-green-100 text-green-700' :
                        'bg-blue-100 text-blue-700'
                      }`}>{u.role.replace('_', ' ')}</span>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600">{u.departmentName || '-'}</td>
                    <td className="px-4 py-3">
                      <span className={`text-xs font-medium ${u.isActive ? 'text-green-600' : 'text-red-600'}`}>
                        {u.isActive ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      {confirmAction === u.id ? (
                        <div className="flex items-center gap-2">
                          <span className="text-xs text-gray-500">Confirm?</span>
                          <button onClick={() => toggleUser(u.id, u.isActive)}
                            className="text-xs font-medium text-red-600 hover:text-red-800">Yes</button>
                          <button onClick={() => setConfirmAction(null)}
                            className="text-xs font-medium text-gray-500 hover:text-gray-700">No</button>
                        </div>
                      ) : (
                        <button onClick={() => setConfirmAction(u.id)}
                          className={`text-sm font-medium ${u.isActive ? 'text-red-600 hover:text-red-800' : 'text-green-600 hover:text-green-800'}`}>
                          {u.isActive ? 'Deactivate' : 'Activate'}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            </div>
          </div>

          {userTotalPages > 1 && (
            <div className="flex items-center justify-center gap-2">
              <button onClick={() => loadUsers(Math.max(0, userPage - 1))} disabled={userPage === 0}
                className="px-3 py-1 rounded-lg text-sm bg-white border border-gray-200 disabled:opacity-50">Prev</button>
              <span className="text-sm text-gray-500">Page {userPage + 1} of {userTotalPages}</span>
              <button onClick={() => loadUsers(Math.min(userTotalPages - 1, userPage + 1))} disabled={userPage >= userTotalPages - 1}
                className="px-3 py-1 rounded-lg text-sm bg-white border border-gray-200 disabled:opacity-50">Next</button>
            </div>
          )}
        </>
      )}

      {tab === 'sla' && (
        <div className="space-y-6">
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">SLA Configuration</h2>
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-1">Select Department</label>
              <select value={selectedDeptForSla} onChange={e => setSelectedDeptForSla(e.target.value)}
                className="border border-gray-300 rounded-lg px-3 py-2 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500">
                <option value="">Select department...</option>
                {departments.map(d => (
                  <option key={d.id} value={d.id}>{d.name}</option>
                ))}
              </select>
            </div>

            {selectedDeptForSla && (
              <>
                <div className="bg-gray-50 rounded-lg p-4 mb-4">
                  <h3 className="text-sm font-medium text-gray-700 mb-3">Current SLA Configs</h3>
                  {slaConfigs.length === 0 ? (
                    <p className="text-sm text-gray-400">No SLA configs found</p>
                  ) : (
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                      {slaConfigs.map(c => (
                        <div key={c.id} className="bg-white rounded-lg border border-gray-200 p-3">
                          <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${
                            c.priority === 'CRITICAL' ? 'bg-red-100 text-red-700' :
                            c.priority === 'HIGH' ? 'bg-orange-100 text-orange-700' :
                            c.priority === 'MEDIUM' ? 'bg-yellow-100 text-yellow-700' :
                            'bg-green-100 text-green-700'
                          }`}>{c.priority}</span>
                          <p className="text-lg font-bold text-gray-900 mt-1">{c.resolutionHours}h</p>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                <form onSubmit={updateSlaConfig} className="flex items-end gap-3">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Priority</label>
                    <select value={slaForm.priority} onChange={e => setSlaForm({...slaForm, priority: e.target.value})}
                      className="border border-gray-300 rounded-lg px-3 py-2 text-sm bg-white">
                      <option value="LOW">LOW</option>
                      <option value="MEDIUM">MEDIUM</option>
                      <option value="HIGH">HIGH</option>
                      <option value="CRITICAL">CRITICAL</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Resolution Hours</label>
                    <input type="number" min="1" value={slaForm.resolutionHours}
                      onChange={e => setSlaForm({...slaForm, resolutionHours: parseInt(e.target.value) || 1})}
                      className="border border-gray-300 rounded-lg px-3 py-2 text-sm w-32 focus:outline-none focus:ring-2 focus:ring-indigo-500" />
                  </div>
                  <button type="submit" className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-indigo-700">
                    Update
                  </button>
                </form>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

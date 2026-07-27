import { useState, useEffect, useRef, useCallback } from 'react'
import { Link } from 'react-router-dom'
import api from '../api/api'
import { useAuth } from '../context/AuthContext'
import SlaCountdown from '../components/SlaCountdown'

const STATUS_OPTIONS = ['RAISED', 'ASSIGNED', 'IN_PROGRESS', 'REOPENED', 'RESOLVED', 'CLOSED']
const PRIORITY_OPTIONS = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']

export default function TicketList() {
  const { user } = useAuth()
  const [tickets, setTickets] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [filterStatus, setFilterStatus] = useState('')
  const [filterPriority, setFilterPriority] = useState('')
  const [search, setSearch] = useState('')
  const [debouncedSearch, setDebouncedSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const debounceRef = useRef(null)

  const viewLabel = user?.role === 'AGENT' ? 'My Assigned Tickets'
    : user?.role === 'DEPT_HEAD' ? 'Department Tickets'
    : user?.role === 'SUPER_ADMIN' ? 'All Tickets'
    : 'My Tickets'

  const handleSearch = useCallback((value) => {
    setSearch(value)
    if (debounceRef.current) clearTimeout(debounceRef.current)
    debounceRef.current = setTimeout(() => {
      setDebouncedSearch(value)
      setPage(0)
    }, 400)
  }, [])

  useEffect(() => {
    setPage(0)
  }, [filterStatus, filterPriority])

  useEffect(() => {
    const fetchTickets = async () => {
      setLoading(true)
      try {
        const params = { page, size: 10 }
        if (filterStatus) params.status = filterStatus
        if (filterPriority) params.priority = filterPriority
        if (debouncedSearch.trim()) params.search = debouncedSearch.trim()

        let res
        if (user?.role === 'AGENT') {
          res = await api.get(`/tickets/agent/${user.userId}`, { params })
        } else if (user?.role === 'DEPT_HEAD') {
          res = await api.get(`/tickets/dept/${user.departmentId}`, { params })
        } else if (user?.role === 'EMPLOYEE') {
          res = await api.get('/tickets/my', { params })
        } else {
          res = await api.get('/tickets/all', { params })
        }
        setTickets(res.data.content || [])
        setTotalPages(res.data.totalPages || 0)
      } catch (err) {
        console.error('Failed to load tickets', err)
      } finally {
        setLoading(false)
      }
    }
    fetchTickets()
  }, [user, page, filterStatus, filterPriority, debouncedSearch])

  const clearFilters = () => {
    setFilterStatus('')
    setFilterPriority('')
    setSearch('')
    setDebouncedSearch('')
  }

  const hasFilters = filterStatus || filterPriority || search

  return (
    <div className="space-y-4 sm:space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl sm:text-2xl font-bold text-gray-900">{viewLabel}</h1>
        <Link to="/tickets/new" className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-indigo-700">
          + New Ticket
        </Link>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4">
        <div className="flex flex-wrap items-center gap-3">
          <input
            type="text"
            placeholder="Search by title or ticket no..."
            value={search}
            onChange={e => handleSearch(e.target.value)}
            className="border border-gray-300 rounded-lg px-3 py-1.5 text-sm flex-1 min-w-[200px] sm:w-64 focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
          <select value={filterStatus} onChange={e => setFilterStatus(e.target.value)}
            className="border border-gray-300 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500">
            <option value="">All Status</option>
            {STATUS_OPTIONS.map(s => <option key={s} value={s}>{s.replace('_', ' ')}</option>)}
          </select>
          <select value={filterPriority} onChange={e => setFilterPriority(e.target.value)}
            className="border border-gray-300 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500">
            <option value="">All Priority</option>
            {PRIORITY_OPTIONS.map(p => <option key={p} value={p}>{p}</option>)}
          </select>
          {hasFilters && (
            <button onClick={clearFilters} className="text-sm text-gray-500 hover:text-gray-700 underline">
              Clear filters
            </button>
          )}
        </div>
      </div>

      {/* Desktop table */}
      <div className="hidden sm:block bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Ticket No</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Title</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Dept</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Priority</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Status</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Created</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">Assigned To</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">SLA</th>
                <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {loading ? (
                <tr><td colSpan="9" className="text-center py-8 text-gray-400">Loading...</td></tr>
              ) : tickets.length === 0 ? (
                <tr><td colSpan="9" className="text-center py-8 text-gray-400">No tickets found</td></tr>
              ) : tickets.map(t => (
                <tr key={t.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-4 py-3 text-sm font-mono text-gray-500">{t.ticketNo}</td>
                  <td className="px-4 py-3">
                    <p className="text-sm font-medium text-gray-900">{t.title}</p>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600">{t.departmentName}</td>
                  <td className="px-4 py-3">
                    <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${
                      t.priority === 'CRITICAL' ? 'bg-red-100 text-red-700' :
                      t.priority === 'HIGH' ? 'bg-orange-100 text-orange-700' :
                      t.priority === 'MEDIUM' ? 'bg-yellow-100 text-yellow-700' :
                      'bg-green-100 text-green-700'
                    }`}>{t.priority}</span>
                  </td>
                  <td className="px-4 py-3">
                    <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${
                      t.status === 'CLOSED' ? 'bg-gray-100 text-gray-600' :
                      t.status === 'RESOLVED' ? 'bg-green-100 text-green-700' :
                      t.status === 'IN_PROGRESS' ? 'bg-indigo-100 text-indigo-700' :
                      t.status === 'ASSIGNED' ? 'bg-blue-100 text-blue-700' :
                      t.status === 'REOPENED' ? 'bg-orange-100 text-orange-700' :
                      'bg-yellow-100 text-yellow-700'
                    }`}>{t.status.replace('_', ' ')}</span>
                    {t.slaBreached && <span className="ml-1 text-xs text-red-600 font-medium">SLA</span>}
                    {t.escalated && <span className="ml-1 text-xs text-orange-600 font-medium">ESC</span>}
                  </td>
                  <td className="px-4 py-3 text-xs text-gray-500">
                    {t.createdAt ? new Date(t.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' }) : '-'}
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600">{t.assignedToName || 'Unassigned'}</td>
                  <td className="px-4 py-3">
                    <SlaCountdown slaDeadline={t.slaDeadline} status={t.status} />
                  </td>
                  <td className="px-4 py-3">
                    <Link to={`/tickets/${t.id}`} className="text-indigo-600 hover:text-indigo-800 text-sm font-medium">View</Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Mobile card view */}
      <div className="sm:hidden space-y-3">
        {loading ? (
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4 text-center text-gray-400">Loading...</div>
        ) : tickets.length === 0 ? (
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4 text-center text-gray-400">No tickets found</div>
        ) : tickets.map(t => (
          <Link key={t.id} to={`/tickets/${t.id}`}
            className="block bg-white rounded-xl shadow-sm border border-gray-200 p-4 hover:bg-gray-50 transition-colors">
            <div className="flex items-center justify-between mb-1">
              <span className="text-xs font-mono text-gray-400">{t.ticketNo}</span>
              <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${
                t.status === 'CLOSED' ? 'bg-gray-100 text-gray-600' :
                t.status === 'RESOLVED' ? 'bg-green-100 text-green-700' :
                t.status === 'IN_PROGRESS' ? 'bg-indigo-100 text-indigo-700' :
                t.status === 'ASSIGNED' ? 'bg-blue-100 text-blue-700' :
                t.status === 'REOPENED' ? 'bg-orange-100 text-orange-700' :
                'bg-yellow-100 text-yellow-700'
              }`}>{t.status.replace('_', ' ')}</span>
            </div>
            <p className="text-sm font-medium text-gray-900 truncate">{t.title}</p>
            <div className="flex items-center justify-between mt-2 text-xs text-gray-500">
              <div className="flex items-center gap-2">
                <span>{t.departmentName}</span>
                <span className={`px-1.5 py-0.5 rounded-full font-medium ${
                  t.priority === 'CRITICAL' ? 'bg-red-100 text-red-700' :
                  t.priority === 'HIGH' ? 'bg-orange-100 text-orange-700' :
                  t.priority === 'MEDIUM' ? 'bg-yellow-100 text-yellow-700' :
                  'bg-green-100 text-green-700'
                }`}>{t.priority}</span>
              </div>
              <div className="flex items-center gap-2">
                {t.slaBreached && <span className="text-red-600 font-medium">SLA</span>}
                <SlaCountdown slaDeadline={t.slaDeadline} status={t.status} />
              </div>
            </div>
          </Link>
        ))}
      </div>

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2">
          <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
            className="px-3 py-1 rounded-lg text-sm bg-white border border-gray-200 disabled:opacity-50">Prev</button>
          <span className="text-sm text-gray-500">Page {page + 1} of {totalPages}</span>
          <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}
            className="px-3 py-1 rounded-lg text-sm bg-white border border-gray-200 disabled:opacity-50">Next</button>
        </div>
      )}
    </div>
  )
}

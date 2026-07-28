import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import api from '../api/api'
import { useAuth } from '../context/AuthContext'
import SlaCountdown from '../components/SlaCountdown'

export default function Dashboard() {
  const { user } = useAuth()
  const [stats, setStats] = useState(null)
  const [tickets, setTickets] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    const fetchDashboard = async () => {
      setLoading(true)
      try {
        const [statsRes, ticketsRes] = await Promise.all([
          api.get('/tickets/dashboard/stats'),
          user?.role === 'EMPLOYEE'
            ? api.get('/tickets/my', { params: { page: 0, size: 5 } })
            : user?.role === 'AGENT' && user?.userId
              ? api.get(`/tickets/agent/${user.userId}`, { params: { page: 0, size: 5 } })
              : user?.role === 'DEPT_HEAD' && user?.departmentId
                ? api.get(`/tickets/dept/${user.departmentId}`, { params: { page: 0, size: 5 } })
                : api.get('/tickets/all', { params: { page: 0, size: 5 } })
        ])
        setStats(statsRes.data)
        setTickets((ticketsRes.data.content || []).slice(0, 5))
      } catch (err) {
        console.error('Failed to load dashboard', err)
        setError('Failed to load dashboard data. Please try again later.')
      } finally {
        setLoading(false)
      }
    }
    if (user) fetchDashboard()
  }, [user])

  const statCards = stats
    ? [
        { label: 'Total', value: stats.total, color: 'text-indigo-600' },
        { label: 'Open', value: stats.open, color: 'text-yellow-600' },
        { label: 'In Progress', value: stats.inProgress, color: 'text-blue-600' },
        { label: 'Closed', value: stats.closed, color: 'text-green-600' },
        { label: 'SLA Breached', value: stats.breached, color: 'text-red-600' },
      ]
    : []

  return (
    <div className="space-y-4 sm:space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <h1 className="text-xl sm:text-2xl font-bold text-gray-900 truncate">Welcome, {user?.name}</h1>
        <Link to="/tickets/new" className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-indigo-700 text-center whitespace-nowrap">
          + New Ticket
        </Link>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 text-sm px-4 py-3 rounded-lg">
          {error}
        </div>
      )}

      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3 sm:gap-4">
        {statCards.map(card => (
          <div key={card.label} className="bg-white rounded-xl shadow-sm border border-gray-200 p-3 sm:p-5">
            <p className="text-xs sm:text-sm text-gray-500 mb-0.5 sm:mb-1">{card.label}</p>
            <p className={`text-2xl sm:text-3xl font-bold ${card.color}`}>{loading ? '-' : card.value}</p>
          </div>
        ))}
      </div>

      {stats?.avgRating > 0 && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-3 sm:p-5">
          <p className="text-xs sm:text-sm text-gray-500 mb-0.5 sm:mb-1">Average Rating</p>
          <p className="text-2xl sm:text-3xl font-bold text-amber-500">{stats.avgRating.toFixed(1)} / 5</p>
        </div>
      )}

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4 sm:p-6">
        <h2 className="text-base sm:text-lg font-semibold text-gray-900 mb-3 sm:mb-4">Recent Tickets</h2>
        <div className="space-y-2">
          {loading ? (
            <p className="text-sm text-gray-400 text-center py-4">Loading...</p>
          ) : tickets.map(t => (
            <Link key={t.id} to={`/tickets/${t.id}`}
              className="block p-3 rounded-lg hover:bg-gray-50 transition-colors border border-gray-100">
              <div className="flex items-start justify-between gap-2">
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-xs font-mono text-gray-400 shrink-0">{t.ticketNo}</span>
                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium shrink-0 ${
                      t.status === 'CLOSED' ? 'bg-gray-100 text-gray-600' :
                      t.status === 'RESOLVED' ? 'bg-green-100 text-green-700' :
                      t.status === 'IN_PROGRESS' ? 'bg-indigo-100 text-indigo-700' :
                      t.status === 'ASSIGNED' ? 'bg-blue-100 text-blue-700' :
                      t.status === 'REOPENED' ? 'bg-orange-100 text-orange-700' :
                      'bg-yellow-100 text-yellow-700'
                    }`}>{t.status.replace('_', ' ')}</span>
                  </div>
                  <p className="text-sm font-medium text-gray-900 truncate">{t.title}</p>
                  <p className="text-xs text-gray-400 mt-0.5">{t.departmentName}</p>
                </div>
                <div className="flex flex-col items-end gap-1 shrink-0">
                  {t.slaBreached && <span className="text-[10px] sm:text-xs text-red-600 font-medium">SLA BREACHED</span>}
                  {t.escalated && <span className="text-[10px] sm:text-xs text-orange-600 font-medium">ESCALATED</span>}
                  <SlaCountdown slaDeadline={t.slaDeadline} slaHours={t.slaHours} status={t.status} />
                </div>
              </div>
            </Link>
          ))}
          {!loading && tickets.length === 0 && <p className="text-sm text-gray-400 text-center py-4">No tickets yet</p>}
        </div>
      </div>
    </div>
  )
}

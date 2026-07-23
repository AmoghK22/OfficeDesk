import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import api from '../api/api'
import { useAuth } from '../context/AuthContext'
import SlaCountdown from '../components/SlaCountdown'

export default function Dashboard() {
  const { user } = useAuth()
  const [tickets, setTickets] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchTickets = async () => {
      setLoading(true)
      try {
        let res
        if (user?.role === 'EMPLOYEE') {
          res = await api.get('/tickets/my', { params: { page: 0, size: 200 } })
        } else if (user?.role === 'AGENT' && user?.userId) {
          res = await api.get(`/tickets/agent/${user.userId}`, { params: { page: 0, size: 200 } })
        } else if (user?.role === 'DEPT_HEAD' && user?.departmentId) {
          res = await api.get(`/tickets/dept/${user.departmentId}`, { params: { page: 0, size: 200 } })
        } else {
          res = await api.get('/tickets/all', { params: { page: 0, size: 200 } })
        }
        setTickets(res.data.content || [])
      } catch (err) {
        console.error('Failed to load tickets')
      } finally {
        setLoading(false)
      }
    }
    if (user) fetchTickets()
  }, [user])

  const open = tickets.filter(t => !['RESOLVED', 'CLOSED'].includes(t.status)).length
  const breached = tickets.filter(t => t.slaBreached).length
  const closed = tickets.filter(t => t.status === 'CLOSED').length
  const inProgress = tickets.filter(t => t.status === 'IN_PROGRESS').length

  const statsLabel = user?.role === 'EMPLOYEE' ? 'My Tickets' :
    user?.role === 'AGENT' ? 'My Assigned Tickets' :
    user?.role === 'DEPT_HEAD' ? 'Department Tickets' : 'All Tickets'

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Welcome, {user?.name}</h1>
        <Link to="/tickets/new" className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-indigo-700">
          + New Ticket
        </Link>
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-5 gap-4">
        {[
          { label: statsLabel, value: tickets.length, color: 'text-indigo-600' },
          { label: 'Open', value: open, color: 'text-yellow-600' },
          { label: 'In Progress', value: inProgress, color: 'text-blue-600' },
          { label: 'Closed', value: closed, color: 'text-green-600' },
          { label: 'SLA Breached', value: breached, color: 'text-red-600' },
        ].map(card => (
          <div key={card.label} className="bg-white rounded-xl shadow-sm border border-gray-200 p-5">
            <p className="text-sm text-gray-500 mb-1">{card.label}</p>
            <p className={`text-3xl font-bold ${card.color}`}>{loading ? '-' : card.value}</p>
          </div>
        ))}
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Recent Tickets</h2>
        <div className="space-y-2">
          {loading ? (
            <p className="text-sm text-gray-400 text-center py-4">Loading...</p>
          ) : tickets.slice(0, 5).map(t => (
            <Link key={t.id} to={`/tickets/${t.id}`}
              className="flex items-center justify-between p-3 rounded-lg hover:bg-gray-50 transition-colors border border-gray-100">
              <div className="flex items-center gap-3">
                <span className="text-xs font-mono text-gray-400">{t.ticketNo}</span>
                <span className="text-sm font-medium text-gray-900">{t.title}</span>
                <span className="text-xs text-gray-400">{t.departmentName}</span>
              </div>
              <div className="flex items-center gap-2">
                {t.slaBreached && <span className="text-xs text-red-600 font-medium">SLA BREACHED</span>}
                {t.escalated && <span className="text-xs text-orange-600 font-medium">ESCALATED</span>}
                <SlaCountdown slaDeadline={t.slaDeadline} status={t.status} />
                <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                  t.status === 'CLOSED' ? 'bg-gray-100 text-gray-600' :
                  t.status === 'RESOLVED' ? 'bg-green-100 text-green-700' :
                  t.status === 'IN_PROGRESS' ? 'bg-indigo-100 text-indigo-700' :
                  t.status === 'ASSIGNED' ? 'bg-blue-100 text-blue-700' :
                  t.status === 'REOPENED' ? 'bg-orange-100 text-orange-700' :
                  'bg-yellow-100 text-yellow-700'
                }`}>{t.status.replace('_', ' ')}</span>
              </div>
            </Link>
          ))}
          {!loading && tickets.length === 0 && <p className="text-sm text-gray-400 text-center py-4">No tickets yet</p>}
        </div>
      </div>
    </div>
  )
}

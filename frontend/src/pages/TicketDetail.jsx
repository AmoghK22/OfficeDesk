import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import api from '../api/api'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import SlaCountdown from '../components/SlaCountdown'

const ACTIVITY_ICONS = {
  CREATED: { bg: 'bg-blue-500', label: 'Created', icon: '★' },
  STATUS_CHANGED: { bg: 'bg-indigo-500', label: 'Status Changed', icon: '→' },
  ASSIGNED: { bg: 'bg-purple-500', label: 'Assigned', icon: '↔' },
  COMMENT_ADDED: { bg: 'bg-gray-500', label: 'Comment', icon: '💬' },
  REOPENED: { bg: 'bg-orange-500', label: 'Reopened', icon: '↺' },
  RATED: { bg: 'bg-yellow-500', label: 'Rated', icon: '★' },
}

export default function TicketDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const toast = useToast()
  const [ticket, setTicket] = useState(null)
  const [comments, setComments] = useState([])
  const [activities, setActivities] = useState([])
  const [newComment, setNewComment] = useState('')
  const [isInternal, setIsInternal] = useState(false)
  const [remarks, setRemarks] = useState('')
  const [reopenReason, setReopenReason] = useState('')
  const [ratingVal, setRatingVal] = useState(5)
  const [feedback, setFeedback] = useState('')
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)
  const [rated, setRated] = useState(false)
  const [agents, setAgents] = useState([])
  const [selectedAgent, setSelectedAgent] = useState('')

  const load = async () => {
    setLoading(true)
    try {
      const [ticketRes, commentsRes, activitiesRes] = await Promise.all([
        api.get(`/tickets/${id}`),
        api.get(`/tickets/${id}/comments`),
        api.get(`/tickets/${id}/activities`)
      ])
      setTicket(ticketRes.data)
      setComments(commentsRes.data)
      setActivities(activitiesRes.data)
      if ((user?.role === 'DEPT_HEAD' || user?.role === 'SUPER_ADMIN') && ticketRes.data.departmentId) {
        api.get(`/tickets/dept/${ticketRes.data.departmentId}/agents`)
          .then(res => setAgents(res.data || []))
          .catch(() => {})
      }
    } catch (err) {
      toast.error('Failed to load ticket')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [id])

  const postComment = async () => {
    if (!newComment.trim()) return
    setActionLoading(true)
    try {
      await api.post(`/tickets/${id}/comments`, { comment: newComment, isInternal })
      setNewComment('')
      setIsInternal(false)
      toast.success('Comment added')
      load()
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to post comment')
    } finally {
      setActionLoading(false)
    }
  }

  const updateStatus = async (status, resolutionNote) => {
    setActionLoading(true)
    try {
      await api.put(`/tickets/${id}/status`, { status, resolutionNote: resolutionNote || null })
      toast.success(`Ticket ${status.toLowerCase().replace('_', ' ')}`)
      load()
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to update status')
    } finally {
      setActionLoading(false)
    }
  }

  const reopen = async () => {
    if (!reopenReason.trim()) return
    setActionLoading(true)
    try {
      await api.post(`/tickets/${id}/reopen`, { reason: reopenReason })
      setReopenReason('')
      toast.success('Ticket reopened')
      load()
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to reopen')
    } finally {
      setActionLoading(false)
    }
  }

  const submitRating = async () => {
    setActionLoading(true)
    try {
      await api.post(`/tickets/${id}/rate`, { rating: ratingVal, feedback })
      setRated(true)
      toast.success('Rating submitted')
      load()
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to submit rating')
    } finally {
      setActionLoading(false)
    }
  }

  const assignTicket = async () => {
    if (!selectedAgent) return
    setActionLoading(true)
    try {
      await api.put(`/tickets/${id}/assign`, { agentId: Number(selectedAgent) })
      toast.success('Ticket reassigned')
      setSelectedAgent('')
      load()
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to assign')
    } finally {
      setActionLoading(false)
    }
  }

  if (loading) return <div className="text-center py-12 text-gray-500">Loading ticket...</div>
  if (!ticket) return <div className="text-center py-12 text-red-500">Ticket not found</div>

  const isAgent = user?.role === 'AGENT'
  const isHead = user?.role === 'DEPT_HEAD' || user?.role === 'SUPER_ADMIN'
  const isEmployee = user?.role === 'EMPLOYEE'
  const canCommentInternal = isAgent || isHead

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <button onClick={() => navigate('/tickets')} className="text-sm text-indigo-600 hover:text-indigo-800 font-medium">
        &larr; Back to Tickets
      </button>

      {/* Ticket Info */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <div className="flex items-start justify-between mb-4">
          <div>
            <div className="flex items-center gap-3 mb-2">
              <span className="text-sm font-mono text-gray-400">{ticket.ticketNo}</span>
              <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${
                ticket.status === 'CLOSED' ? 'bg-gray-100 text-gray-600' :
                ticket.status === 'RESOLVED' ? 'bg-green-100 text-green-700' :
                ticket.status === 'IN_PROGRESS' ? 'bg-indigo-100 text-indigo-700' :
                ticket.status === 'ASSIGNED' ? 'bg-blue-100 text-blue-700' :
                ticket.status === 'REOPENED' ? 'bg-orange-100 text-orange-700' :
                'bg-yellow-100 text-yellow-700'
              }`}>{ticket.status.replace('_', ' ')}</span>
              <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${
                ticket.priority === 'CRITICAL' ? 'bg-red-100 text-red-700' :
                ticket.priority === 'HIGH' ? 'bg-orange-100 text-orange-700' :
                ticket.priority === 'MEDIUM' ? 'bg-yellow-100 text-yellow-700' :
                'bg-green-100 text-green-700'
              }`}>{ticket.priority}</span>
              {ticket.slaBreached && <span className="text-xs bg-red-100 text-red-700 px-2 py-0.5 rounded-full font-medium">SLA BREACHED</span>}
              {ticket.escalated && <span className="text-xs bg-orange-100 text-orange-700 px-2 py-0.5 rounded-full font-medium">ESCALATED</span>}
            </div>
            <h1 className="text-xl font-bold text-gray-900">{ticket.title}</h1>
          </div>
        </div>

        <p className="text-sm text-gray-600 mb-4 whitespace-pre-wrap">{ticket.description}</p>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-sm border-t border-gray-100 pt-4">
          <div><span className="text-gray-400 text-xs">Category</span><p className="font-medium text-gray-900">{ticket.category}</p></div>
          <div><span className="text-gray-400 text-xs">Department</span><p className="font-medium text-gray-900">{ticket.departmentName}</p></div>
          <div><span className="text-gray-400 text-xs">Raised By</span><p className="font-medium text-gray-900">{ticket.raisedByName}</p></div>
          <div><span className="text-gray-400 text-xs">Assigned To</span><p className="font-medium text-gray-900">{ticket.assignedToName || 'Unassigned'}</p></div>
          <div><span className="text-gray-400 text-xs">Created At</span><p className="font-medium text-gray-900">{ticket.createdAt ? new Date(ticket.createdAt).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : '-'}</p></div>
          <div>
            <span className="text-gray-400 text-xs">SLA Deadline</span>
            <p className="font-medium text-gray-900">
              {ticket.slaDeadline ? new Date(ticket.slaDeadline).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : '-'}
            </p>
            {ticket.slaDeadline && (
              <p className="mt-0.5"><SlaCountdown slaDeadline={ticket.slaDeadline} status={ticket.status} /></p>
            )}
          </div>
          {ticket.closedAt && <div><span className="text-gray-400 text-xs">Closed At</span><p className="font-medium text-gray-900">{new Date(ticket.closedAt).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })}</p></div>}
        </div>

        {ticket.resolutionNote && (
          <div className="mt-4 bg-green-50 border border-green-200 rounded-lg p-4">
            <p className="text-xs font-medium text-green-700 mb-1">Resolution Note</p>
            <p className="text-sm text-green-800">{ticket.resolutionNote}</p>
          </div>
        )}
      </div>

      {/* Actions */}
      {(isAgent || isHead || isEmployee) && ticket.status !== 'CLOSED' && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Actions</h2>
          <div className="flex flex-wrap gap-3">
            {(isAgent || isHead) && ticket.status === 'ASSIGNED' && (
              <button onClick={() => updateStatus('IN_PROGRESS')} disabled={actionLoading}
                className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-indigo-700 disabled:opacity-50">
                {actionLoading ? 'Processing...' : 'Start Progress'}
              </button>
            )}

            {(isAgent || isHead) && (ticket.status === 'IN_PROGRESS' || ticket.status === 'ASSIGNED' || ticket.status === 'REOPENED') && (
              <div className="flex items-center gap-2">
                <input type="text" placeholder="Resolution note" value={remarks} onChange={e => setRemarks(e.target.value)}
                  className="border border-gray-300 rounded-lg px-3 py-1.5 text-sm w-64" />
                <button onClick={() => { updateStatus('RESOLVED', remarks); setRemarks('') }} disabled={!remarks.trim() || actionLoading}
                  className="bg-green-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-green-700 disabled:opacity-50">
                  {actionLoading ? 'Processing...' : 'Resolve'}
                </button>
              </div>
            )}

            {isEmployee && ticket.status === 'RESOLVED' && (
              <>
                <button onClick={() => updateStatus('CLOSED')} disabled={actionLoading}
                  className="bg-gray-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-gray-700 disabled:opacity-50">
                  {actionLoading ? 'Processing...' : 'Close Ticket'}
                </button>
                <div className="flex items-center gap-2">
                  <input type="text" placeholder="Reason for reopening" value={reopenReason} onChange={e => setReopenReason(e.target.value)}
                    className="border border-gray-300 rounded-lg px-3 py-1.5 text-sm w-64" />
                  <button onClick={reopen} disabled={!reopenReason.trim() || actionLoading}
                    className="bg-orange-500 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-orange-600 disabled:opacity-50">
                    {actionLoading ? 'Processing...' : 'Reopen'}
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      )}

      {/* Reassign (Dept Head / Admin only) */}
      {isHead && agents.length > 0 && ticket.status !== 'CLOSED' && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Reassign Ticket</h2>
          <div className="flex items-center gap-3">
            <select value={selectedAgent} onChange={e => setSelectedAgent(e.target.value)}
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500">
              <option value="">Select agent...</option>
              {agents.map(a => (
                <option key={a.id} value={a.id}>{a.name} ({a.email})</option>
              ))}
            </select>
            <button onClick={assignTicket} disabled={!selectedAgent || actionLoading}
              className="bg-purple-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-purple-700 disabled:opacity-50">
              {actionLoading ? 'Assigning...' : 'Assign'}
            </button>
          </div>
        </div>
      )}

      {/* Activity Timeline */}
      {activities.length > 0 && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-5">Activity Timeline</h2>
          <div className="relative">
            <div className="absolute left-[11px] top-2 bottom-2 w-0.5 bg-gray-200" />
            <div className="space-y-1">
              {[...activities].reverse().map((a, i) => {
                const act = ACTIVITY_ICONS[a.action] || { bg: 'bg-gray-400', label: a.action, icon: '•' }
                return (
                  <div key={a.id} className="relative flex items-start gap-4 pl-0 py-2">
                    <div className={`relative z-10 w-6 h-6 rounded-full ${act.bg} flex items-center justify-center flex-shrink-0 ring-2 ring-white`}>
                      <span className="text-white text-[10px] font-bold">{act.icon}</span>
                    </div>
                    <div className="flex-1 min-w-0 -mt-0.5">
                      <div className="flex items-center gap-2 mb-0.5">
                        <span className="text-xs font-semibold text-gray-500 uppercase tracking-wide">{act.label}</span>
                        <span className="text-xs text-gray-400">by {a.performedByName}</span>
                        <span className="text-xs text-gray-400 ml-auto flex-shrink-0">
                          {new Date(a.createdAt).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })}
                        </span>
                      </div>
                      <p className="text-sm text-gray-700">{a.description}</p>
                    </div>
                  </div>
                )
              })}
            </div>
          </div>
        </div>
      )}

      {/* Rating (only for employee on closed tickets, hidden after submission) */}
      {isEmployee && ticket.status === 'CLOSED' && !rated && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Rate Resolution</h2>
          <div className="flex items-center gap-1 mb-3">
            {[1,2,3,4,5].map(i => (
              <button key={i} onClick={() => setRatingVal(i)}
                className={`text-3xl transition-colors ${i <= ratingVal ? 'text-yellow-400' : 'text-gray-300'} hover:text-yellow-400`}>
                &#9733;
              </button>
            ))}
          </div>
          <input type="text" placeholder="Optional feedback" value={feedback} onChange={e => setFeedback(e.target.value)}
            className="border border-gray-300 rounded-lg px-3 py-1.5 text-sm w-full mb-3" />
          <button onClick={submitRating} disabled={actionLoading}
            className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-indigo-700 disabled:opacity-50">
            {actionLoading ? 'Submitting...' : 'Submit Rating'}
          </button>
        </div>
      )}

      {isEmployee && ticket.status === 'CLOSED' && rated && (
        <div className="bg-green-50 border border-green-200 rounded-xl p-6 text-center">
          <p className="text-sm text-green-700 font-medium">Thank you! Your rating has been submitted.</p>
        </div>
      )}

      {/* Comments */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Comments ({comments.length})</h2>
        <div className="space-y-3 mb-4">
          {comments.length === 0 ? (
            <p className="text-sm text-gray-400">No comments yet</p>
          ) : comments.map(c => (
            <div key={c.id} className={`rounded-lg p-3 ${c.isInternal ? 'bg-purple-50 border border-purple-200' : 'bg-gray-50'}`}>
              <div className="flex items-center gap-2 mb-1">
                <span className="text-sm font-medium text-gray-900">{c.postedByName}</span>
                {c.isInternal && <span className="text-xs bg-purple-100 text-purple-700 px-1.5 py-0.5 rounded">Internal</span>}
                <span className="text-xs text-gray-400">{new Date(c.createdAt).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })}</span>
              </div>
              <p className="text-sm text-gray-600">{c.comment}</p>
            </div>
          ))}
        </div>
        {ticket.status !== 'CLOSED' && (
          <div className="space-y-2">
            <div className="flex gap-2">
              <input type="text" placeholder="Add a comment..." value={newComment}
                onChange={e => setNewComment(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && postComment()}
                className="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
              <button onClick={postComment} disabled={!newComment.trim() || actionLoading}
                className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-indigo-700 disabled:opacity-50">
                {actionLoading ? 'Posting...' : 'Post'}
              </button>
            </div>
            {canCommentInternal && (
              <label className="flex items-center gap-2 text-sm text-gray-500 cursor-pointer">
                <input type="checkbox" checked={isInternal} onChange={e => setIsInternal(e.target.checked)}
                  className="rounded" />
                Internal note (agents/heads only)
              </label>
            )}
          </div>
        )}
      </div>
    </div>
  )
}

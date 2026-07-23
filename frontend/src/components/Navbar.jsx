import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const roleLabels = {
  EMPLOYEE: 'Employee',
  AGENT: 'Agent',
  DEPT_HEAD: 'Dept Head',
  SUPER_ADMIN: 'Super Admin',
}

const roleColors = {
  EMPLOYEE: 'bg-blue-100 text-blue-800',
  AGENT: 'bg-green-100 text-green-800',
  DEPT_HEAD: 'bg-purple-100 text-purple-800',
  SUPER_ADMIN: 'bg-red-100 text-red-800',
}

export default function Navbar() {
  const { user, logout } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [mobileOpen, setMobileOpen] = useState(false)

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const links = [
    { to: '/', label: 'Dashboard' },
    { to: '/tickets', label: 'Tickets' },
    { to: '/tickets/new', label: 'New Ticket' },
  ]

  if (user?.role === 'SUPER_ADMIN') {
    links.push({ to: '/admin', label: 'Admin' })
  }

  const isActive = (path) => {
    if (path === '/') return location.pathname === '/'
    return location.pathname.startsWith(path)
  }

  return (
    <nav className="bg-white border-b border-gray-200 shadow-sm">
      <div className="max-w-7xl mx-auto px-4">
        <div className="flex items-center justify-between h-16">
          <div className="flex items-center gap-8">
            <Link to="/" className="text-xl font-bold text-indigo-600">OfficeDesk</Link>
            <div className="hidden sm:flex gap-1">
              {links.map(link => (
                <Link key={link.to} to={link.to}
                  className={`px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                    isActive(link.to)
                      ? 'bg-indigo-50 text-indigo-700'
                      : 'text-gray-600 hover:bg-gray-100'
                  }`}>
                  {link.label}
                </Link>
              ))}
            </div>
          </div>

          <div className="hidden sm:flex items-center gap-3">
            <span className={`text-xs px-2 py-1 rounded-full font-medium ${roleColors[user?.role] || ''}`}>
              {roleLabels[user?.role]}
            </span>
            <span className="text-sm text-gray-700">{user?.name}</span>
            <button onClick={handleLogout}
              className="text-sm text-gray-500 hover:text-red-600 transition-colors">
              Logout
            </button>
          </div>

          {/* Mobile menu button */}
          <button onClick={() => setMobileOpen(!mobileOpen)} className="sm:hidden text-gray-600 hover:text-gray-900">
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              {mobileOpen ? (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              ) : (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
              )}
            </svg>
          </button>
        </div>

        {/* Mobile menu */}
        {mobileOpen && (
          <div className="sm:hidden pb-4 space-y-1">
            {links.map(link => (
              <Link key={link.to} to={link.to} onClick={() => setMobileOpen(false)}
                className={`block px-3 py-2 rounded-md text-sm font-medium ${
                  isActive(link.to) ? 'bg-indigo-50 text-indigo-700' : 'text-gray-600 hover:bg-gray-100'
                }`}>
                {link.label}
              </Link>
            ))}
            <div className="border-t border-gray-200 mt-2 pt-2 px-3">
              <div className="flex items-center gap-2 mb-2">
                <span className={`text-xs px-2 py-1 rounded-full font-medium ${roleColors[user?.role] || ''}`}>
                  {roleLabels[user?.role]}
                </span>
                <span className="text-sm text-gray-700">{user?.name}</span>
              </div>
              <button onClick={handleLogout} className="text-sm text-red-600 hover:text-red-800 font-medium">Logout</button>
            </div>
          </div>
        )}
      </div>
    </nav>
  )
}

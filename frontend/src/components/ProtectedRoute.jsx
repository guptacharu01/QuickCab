import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { Spinner } from './ui.jsx'

const HOME = { RIDER: '/rider', DRIVER: '/driver', ADMIN: '/admin' }

/** Guards a route: must be logged in, and (optionally) have one of the roles. */
export default function ProtectedRoute({ children, roles }) {
  const { user, loading } = useAuth()
  if (loading) return <div className="max-w-6xl mx-auto px-4"><Spinner /></div>
  if (!user) return <Navigate to="/login" replace />
  if (roles && !roles.includes(user.role)) return <Navigate to={HOME[user.role] || '/'} replace />
  return children
}

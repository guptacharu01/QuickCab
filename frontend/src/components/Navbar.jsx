import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

const LINKS = {
  RIDER: [
    { to: '/rider', label: 'Book' },
    { to: '/history', label: 'My rides' },
    { to: '/support', label: 'Support' },
  ],
  DRIVER: [
    { to: '/driver', label: 'Dashboard' },
    { to: '/history', label: 'History' },
    { to: '/support', label: 'Support' },
  ],
  ADMIN: [{ to: '/admin', label: 'Admin' }],
}

export default function Navbar() {
  const { user, logout } = useAuth()
  const nav = useNavigate()

  return (
    <nav className="bg-gradient-to-r from-slate-950 via-violet-950 to-slate-950 text-white sticky top-0 z-10 shadow-lg shadow-violet-950/30">
      <div className="max-w-6xl mx-auto px-4 h-15 py-2.5 flex items-center justify-between">
        <Link to="/" className="flex items-center gap-2">
          <span className="grid place-items-center w-9 h-9 rounded-xl bg-gradient-to-br from-amber-300 to-orange-500 text-xl shadow-md shadow-orange-500/30">🚖</span>
          <span className="text-xl font-extrabold tracking-tight">
            Quick<span className="text-amber-300">Cab</span>
          </span>
        </Link>
        <div className="flex items-center gap-3">
          {user ? (
            <>
              {(LINKS[user.role] || []).map((l) => (
                <Link key={l.to} to={l.to} className="text-sm text-violet-200 hover:text-white font-medium transition">{l.label}</Link>
              ))}
              <span className="text-sm text-violet-200/80 hidden sm:inline">{user.name}</span>
              <button
                onClick={() => { logout(); nav('/login') }}
                className="text-sm font-medium px-3 py-1.5 rounded-xl bg-white/10 hover:bg-white/20 text-white transition">
                Logout
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="text-sm text-violet-200 hover:text-white font-medium transition">Login</Link>
              <Link to="/register" className="text-sm font-bold px-4 py-1.5 rounded-xl bg-gradient-to-r from-amber-300 to-orange-400 text-slate-950 hover:brightness-105 shadow-md shadow-orange-500/30 transition">Sign up</Link>
            </>
          )}
        </div>
      </div>
    </nav>
  )
}

import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './context/AuthContext.jsx'
import Navbar from './components/Navbar.jsx'
import ProtectedRoute from './components/ProtectedRoute.jsx'
import Landing from './pages/Landing.jsx'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import RiderDashboard from './pages/RiderDashboard.jsx'
import DriverDashboard from './pages/DriverDashboard.jsx'
import AdminDashboard from './pages/AdminDashboard.jsx'
import RideHistory from './pages/RideHistory.jsx'
import Support from './pages/Support.jsx'

const HOME = { RIDER: '/rider', DRIVER: '/driver', ADMIN: '/admin' }

function RoleHome() {
  const { user } = useAuth()
  return <Navigate to={user ? HOME[user.role] || '/' : '/'} replace />
}

export default function App() {
  return (
    <div className="min-h-screen">
      <Navbar />
      <main className="max-w-6xl mx-auto px-4 py-6">
        <Routes>
          <Route path="/" element={<Landing />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/home" element={<ProtectedRoute><RoleHome /></ProtectedRoute>} />
          <Route path="/rider" element={<ProtectedRoute roles={['RIDER']}><RiderDashboard /></ProtectedRoute>} />
          <Route path="/driver" element={<ProtectedRoute roles={['DRIVER']}><DriverDashboard /></ProtectedRoute>} />
          <Route path="/admin" element={<ProtectedRoute roles={['ADMIN']}><AdminDashboard /></ProtectedRoute>} />
          <Route path="/history" element={<ProtectedRoute roles={['RIDER', 'DRIVER']}><RideHistory /></ProtectedRoute>} />
          <Route path="/support" element={<ProtectedRoute roles={['RIDER', 'DRIVER']}><Support /></ProtectedRoute>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
      <footer className="text-center text-xs text-slate-400 pb-6">
        QuickCab demo · maps © OpenStreetMap contributors · routing via OSRM demo server
      </footer>
    </div>
  )
}

import { useEffect, useState } from 'react'
import api, { apiError } from '../services/api.js'
import StatusBadge, { pretty } from '../components/StatusBadge.jsx'
import { Card, Btn, Alert, Spinner, Empty, inputCls } from '../components/ui.jsx'

const inr = (v) => `₹${Number(v || 0).toFixed(2)}`
const TABS = ['Overview', 'Users', 'Rides', 'Payments', 'Tickets']

export default function AdminDashboard() {
  const [tab, setTab] = useState('Overview')
  const [stats, setStats] = useState(null)
  const [users, setUsers] = useState([])
  const [rides, setRides] = useState([])
  const [payments, setPayments] = useState([])
  const [tickets, setTickets] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    (async () => {
      try {
        const [s, u, r, p, t] = await Promise.all([
          api.get('/admin/stats'), api.get('/admin/users'), api.get('/admin/rides'),
          api.get('/admin/payments'), api.get('/admin/tickets'),
        ])
        setStats(s.data); setUsers(u.data); setRides(r.data); setPayments(p.data); setTickets(t.data)
      } catch (e) { setError(apiError(e, 'Could not load admin data')) }
      finally { setLoading(false) }
    })()
  }, [])

  const toggleActive = async (u) => {
    if (!window.confirm(`${u.active ? 'Deactivate' : 'Activate'} ${u.name}?`)) return
    try {
      const { data } = await api.put(`/admin/users/${u.id}/active`, { active: !u.active })
      setUsers(users.map((x) => (x.id === u.id ? data : x)))
    } catch (e) { setError(apiError(e)) }
  }

  const setTicketStatus = async (id, status) => {
    try {
      const { data } = await api.put(`/admin/tickets/${id}`, { status })
      setTickets(tickets.map((t) => (t.id === id ? data : t)))
    } catch (e) { setError(apiError(e)) }
  }

  if (loading) return <Spinner label="Loading admin console…" />

  return (
    <div className="space-y-5">
      <h1 className="text-2xl font-bold">Admin console</h1>
      {error && <Alert>{error}</Alert>}

      <div className="flex gap-2 flex-wrap">
        {TABS.map((t) => (
          <Btn key={t} variant={tab === t ? 'primary' : 'secondary'} onClick={() => setTab(t)}>{t}</Btn>
        ))}
      </div>

      {tab === 'Overview' && stats && (
        <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {[
            ['👥 Users', stats.totalUsers],
            ['🧍 Riders', stats.totalRiders],
            ['🚗 Drivers', `${stats.totalDrivers} (${stats.onlineDrivers} online)`],
            ['🛣️ Rides', `${stats.totalRides} (${stats.activeRides} active)`],
            ['✅ Completed', stats.completedRides],
            ['❌ Cancelled', stats.cancelledRides],
            ['💰 Revenue', inr(stats.totalRevenue)],
            ['⭐ Ratings', stats.totalRatings],
          ].map(([label, value]) => (
            <Card key={label}><p className="text-sm text-slate-500">{label}</p><p className="text-2xl font-bold">{value}</p></Card>
          ))}
        </div>
      )}

      {tab === 'Users' && (
        <Card>
          <table className="w-full text-sm">
            <thead><tr className="text-left text-slate-500 border-b">
              <th className="py-2">Name</th><th>Email</th><th>Role</th><th>Status</th><th></th>
            </tr></thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id} className="border-b border-slate-100">
                  <td className="py-2">{u.name}</td><td>{u.email}</td><td>{u.role}</td>
                  <td>{u.active ? '✅ active' : '⛔ deactivated'}</td>
                  <td className="text-right">
                    {u.role !== 'ADMIN' && (
                      <Btn variant="secondary" onClick={() => toggleActive(u)}>
                        {u.active ? 'Deactivate' : 'Activate'}
                      </Btn>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}

      {tab === 'Rides' && (
        <Card>
          {rides.length === 0 ? <Empty>No rides yet.</Empty> : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead><tr className="text-left text-slate-500 border-b">
                  <th className="py-2">ID</th><th>Route</th><th>Fare</th><th>Status</th><th>Booked</th>
                </tr></thead>
                <tbody>
                  {rides.map((r) => (
                    <tr key={r.id} className="border-b border-slate-100">
                      <td className="py-2">#{r.id}</td>
                      <td>{r.pickupLabel} → {r.dropoffLabel}</td>
                      <td>{inr(r.finalFare || r.estimatedFare)}</td>
                      <td><StatusBadge status={r.status} /></td>
                      <td className="text-slate-500">{new Date(r.createdAt).toLocaleString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      )}

      {tab === 'Payments' && (
        <Card>
          {payments.length === 0 ? <Empty>No payments yet.</Empty> : (
            <table className="w-full text-sm">
              <thead><tr className="text-left text-slate-500 border-b">
                <th className="py-2">ID</th><th>Ride</th><th>Amount</th><th>Status</th><th>Paid at</th>
              </tr></thead>
              <tbody>
                {payments.map((p) => (
                  <tr key={p.id} className="border-b border-slate-100">
                    <td className="py-2">#{p.id}</td><td>#{p.rideId}</td>
                    <td>{inr(p.amount)}</td><td><StatusBadge status={p.status} /></td>
                    <td className="text-slate-500">{p.createdAt ? new Date(p.createdAt).toLocaleString() : '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>
      )}

      {tab === 'Tickets' && (
        <Card>
          {tickets.length === 0 ? <Empty>No support tickets.</Empty> : (
            <div className="space-y-3">
              {tickets.map((t) => (
                <div key={t.id} className="border border-slate-200 rounded-xl p-3">
                  <div className="flex items-center justify-between mb-1">
                    <p className="font-semibold text-sm">#{t.id} · {t.subject} <span className="font-normal text-slate-500">by {t.userName}</span></p>
                    <StatusBadge status={t.status} />
                  </div>
                  <p className="text-sm text-slate-600 mb-2">{t.message}</p>
                  <select className={inputCls + ' !w-auto text-sm'} value={t.status}
                    onChange={(e) => setTicketStatus(t.id, e.target.value)}>
                    {['OPEN', 'IN_PROGRESS', 'CLOSED'].map((s) => <option key={s} value={s}>{pretty(s)}</option>)}
                  </select>
                </div>
              ))}
            </div>
          )}
        </Card>
      )}
    </div>
  )
}

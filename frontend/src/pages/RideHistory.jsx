import { useEffect, useState } from 'react'
import api, { apiError } from '../services/api.js'
import { useAuth } from '../context/AuthContext.jsx'
import StatusBadge from '../components/StatusBadge.jsx'
import { Card, Alert, Spinner, Empty, Btn } from '../components/ui.jsx'

const inr = (v) => `₹${Number(v || 0).toFixed(2)}`

/** Ride history for riders and drivers, with per-ride receipts. */
export default function RideHistory() {
  const { user } = useAuth()
  const [rides, setRides] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [receipts, setReceipts] = useState({})

  useEffect(() => {
    const url = user.role === 'DRIVER' ? '/driver/rides/history' : '/rides/mine'
    api.get(url)
      .then(({ data }) => setRides(data))
      .catch((e) => setError(apiError(e, 'Could not load history')))
      .finally(() => setLoading(false))
  }, [user.role])

  const showReceipt = async (ride) => {
    if (receipts[ride.id]) { setReceipts({ ...receipts, [ride.id]: null }); return }
    try {
      const { data } = await api.get(`/payments/rides/${ride.id}`)
      setReceipts({ ...receipts, [ride.id]: data })
    } catch (e) { setError(apiError(e, 'No receipt for this ride')) }
  }

  if (loading) return <Spinner label="Loading history…" />

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">Ride history</h1>
      {error && <Alert>{error}</Alert>}
      {rides.length === 0 ? <Card><Empty>No rides yet.</Empty></Card> : rides.map((r) => (
        <Card key={r.id}>
          <div className="flex flex-wrap items-center justify-between gap-2 mb-1">
            <p className="font-semibold">#{r.id} · {r.pickupLabel} → {r.dropoffLabel}</p>
            <StatusBadge status={r.status} />
          </div>
          <p className="text-sm text-slate-500 mb-2">
            {r.distanceKm} km · {new Date(r.createdAt).toLocaleString()}
            {r.driver && ` · Driver: ${r.driver.name} (${r.driver.vehicleModel})`}
          </p>
          {user.role === 'RIDER' && r.status === 'COMPLETED' && (
            <>
              <Btn variant="secondary" onClick={() => showReceipt(r)}>
                {receipts[r.id] ? 'Hide receipt' : '🧾 Receipt'}
              </Btn>
              {receipts[r.id] && (
                <div className="mt-2 bg-slate-50 rounded-xl p-3 text-sm">
                  <p>Amount: <b>{inr(receipts[r.id].amount)}</b> · <StatusBadge status={receipts[r.id].status} /></p>
                  <p className="text-slate-500">Paid at: {receipts[r.id].paidAt ? new Date(receipts[r.id].paidAt).toLocaleString() : '—'}</p>
                </div>
              )}
            </>
          )}
        </Card>
      ))}
    </div>
  )
}

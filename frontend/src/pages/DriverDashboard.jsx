import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { apiError } from '../services/api.js'
import MapPicker from '../components/MapPicker.jsx'
import StatusBadge from '../components/StatusBadge.jsx'
import { Card, Btn, Field, inputCls, Alert, Spinner, Empty } from '../components/ui.jsx'
import { useRideSocket } from '../hooks/useRideSocket.js'

const ACTIVE = ['ACCEPTED', 'DRIVER_ARRIVING', 'IN_PROGRESS']
const NEXT_STEP = {
  ACCEPTED: { status: 'DRIVER_ARRIVING', label: '📍 Mark arrived at pickup' },
  DRIVER_ARRIVING: { status: 'IN_PROGRESS', label: '▶️ Start trip' },
  IN_PROGRESS: { status: 'COMPLETED', label: '🏁 Complete trip' },
}
const inr = (v) => `₹${Number(v || 0).toFixed(2)}`

export default function DriverDashboard() {
  const [profile, setProfile] = useState(null)
  const [requests, setRequests] = useState([])
  const [activeRide, setActiveRide] = useState(null)
  const [ratings, setRatings] = useState([])
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [loading, setLoading] = useState(true)
  const [sharing, setSharing] = useState(false)
  const watchRef = useRef(null)
  const lastSent = useRef(0)

  const loadProfile = async () => {
    const { data } = await api.get('/driver/profile')
    setProfile(data)
    return data
  }
  const loadRequests = async () => {
    const { data } = await api.get('/driver/requests')
    setRequests(data)
  }
  const loadActive = async () => {
    const { data } = await api.get('/driver/rides/history')
    setActiveRide(data.find((r) => ACTIVE.includes(r.status)) || null)
  }

  const loadAll = async () => {
    try {
      await Promise.all([loadProfile(), loadRequests(), loadActive(),
        api.get('/driver/ratings').then(({ data }) => setRatings(data))])
    } catch (e) { setError(apiError(e, 'Could not load dashboard')) }
    finally { setLoading(false) }
  }
  useEffect(() => { loadAll() }, [])

  // New ride requests arrive live on channel 0.
  useRideSocket(0, (msg) => {
    if (msg.type === 'NEW_REQUEST') {
      setNotice(`🔔 New ride request #${msg.data.rideId}: ${msg.data.pickupLabel} → ${msg.data.dropoffLabel} (${inr(msg.data.fare)})`)
      loadRequests()
    }
  })
  // Status changes on the active ride (e.g. rider cancelled).
  useRideSocket(activeRide?.id, (msg) => {
    if (msg.type === 'STATUS') loadActive()
  })

  // Live location sharing while online.
  const startSharing = () => {
    if (!navigator.geolocation) { setError('Geolocation is not supported by this browser.'); return }
    setSharing(true)
    watchRef.current = navigator.geolocation.watchPosition(
      (pos) => {
        const now = Date.now()
        if (now - lastSent.current < 10000) return // at most every 10s
        lastSent.current = now
        api.post('/driver/location', { lat: pos.coords.latitude, lng: pos.coords.longitude }).catch(() => {})
      },
      () => { setError('Location permission denied — live tracking is off.'); setSharing(false) },
      { enableHighAccuracy: true }
    )
  }
  const stopSharing = () => {
    setSharing(false)
    if (watchRef.current) navigator.geolocation.clearWatch(watchRef.current)
  }
  useEffect(() => () => stopSharing(), []) // eslint-disable-line react-hooks/exhaustive-deps

  const toggleOnline = async () => {
    try {
      const next = !profile.online
      await api.put('/driver/availability', { online: next })
      setProfile({ ...profile, online: next })
      if (next) { loadRequests(); setNotice('You are online — ride requests will appear here.') }
      else { stopSharing(); setNotice('You are offline.') }
    } catch (e) { setError(apiError(e)) }
  }

  const accept = async (id) => {
    try {
      await api.post(`/driver/rides/${id}/accept`)
      setNotice('Ride accepted — head to the pickup!')
      await Promise.all([loadRequests(), loadActive()])
    } catch (e) { setError(apiError(e)); loadRequests() }
  }
  const reject = async (id) => {
    await api.post(`/driver/rides/${id}/reject`).catch(() => {})
    setRequests(requests.filter((r) => r.id !== id))
  }
  const advance = async () => {
    const step = NEXT_STEP[activeRide.status]
    if (!step) return
    try {
      const { data } = await api.post(`/driver/rides/${activeRide.id}/status`, { status: step.status })
      if (step.status === 'COMPLETED') {
        setActiveRide(null) // trip is done — it now lives in history
        setNotice('Trip completed — the rider can now pay.')
        loadProfile()
      } else {
        setActiveRide(data)
      }
    } catch (e) { setError(apiError(e)) }
  }

  if (loading) return <Spinner label="Loading dashboard…" />

  return (
    <div className="space-y-5">
      {error && <Alert>{error}</Alert>}
      {notice && <Alert kind="ok">{notice}</Alert>}

      <div className="grid md:grid-cols-3 gap-4">
        <Card>
          <h3 className="font-semibold mb-2">Status</h3>
          <p className="text-3xl mb-3">{profile.online ? '🟢 Online' : '⚪ Offline'}</p>
          <div className="flex gap-2">
            <Btn variant={profile.online ? 'secondary' : 'success'} onClick={toggleOnline}>
              {profile.online ? 'Go offline' : 'Go online'}
            </Btn>
            {profile.online && !sharing && <Btn variant="secondary" onClick={startSharing}>📡 Share location</Btn>}
            {sharing && <Btn variant="secondary" onClick={stopSharing}>Stop sharing</Btn>}
          </div>
          {!sharing && profile.online && <p className="text-xs text-slate-400 mt-2">Tip: share location so riders can track you live.</p>}
        </Card>
        <Card>
          <h3 className="font-semibold mb-2">Earnings</h3>
          <p className="text-3xl font-bold text-green-700">{inr(profile.totalEarnings)}</p>
          <p className="text-xs text-slate-400 mt-1">80% of completed fares</p>
        </Card>
        <Card>
          <h3 className="font-semibold mb-2">Rating</h3>
          <p className="text-3xl">⭐ {profile.ratingAvg.toFixed(1)}</p>
          <p className="text-xs text-slate-400 mt-1">{profile.ratingCount} reviews · {profile.vehicleModel} · {profile.vehiclePlate}</p>
        </Card>
      </div>

      {activeRide ? (
        <Card className="!border-blue-200">
          <div className="flex items-center justify-between mb-2">
            <h3 className="font-semibold">Active ride #{activeRide.id}</h3>
            <StatusBadge status={activeRide.status} />
          </div>
          <p className="text-sm text-slate-600 mb-3">
            {activeRide.pickupLabel} → {activeRide.dropoffLabel} · {activeRide.distanceKm} km · {inr(activeRide.finalFare || activeRide.estimatedFare)}
          </p>
          <MapPicker pickup={{ lat: activeRide.pickupLat, lng: activeRide.pickupLng }}
            dropoff={{ lat: activeRide.dropoffLat, lng: activeRide.dropoffLng }} height="h-64" />
          {NEXT_STEP[activeRide.status] && (
            <Btn onClick={advance} className="mt-3">{NEXT_STEP[activeRide.status].label}</Btn>
          )}
        </Card>
      ) : (
        <Card>
          <h3 className="font-semibold mb-3">Ride requests {profile.online ? '' : '(go online to receive them)'}</h3>
          {requests.length === 0 ? <Empty>No pending requests right now.</Empty> : (
            <div className="space-y-3">
              {requests.map((r) => (
                <div key={r.id} className="border border-slate-200 rounded-xl p-3 flex flex-wrap items-center justify-between gap-2">
                  <div className="text-sm">
                    <p className="font-semibold">#{r.id} · {r.pickupLabel} → {r.dropoffLabel}</p>
                    <p className="text-slate-500">{r.distanceKm} km · {inr(r.estimatedFare)}{r.promoCode ? ` · 🎟️ ${r.promoCode}` : ''}</p>
                  </div>
                  <div className="flex gap-2">
                    <Btn variant="success" onClick={() => accept(r.id)}>Accept</Btn>
                    <Btn variant="secondary" onClick={() => reject(r.id)}>Reject</Btn>
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>
      )}

      <div className="grid md:grid-cols-2 gap-4">
        <Card>
          <h3 className="font-semibold mb-3">Latest reviews</h3>
          {ratings.length === 0 ? <Empty>No reviews yet.</Empty> : (
            <div className="space-y-2 max-h-64 overflow-auto">
              {ratings.slice(0, 10).map((r) => (
                <div key={r.id} className="text-sm border-b border-slate-100 pb-2">
                  <span className="text-amber-400">{'★'.repeat(r.stars)}<span className="text-slate-300">{'★'.repeat(5 - r.stars)}</span></span>
                  <span className="text-slate-500"> · {r.riderName}</span>
                  {r.comment && <p className="text-slate-600">“{r.comment}”</p>}
                </div>
              ))}
            </div>
          )}
        </Card>
        <Card>
          <div className="flex items-center justify-between mb-3">
            <h3 className="font-semibold">Trip history</h3>
            <Link to="/history" className="text-sm text-indigo-600 hover:underline">Full history</Link>
          </div>
          <DriverHistoryPreview />
        </Card>
      </div>
    </div>
  )
}

function DriverHistoryPreview() {
  const [rides, setRides] = useState([])
  useEffect(() => {
    api.get('/driver/rides/history').then(({ data }) => setRides(data.slice(0, 5))).catch(() => {})
  }, [])
  if (rides.length === 0) return <Empty>No trips yet.</Empty>
  return (
    <div className="divide-y divide-slate-100">
      {rides.map((r) => (
        <div key={r.id} className="py-2 flex items-center justify-between text-sm">
          <span>#{r.id} · {r.pickupLabel} → {r.dropoffLabel}</span>
          <StatusBadge status={r.status} />
        </div>
      ))}
    </div>
  )
}

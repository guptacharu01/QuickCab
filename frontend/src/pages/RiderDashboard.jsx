import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { apiError } from '../services/api.js'
import MapPicker, { fetchRoute } from '../components/MapPicker.jsx'
import StatusBadge, { pretty } from '../components/StatusBadge.jsx'
import { Card, Btn, Field, inputCls, Alert, Spinner, Empty } from '../components/ui.jsx'
import { useRideSocket } from '../hooks/useRideSocket.js'

const ACTIVE = ['REQUESTED', 'ACCEPTED', 'DRIVER_ARRIVING', 'IN_PROGRESS']
const inr = (v) => `₹${Number(v || 0).toFixed(2)}`

export default function RiderDashboard() {
  // ---- booking state ----
  const [mode, setMode] = useState('pickup')
  const [pickup, setPickup] = useState(null)
  const [dropoff, setDropoff] = useState(null)
  const [pickupLabel, setPickupLabel] = useState('')
  const [dropoffLabel, setDropoffLabel] = useState('')
  const [promo, setPromo] = useState('')
  const [promoInfo, setPromoInfo] = useState(null)
  const [estimate, setEstimate] = useState(null)
  const [routeCoords, setRouteCoords] = useState([])
  const [booking, setBooking] = useState(false)

  // ---- ride state ----
  const [activeRide, setActiveRide] = useState(null)
  const [driverPos, setDriverPos] = useState(null)
  const [past, setPast] = useState([])
  const [payment, setPayment] = useState(null)
  const [payConfig, setPayConfig] = useState(null)
  const [rated, setRated] = useState(false)
  const [stars, setStars] = useState(5)
  const [comment, setComment] = useState('')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [loading, setLoading] = useState(true)

  const load = async () => {
    try {
      const { data } = await api.get('/rides/mine')
      setActiveRide(data.find((r) => ACTIVE.includes(r.status)) || null)
      setPast(data.filter((r) => !ACTIVE.includes(r.status)).slice(0, 5))
    } catch (e) { setError(apiError(e, 'Could not load rides')) }
    finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  // Live updates for the active ride.
  useRideSocket(activeRide?.id, (msg) => {
    if (msg.type === 'STATUS') {
      api.get(`/rides/${activeRide.id}`).then(({ data }) => {
        setActiveRide(data)
        if (data.status === 'COMPLETED') loadPayment(data.id)
      })
    } else if (msg.type === 'LOCATION') {
      setDriverPos({ lat: msg.data.lat, lng: msg.data.lng })
    } else if (msg.type === 'DRIVER_DECLINED') {
      setNotice('A driver declined — still looking for a driver…')
    }
  })

  // Fare estimate + route preview whenever both points are set.
  useEffect(() => {
    if (!pickup || !dropoff) { setEstimate(null); setRouteCoords([]); return }
    const t = setTimeout(async () => {
      try {
        const { data } = await api.post('/rides/estimate', {
          pickupLat: pickup.lat, pickupLng: pickup.lng,
          dropoffLat: dropoff.lat, dropoffLng: dropoff.lng,
        })
        setEstimate(data)
        setRouteCoords(await fetchRoute(pickup, dropoff))
      } catch { /* estimate failed; booking will surface the error */ }
    }, 400)
    return () => clearTimeout(t)
  }, [pickup, dropoff])

  const onPick = (p) => (mode === 'pickup' ? setPickup(p) : setDropoff(p))

  const useMyLocation = () => {
    if (!navigator.geolocation) { setError('Geolocation is not supported by this browser.'); return }
    navigator.geolocation.getCurrentPosition(
      (pos) => setPickup({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
      () => setError('Location permission denied — click the map instead.')
    )
  }

  const checkPromo = async () => {
    if (!promo.trim()) { setPromoInfo(null); return }
    try {
      const { data } = await api.get('/promos/validate', { params: { code: promo.trim() } })
      setPromoInfo(data); setNotice(`Promo ${data.code} applied at booking.`)
    } catch (e) { setPromoInfo(null); setError(apiError(e, 'Invalid promo code')) }
  }

  const book = async () => {
    setError(''); setNotice('')
    if (!pickup || !dropoff) { setError('Set both pickup and destination on the map.'); return }
    setBooking(true)
    try {
      const { data } = await api.post('/rides', {
        pickupLat: pickup.lat, pickupLng: pickup.lng,
        dropoffLat: dropoff.lat, dropoffLng: dropoff.lng,
        pickupLabel: pickupLabel || 'Pickup', dropoffLabel: dropoffLabel || 'Destination',
        promoCode: promoInfo ? promoInfo.code : undefined,
      })
      setActiveRide(data)
      setDriverPos(null)
      setNotice('Ride requested — notifying nearby drivers…')
    } catch (e) { setError(apiError(e, 'Booking failed')) }
    finally { setBooking(false) }
  }

  const cancel = async () => {
    if (!window.confirm('Cancel this ride?')) return
    try {
      await api.post(`/rides/${activeRide.id}/cancel`)
      setActiveRide(null); setDriverPos(null); load(); setNotice('Ride cancelled.')
    } catch (e) { setError(apiError(e)) }
  }

  const sos = async () => {
    const note = window.prompt('Describe the emergency:', 'I need help')
    if (note === null) return
    try {
      await api.post(`/rides/${activeRide.id}/sos`, { note })
      setNotice('SOS sent — the driver and admin have been alerted.')
    } catch (e) { setError(apiError(e)) }
  }

  const loadPayment = async (rideId) => {
    try {
      const { data } = await api.get(`/payments/rides/${rideId}`)
      setPayment(data)
      if (data.status === 'SUCCEEDED') {
        // If a rating already exists the backend will reject a duplicate; check cheaply.
        setRated(false)
      }
    } catch { setPayment(null) }
    try {
      const { data } = await api.get('/payments/config')
      setPayConfig(data)
    } catch { /* ignore */ }
  }
  useEffect(() => {
    if (activeRide?.status === 'COMPLETED') loadPayment(activeRide.id)
    else { setPayment(null); setRated(false) }
  }, [activeRide?.id, activeRide?.status])

  const submitRating = async () => {
    try {
      await api.post(`/rides/${activeRide.id}/rate`, { stars, comment })
      setRated(true); setNotice('Thanks for your feedback!')
    } catch (e) { setError(apiError(e)) }
  }

  if (loading) return <Spinner label="Loading your rides…" />

  return (
    <div className="space-y-5">
      {error && <Alert>{error}</Alert>}
      {notice && <Alert kind="ok">{notice}</Alert>}

      {activeRide ? (
        <ActiveRideCard ride={activeRide} driverPos={driverPos} payment={payment} payConfig={payConfig}
          rated={rated} stars={stars} setStars={setStars} comment={comment} setComment={setComment}
          onCancel={cancel} onSos={sos} onPaid={() => loadPayment(activeRide.id)} onRated={submitRating} />
      ) : (
        <Card>
          <h2 className="text-xl font-bold mb-1">Book a ride</h2>
          <p className="text-sm text-slate-500 mb-4">Tap the buttons, then click the map to drop your pins.</p>
          <div className="grid lg:grid-cols-2 gap-5">
            <div>
              <div className="flex gap-2 mb-3">
                <Btn variant={mode === 'pickup' ? 'primary' : 'secondary'} onClick={() => setMode('pickup')}>
                  📍 Pickup {pickup && '✓'}
                </Btn>
                <Btn variant={mode === 'dropoff' ? 'primary' : 'secondary'} onClick={() => setMode('dropoff')}>
                  🏁 Drop {dropoff && '✓'}
                </Btn>
                <Btn variant="ghost" onClick={useMyLocation}>Use my location</Btn>
              </div>
              <MapPicker pickup={pickup} dropoff={dropoff} routeCoords={routeCoords} mode={mode} onPick={onPick} />
            </div>
            <div>
              <Field label="Pickup label"><input className={inputCls} value={pickupLabel} onChange={(e) => setPickupLabel(e.target.value)} placeholder="e.g. Home" /></Field>
              <Field label="Destination label"><input className={inputCls} value={dropoffLabel} onChange={(e) => setDropoffLabel(e.target.value)} placeholder="e.g. Office" /></Field>
              <div className="flex gap-2 mb-3">
                <input className={inputCls} value={promo} onChange={(e) => setPromo(e.target.value)} placeholder="Promo code (optional)" />
                <Btn variant="secondary" onClick={checkPromo}>Apply</Btn>
              </div>
              {estimate ? (
                <div className="bg-indigo-50 border border-indigo-100 rounded-xl p-4 mb-4 text-sm">
                  <div className="flex justify-between"><span className="text-slate-500">Distance</span><b>{estimate.distanceKm} km</b></div>
                  <div className="flex justify-between"><span className="text-slate-500">ETA</span><b>~{estimate.etaMinutes} min</b></div>
                  <div className="flex justify-between text-lg mt-1"><span className="text-slate-500">Estimate</span><b className="text-indigo-700">{inr(estimate.estimatedFare)}</b></div>
                  {promoInfo && <p className="text-green-700 text-xs mt-1">Promo {promoInfo.code} will be applied at booking.</p>}
                </div>
              ) : (
                <p className="text-sm text-slate-400 mb-4">Set pickup and destination to see the fare.</p>
              )}
              <Btn onClick={book} disabled={booking || !pickup || !dropoff} className="w-full !py-3 !text-base">
                {booking ? 'Booking…' : 'Book ride'}
              </Btn>
            </div>
          </div>
        </Card>
      )}

      <Card>
        <div className="flex items-center justify-between mb-3">
          <h3 className="font-semibold">Recent rides</h3>
          <Link to="/history" className="text-sm text-indigo-600 hover:underline">View all</Link>
        </div>
        {past.length === 0 ? <Empty>No past rides yet — book your first trip above.</Empty> : (
          <div className="divide-y divide-slate-100">
            {past.map((r) => (
              <div key={r.id} className="py-2 flex items-center justify-between text-sm">
                <span>#{r.id} · {r.pickupLabel} → {r.dropoffLabel}</span>
                <span className="flex items-center gap-2"><b>{inr(r.finalFare || r.estimatedFare)}</b><StatusBadge status={r.status} /></span>
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  )
}

function ActiveRideCard({ ride, driverPos, payment, payConfig, rated, stars, setStars, comment, setComment, onCancel, onSos, onPaid, onRated }) {
  const [intent, setIntent] = useState(null)
  const [payBusy, setPayBusy] = useState(false)
  const [payError, setPayError] = useState('')

  const startPay = async () => {
    setPayError('')
    setPayBusy(true)
    try {
      const { data } = await api.post(`/payments/rides/${ride.id}/intent`)
      if (!data.configured) { setPayError('Online payment is not configured. Please pay the driver directly in cash.'); return }
      setIntent(data)
    } catch (e) { setPayError(apiError(e)) }
    finally { setPayBusy(false) }
  }

  return (
    <Card className="!border-indigo-200">
      <div className="flex items-center justify-between mb-3">
        <h2 className="text-xl font-bold">Ride #{ride.id}</h2>
        <StatusBadge status={ride.status} />
      </div>
      <p className="text-sm text-slate-600 mb-3">{ride.pickupLabel} → {ride.dropoffLabel} · {ride.distanceKm} km · <b>{inr(ride.finalFare || ride.estimatedFare)}</b></p>

      {ride.driver ? (
        <div className="bg-slate-50 rounded-xl p-3 mb-3 text-sm">
          <p className="font-semibold">{ride.driver.name} <span className="font-normal text-slate-500">⭐ {ride.driver.ratingAvg.toFixed(1)}</span></p>
          <p className="text-slate-500">{ride.driver.vehicleModel} · {ride.driver.vehiclePlate} · {ride.driver.phone}</p>
        </div>
      ) : (
        <p className="text-sm text-amber-700 bg-amber-50 rounded-xl p-3 mb-3">Looking for a driver… you'll be notified live when one accepts.</p>
      )}

      {(ride.status === 'ACCEPTED' || ride.status === 'DRIVER_ARRIVING' || ride.status === 'IN_PROGRESS') && (
        <div className="mb-3">
          <MapPicker pickup={{ lat: ride.pickupLat, lng: ride.pickupLng }} dropoff={{ lat: ride.dropoffLat, lng: ride.dropoffLng }}
            driverPos={driverPos} height="h-64" />
          <p className="text-xs mt-2 flex items-center gap-2">
            {driverPos ? (
              <>
                <span className="live-dot w-2.5 h-2.5 rounded-full bg-green-500" />
                <span className="text-green-700 font-semibold">Live — your driver is on the move 🚖</span>
              </>
            ) : (
              <span className="text-slate-400">Waiting for the driver to share location… (driver taps “📡 Share location”)</span>
            )}
          </p>
        </div>
      )}

      <div className="flex flex-wrap gap-2 mb-2">
        {(ride.status === 'REQUESTED' || ride.status === 'ACCEPTED') && <Btn variant="danger" onClick={onCancel}>Cancel ride</Btn>}
        {ACTIVE.includes(ride.status) && <Btn variant="secondary" onClick={onSos}>🚨 SOS</Btn>}
      </div>

      {ride.status === 'COMPLETED' && (
        <div className="border-t border-slate-100 mt-3 pt-3">
          <h3 className="font-semibold mb-2">Payment</h3>
          {!payment ? <Spinner label="Loading payment…" /> : (
            <>
              <p className="text-sm mb-2">Amount due: <b>{inr(payment.amount)}</b> · <StatusBadge status={payment.status} /></p>
              {payment.status === 'PENDING' && (
                !payConfig ? <Spinner label="Loading payment…" /> :
                !intent ? (
                  <>
                    {payError && <Alert>{payError}</Alert>}
                    <Btn onClick={startPay} disabled={payBusy}>{payBusy ? 'Preparing…' : `Pay ${inr(payment.amount)}`}</Btn>
                    <p className="text-xs text-slate-400 mt-1">Test mode — use card 4242 4242 4242 4242.</p>
                  </>
                ) : (
                  <StripePay rideId={ride.id} publishableKey={payConfig.publishableKey} clientSecret={intent.clientSecret} onDone={onPaid} />
                )
              )}
              {payment.status === 'SUCCEEDED' && !rated && (
                <div className="mt-3 bg-slate-50 rounded-xl p-3">
                  <h4 className="font-semibold text-sm mb-2">Rate your driver</h4>
                  <div className="flex gap-1 mb-2">
                    {[1, 2, 3, 4, 5].map((s) => (
                      <button key={s} onClick={() => setStars(s)} className={`text-2xl ${s <= stars ? 'text-amber-400' : 'text-slate-300'}`}>★</button>
                    ))}
                  </div>
                  <textarea className={inputCls + ' mb-2'} rows="2" value={comment} onChange={(e) => setComment(e.target.value)} placeholder="How was your ride? (optional)" />
                  <Btn onClick={onRated}>Submit rating</Btn>
                </div>
              )}
              {payment.status === 'SUCCEEDED' && rated && <p className="text-sm text-green-700">Thanks — your rating was recorded.</p>}
            </>
          )}
        </div>
      )}
    </Card>
  )
}

/** Real Stripe.js card form (test mode). Mounted imperatively to avoid extra deps. */
function StripePay({ rideId, publishableKey, clientSecret, onDone }) {
  const mountRef = useRef(null)
  const [stripe, setStripe] = useState(null)
  const [card, setCard] = useState(null)
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    let cancelled = false
    let el = null
    if (!publishableKey) {
      setErr('The card form could not load because the Stripe publishable key is missing. Make sure STRIPE_PUBLISHABLE_KEY is set in the backend .env file and the backend was restarted, then refresh this page.')
      return
    }
    import('@stripe/stripe-js')
      .then(({ loadStripe }) => loadStripe(publishableKey))
      .then((s) => {
        if (cancelled) return
        if (!s) throw new Error('Stripe.js failed to initialise')
        setStripe(s)
        el = s.elements().create('card')
        el.mount(mountRef.current)
        setCard(el)
      })
      .catch(() => {
        if (!cancelled) setErr('Could not load the secure card form. This is usually caused by an ad-blocker — disable it for this site and refresh the page.')
      })
    return () => { cancelled = true; try { el && el.unmount() } catch {} }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const pay = async () => {
    setErr(''); setBusy(true)
    try {
      const { error } = await stripe.confirmCardPayment(clientSecret, { payment_method: { card } })
      if (error) { setErr(error.message); setBusy(false); return }
      await api.post(`/payments/rides/${rideId}/confirm`) // backend re-verifies with Stripe
      onDone()
    } catch (e) { setErr(apiError(e)) }
    finally { setBusy(false) }
  }

  return (
    <div className="bg-slate-50 rounded-xl p-3">
      {err && <Alert>{err}</Alert>}
      <div ref={mountRef} className="bg-white border border-slate-300 rounded-lg p-3 mb-2" />
      <Btn onClick={pay} disabled={!stripe || !card || busy}>{busy ? 'Processing…' : 'Confirm payment'}</Btn>
    </div>
  )
}

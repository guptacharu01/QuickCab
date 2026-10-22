import { useEffect, useRef } from 'react'

/** WebSocket URL for a ride channel (rideId=0 is the driver request pool). */
export function wsUrl(rideId) {
  const httpBase = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'
  const wsBase = httpBase.replace(/^http/, 'ws').replace(/\/api$/, '')
  const token = localStorage.getItem('qc_token')
  return `${wsBase}/ws/rides?rideId=${rideId}&token=${token}`
}

/** Subscribes to live events for one ride; reconnects when rideId changes. */
export function useRideSocket(rideId, onEvent) {
  const cb = useRef(onEvent)
  cb.current = onEvent
  useEffect(() => {
    if (!rideId) return
    let ws
    try {
      ws = new WebSocket(wsUrl(rideId))
    } catch { return }
    ws.onmessage = (e) => {
      try { cb.current(JSON.parse(e.data)) } catch { /* ignore */ }
    }
    return () => { try { ws.close() } catch {} }
  }, [rideId])
}

import { useEffect, useRef } from 'react'
import L from 'leaflet'

/**
 * Leaflet + OpenStreetMap picker. Click the map to drop the pickup / drop pin
 * (decided by the `mode` prop). Also draws the route polyline and the live
 * driver marker when provided. Circle markers are used so no image assets
 * are needed.
 */
export default function MapPicker({ pickup, dropoff, driverPos, routeCoords, mode, onPick, height = 'h-80' }) {
  const divRef = useRef(null)
  const mapRef = useRef(null)
  const layerRef = useRef(null)
  const pickRef = useRef(onPick)
  pickRef.current = onPick

  useEffect(() => {
    if (mapRef.current) return
    const map = L.map(divRef.current).setView([28.6139, 77.209], 11) // Delhi NCR default
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      maxZoom: 19,
    }).addTo(map)
    layerRef.current = L.layerGroup().addTo(map)
    map.on('click', (e) => pickRef.current && pickRef.current({ lat: e.latlng.lat, lng: e.latlng.lng }))
    mapRef.current = map
    return () => { map.remove(); mapRef.current = null }
  }, [])

  useEffect(() => {
    const g = layerRef.current
    const map = mapRef.current
    if (!g || !map) return
    g.clearLayers()
    const dot = (p, color, label) =>
      L.circleMarker([p.lat, p.lng], { radius: 9, color, fillColor: color, fillOpacity: 0.9 })
        .addTo(g).bindTooltip(label)
    if (pickup) dot(pickup, '#16a34a', 'Pickup')
    if (dropoff) dot(dropoff, '#dc2626', 'Drop')
    if (driverPos) dot(driverPos, '#2563eb', 'Driver')
    if (routeCoords && routeCoords.length > 1) L.polyline(routeCoords, { color: '#4f46e5', weight: 4 }).addTo(g)
    const pts = [pickup, dropoff, driverPos].filter(Boolean).map((p) => [p.lat, p.lng])
    if (pts.length > 1) map.fitBounds(L.latLngBounds(pts).pad(0.25))
    else if (pts.length === 1) map.setView(pts[0], 14)
  }, [pickup, dropoff, driverPos, routeCoords])

  return (
    <div>
      {mode && <p className="text-xs text-slate-500 mb-1">Click the map to set the <b>{mode}</b> point</p>}
      <div ref={divRef} className={`${height} w-full`} />
      <p className="text-[11px] text-slate-400 mt-1">Map data © OpenStreetMap contributors</p>
    </div>
  )
}

/**
 * Fetch a driving route from the public OSRM demo server.
 * Free for light use; falls back to a straight line if unavailable.
 */
export async function fetchRoute(a, b) {
  try {
    const url = `https://router.project-osrm.org/route/v1/driving/${a.lng},${a.lat};${b.lng},${b.lat}?overview=full&geometries=geojson`
    const res = await fetch(url)
    const json = await res.json()
    const coords = json?.routes?.[0]?.geometry?.coordinates
    if (coords && coords.length > 1) return coords.map(([lng, lat]) => ({ lat, lng }))
  } catch { /* fall through to straight line */ }
  return [a, b]
}

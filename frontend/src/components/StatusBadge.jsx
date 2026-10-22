const COLORS = {
  REQUESTED: 'bg-amber-100 text-amber-800',
  ACCEPTED: 'bg-blue-100 text-blue-800',
  DRIVER_ARRIVING: 'bg-sky-100 text-sky-800',
  IN_PROGRESS: 'bg-indigo-100 text-indigo-800',
  COMPLETED: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-slate-200 text-slate-600',
  PENDING: 'bg-amber-100 text-amber-800',
  SUCCEEDED: 'bg-green-100 text-green-800',
  FAILED: 'bg-red-100 text-red-700',
  OPEN: 'bg-amber-100 text-amber-800',
  IN_PROGRESS_T: 'bg-blue-100 text-blue-800',
  CLOSED: 'bg-slate-200 text-slate-600',
}

export const pretty = (s) => (s || '').replace(/_/g, ' ')

export default function StatusBadge({ status }) {
  return (
    <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-semibold ${COLORS[status] || 'bg-slate-100 text-slate-700'}`}>
      {pretty(status)}
    </span>
  )
}

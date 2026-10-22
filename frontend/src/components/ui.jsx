/** Tiny reusable UI pieces so pages stay readable. */
export function Card({ children, className = '' }) {
  return (
    <div className={`rise bg-white/85 backdrop-blur rounded-2xl border border-white shadow-[0_10px_36px_rgb(88_28_135/0.08)] p-5 ${className}`}>
      {children}
    </div>
  )
}

export function Btn({ children, onClick, type = 'button', variant = 'primary', disabled, className = '' }) {
  const styles = {
    primary: 'bg-gradient-to-r from-violet-600 to-fuchsia-600 hover:from-violet-700 hover:to-fuchsia-700 text-white shadow-lg shadow-fuchsia-500/30',
    secondary: 'bg-white hover:bg-violet-50 text-violet-900 border border-violet-200 shadow-sm',
    danger: 'bg-gradient-to-r from-rose-500 to-red-600 hover:from-rose-600 hover:to-red-700 text-white shadow-lg shadow-red-500/25',
    success: 'bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-600 hover:to-teal-600 text-white shadow-lg shadow-emerald-500/25',
    ghost: 'text-fuchsia-700 hover:text-fuchsia-900 hover:bg-fuchsia-50',
  }[variant]
  return (
    <button type={type} onClick={onClick} disabled={disabled}
      className={`px-4 py-2 rounded-xl text-sm font-semibold transition active:scale-[.98] disabled:opacity-50 disabled:cursor-not-allowed disabled:active:scale-100 ${styles} ${className}`}>
      {children}
    </button>
  )
}

export function Field({ label, children, hint }) {
  return (
    <label className="block mb-3">
      <span className="text-sm font-semibold text-slate-700">{label}</span>
      <div className="mt-1">{children}</div>
      {hint && <span className="text-xs text-slate-400">{hint}</span>}
    </label>
  )
}

export const inputCls =
  'w-full px-3 py-2 bg-white border border-violet-200 rounded-xl text-sm focus:ring-2 focus:ring-fuchsia-400 focus:border-fuchsia-400 placeholder:text-slate-400'

export function Spinner({ label = 'Loading…' }) {
  return (
    <div className="flex items-center gap-2 text-violet-700 text-sm py-6 justify-center">
      <span className="inline-block w-4 h-4 border-2 border-fuchsia-500 border-t-transparent rounded-full animate-spin" />
      {label}
    </div>
  )
}

export function Alert({ kind = 'error', children }) {
  const cls = kind === 'error'
    ? 'bg-rose-50/90 border-rose-200 text-rose-700'
    : 'bg-emerald-50/90 border-emerald-200 text-emerald-700'
  return <div className={`rise border rounded-xl px-3 py-2 text-sm mb-3 ${cls}`}>{children}</div>
}

export function Empty({ children }) {
  return <p className="text-slate-400 text-sm py-6 text-center">{children}</p>
}

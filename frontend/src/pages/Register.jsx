import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { Card, Btn, Field, inputCls, Alert } from '../components/ui.jsx'
import { apiError } from '../services/api.js'

const HOME = { RIDER: '/rider', DRIVER: '/driver', ADMIN: '/admin' }

export default function Register() {
  const { register } = useAuth()
  const nav = useNavigate()
  const [form, setForm] = useState({ name: '', email: '', phone: '', password: '', role: 'RIDER', vehicleModel: '', vehiclePlate: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    if (!form.name || !form.email || !form.phone || !form.password) { setError('All fields are required.'); return }
    if (form.password.length < 6) { setError('Password must be at least 6 characters.'); return }
    if (form.role === 'DRIVER' && (!form.vehicleModel || !form.vehiclePlate)) {
      setError('Drivers must enter vehicle details.'); return
    }
    setBusy(true)
    try {
      const payload = { ...form, email: form.email.trim() }
      if (payload.role === 'RIDER') { delete payload.vehicleModel; delete payload.vehiclePlate }
      const data = await register(payload)
      nav(HOME[data.role] || '/', { replace: true })
    } catch (err) {
      setError(apiError(err, 'Registration failed'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="max-w-md mx-auto">
      <Card>
        <h1 className="text-2xl font-bold mb-1">Create your account</h1>
        <p className="text-sm text-slate-500 mb-5">Riders book rides · drivers earn on every trip</p>
        {error && <Alert>{error}</Alert>}
        <form onSubmit={submit}>
          <Field label="I am a…">
            <select className={inputCls} value={form.role} onChange={set('role')}>
              <option value="RIDER">Rider — I need rides</option>
              <option value="DRIVER">Driver — I offer rides</option>
            </select>
          </Field>
          <Field label="Full name"><input className={inputCls} value={form.name} onChange={set('name')} placeholder="Aarav Sharma" /></Field>
          <Field label="Email"><input className={inputCls} type="email" value={form.email} onChange={set('email')} placeholder="you@example.com" /></Field>
          <Field label="Phone"><input className={inputCls} value={form.phone} onChange={set('phone')} placeholder="98765 43210" /></Field>
          <Field label="Password" hint="At least 6 characters">
            <input className={inputCls} type="password" value={form.password} onChange={set('password')} placeholder="••••••••" />
          </Field>
          {form.role === 'DRIVER' && (
            <>
              <Field label="Vehicle model"><input className={inputCls} value={form.vehicleModel} onChange={set('vehicleModel')} placeholder="Swift Dzire" /></Field>
              <Field label="Vehicle plate"><input className={inputCls} value={form.vehiclePlate} onChange={set('vehiclePlate')} placeholder="DL 8C 1234" /></Field>
            </>
          )}
          <Btn type="submit" disabled={busy} className="w-full !py-2.5">{busy ? 'Creating…' : 'Create account'}</Btn>
        </form>
        <p className="text-sm text-slate-500 mt-4 text-center">
          Have an account? <Link to="/login" className="text-indigo-600 hover:underline">Log in</Link>
        </p>
      </Card>
    </div>
  )
}

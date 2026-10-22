import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { Card, Btn, Field, inputCls, Alert } from '../components/ui.jsx'
import { apiError } from '../services/api.js'

const HOME = { RIDER: '/rider', DRIVER: '/driver', ADMIN: '/admin' }

export default function Login() {
  const { login } = useAuth()
  const nav = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    if (!email || !password) { setError('Email and password are required.'); return }
    setBusy(true)
    try {
      const data = await login(email.trim(), password)
      nav(HOME[data.role] || '/', { replace: true })
    } catch (err) {
      setError(apiError(err, 'Login failed'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="max-w-md mx-auto">
      <Card>
        <h1 className="text-2xl font-bold mb-1">Welcome back</h1>
        <p className="text-sm text-slate-500 mb-5">Log in to your QuickCab account</p>
        {error && <Alert>{error}</Alert>}
        <form onSubmit={submit}>
          <Field label="Email">
            <input className={inputCls} type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="you@example.com" />
          </Field>
          <Field label="Password">
            <input className={inputCls} type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="••••••••" />
          </Field>
          <Btn type="submit" disabled={busy} className="w-full !py-2.5">{busy ? 'Logging in…' : 'Log in'}</Btn>
        </form>
        <p className="text-sm text-slate-500 mt-4 text-center">
          New here? <Link to="/register" className="text-indigo-600 hover:underline">Create an account</Link>
        </p>
      </Card>
    </div>
  )
}

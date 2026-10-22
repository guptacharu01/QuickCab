import { useEffect, useState } from 'react'
import api, { apiError } from '../services/api.js'
import StatusBadge from '../components/StatusBadge.jsx'
import { Card, Btn, Field, inputCls, Alert, Spinner, Empty } from '../components/ui.jsx'

/** Raise and track support tickets. */
export default function Support() {
  const [tickets, setTickets] = useState([])
  const [subject, setSubject] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)

  const load = () => api.get('/tickets').then(({ data }) => setTickets(data)).catch(() => {})
  useEffect(() => { load().finally(() => setLoading(false)) }, [])

  const submit = async (e) => {
    e.preventDefault()
    setError(''); setNotice('')
    if (!subject.trim() || !message.trim()) { setError('Subject and message are required.'); return }
    setBusy(true)
    try {
      await api.post('/tickets', { subject: subject.trim(), message: message.trim() })
      setSubject(''); setMessage('')
      setNotice('Ticket raised — our team will get back to you.')
      load()
    } catch (err) { setError(apiError(err)) }
    finally { setBusy(false) }
  }

  if (loading) return <Spinner />

  return (
    <div className="space-y-5 max-w-2xl">
      <h1 className="text-2xl font-bold">Support</h1>
      {error && <Alert>{error}</Alert>}
      {notice && <Alert kind="ok">{notice}</Alert>}

      <Card>
        <h2 className="font-semibold mb-3">Raise a ticket</h2>
        <form onSubmit={submit}>
          <Field label="Subject"><input className={inputCls} value={subject} onChange={(e) => setSubject(e.target.value)} placeholder="e.g. Fare looks wrong" /></Field>
          <Field label="Message"><textarea className={inputCls} rows="4" value={message} onChange={(e) => setMessage(e.target.value)} placeholder="Describe the issue…" /></Field>
          <Btn type="submit" disabled={busy}>{busy ? 'Sending…' : 'Submit ticket'}</Btn>
        </form>
      </Card>

      <Card>
        <h2 className="font-semibold mb-3">My tickets</h2>
        {tickets.length === 0 ? <Empty>No tickets yet.</Empty> : (
          <div className="space-y-3">
            {tickets.map((t) => (
              <div key={t.id} className="border border-slate-200 rounded-xl p-3">
                <div className="flex items-center justify-between mb-1">
                  <p className="font-semibold text-sm">#{t.id} · {t.subject}</p>
                  <StatusBadge status={t.status} />
                </div>
                <p className="text-sm text-slate-600">{t.message}</p>
                <p className="text-xs text-slate-400 mt-1">{new Date(t.createdAt).toLocaleString()}</p>
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  )
}

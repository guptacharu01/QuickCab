import { Link } from 'react-router-dom'
import { Card, Btn } from '../components/ui.jsx'

const FEATURES = [
  ['⚡', 'Live tracking', 'Watch your driver approach in real time on the map.', 'from-amber-400 to-orange-500'],
  ['💳', 'Secure payments', 'Stripe-powered test payments with proper receipts.', 'from-violet-500 to-fuchsia-500'],
  ['⭐', 'Ratings', 'Rate every trip and help the best drivers stand out.', 'from-emerald-400 to-teal-500'],
  ['🎟️', 'Promo codes', 'Apply discounts like WELCOME50 at booking time.', 'from-sky-400 to-indigo-500'],
]

const STEPS = [
  ['Pick your pins', 'Riders drop pickup and destination pins on the map and book.'],
  ['Driver accepts', 'Online drivers see the request — only one driver can take a ride.'],
  ['Track, ride & pay', 'Follow the driver live, ride, pay securely, then rate your trip.'],
]

export default function Landing() {
  return (
    <div>
      {/* Hero */}
      <div className="rise relative overflow-hidden rounded-3xl bg-gradient-to-br from-violet-800 via-purple-800 to-fuchsia-700 shadow-2xl shadow-purple-900/30">
        <div className="blob absolute -top-16 -left-16 w-72 h-72 rounded-full bg-fuchsia-400/30 blur-3xl" />
        <div className="blob absolute -bottom-20 -right-10 w-80 h-80 rounded-full bg-amber-300/25 blur-3xl" style={{ animationDelay: '-4s' }} />
        <div className="relative text-center px-6 py-16 md:py-20">
          <p className="inline-block text-xs font-bold uppercase tracking-widest text-amber-200 bg-white/10 rounded-full px-4 py-1.5 mb-5">
            🚖 Your city, on demand
          </p>
          <h1 className="text-4xl md:text-6xl font-extrabold text-white mb-4 tracking-tight">
            Your ride, <span className="text-transparent bg-clip-text bg-gradient-to-r from-amber-200 to-orange-300">one tap</span> away
          </h1>
          <p className="text-violet-100 mb-9 max-w-xl mx-auto text-base md:text-lg">
            QuickCab connects riders with nearby drivers — live tracking, transparent fares and secure payments.
          </p>
          <div className="flex gap-3 justify-center flex-wrap">
            <Link to="/register">
              <Btn className="!px-7 !py-3 !text-base !bg-gradient-to-r !from-amber-300 !to-orange-400 !text-slate-950 hover:!brightness-105 !shadow-orange-500/40 font-bold">
                Get started
              </Btn>
            </Link>
            <Link to="/login">
              <Btn variant="ghost" className="!px-7 !py-3 !text-base !text-white border !border-white/30 hover:!bg-white/10">
                Log in
              </Btn>
            </Link>
          </div>
        </div>
      </div>

      {/* Features */}
      <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4 mt-6">
        {FEATURES.map(([icon, title, text, grad]) => (
          <Card key={title} className="hover:-translate-y-1 transition-transform">
            <div className={`w-11 h-11 rounded-2xl bg-gradient-to-br ${grad} grid place-items-center text-2xl mb-3 shadow-md`}>{icon}</div>
            <h3 className="font-bold mb-1">{title}</h3>
            <p className="text-sm text-slate-500">{text}</p>
          </Card>
        ))}
      </div>

      {/* How it works */}
      <Card className="mt-6">
        <h2 className="font-extrabold text-lg mb-4">How it works</h2>
        <div className="grid md:grid-cols-3 gap-4">
          {STEPS.map(([title, text], i) => (
            <div key={title} className="flex gap-3">
              <span className="shrink-0 w-9 h-9 rounded-full bg-gradient-to-br from-violet-600 to-fuchsia-600 text-white font-extrabold grid place-items-center shadow-md shadow-fuchsia-500/30">
                {i + 1}
              </span>
              <div>
                <p className="font-bold text-sm">{title}</p>
                <p className="text-sm text-slate-500">{text}</p>
              </div>
            </div>
          ))}
        </div>
      </Card>
    </div>
  )
}

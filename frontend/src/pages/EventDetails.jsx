import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { bookingApi, eventApi, getErrorMessage } from '../api/client'
import { useAuth } from '../context/AuthContext'
import { SeatsBadge } from '../components/EventCard'
import { formatDateTime, formatMoney, formatPrice } from '../utils/format'

const MAX_PER_BOOKING = 10

export default function EventDetails() {
  const { id } = useParams()
  const { user } = useAuth()
  const navigate = useNavigate()

  const [event, setEvent] = useState(null)
  const [error, setError] = useState('')
  const [quantity, setQuantity] = useState(1)
  const [booking, setBooking] = useState(false)
  const [bookingError, setBookingError] = useState('')

  useEffect(() => {
    eventApi.get(id).then(setEvent).catch((err) => setError(getErrorMessage(err)))
  }, [id])

  const handleBook = async () => {
    if (!user) {
      navigate('/login', { state: { from: `/events/${id}` } })
      return
    }
    setBooking(true)
    setBookingError('')
    try {
      const result = await bookingApi.book(event.id, quantity)
      navigate('/my-bookings', { state: { justBookedId: result.id } })
    } catch (err) {
      setBookingError(getErrorMessage(err))
      // Seats may have changed while the user was on this page; refresh them
      eventApi.get(id).then(setEvent).catch(() => {})
    } finally {
      setBooking(false)
    }
  }

  if (error) return <div className="alert alert-error">{error} <Link to="/">Back to events</Link></div>
  if (!event) return <p className="center muted">Loading…</p>

  const maxQty = Math.min(MAX_PER_BOOKING, event.availableSeats)
  const soldOut = event.availableSeats === 0

  return (
    <div className="details-layout">
      <section className="card details-main">
        <Link to="/" className="muted small">← All events</Link>
        <h1>{event.title}</h1>
        <div className="details-meta">
          <p>🕒 {formatDateTime(event.startTime)}</p>
          <p>📍 {event.venue}, {event.city}</p>
          <p>👤 Organized by {event.organizerName}</p>
        </div>
        <h3>About this event</h3>
        <p className="description">{event.description || 'No description provided.'}</p>
      </section>

      <aside className="card booking-box">
        <div className="booking-price">
          <span className="price-large">{formatPrice(event.price)}</span>
          {Number(event.price) > 0 && <span className="muted"> / ticket</span>}
        </div>
        <SeatsBadge available={event.availableSeats} total={event.totalSeats} />

        {!soldOut && (
          <>
            <label className="field">
              <span>Number of tickets</span>
              <select value={quantity} onChange={(e) => setQuantity(Number(e.target.value))}>
                {Array.from({ length: maxQty }, (_, i) => i + 1).map((n) => (
                  <option key={n} value={n}>{n}</option>
                ))}
              </select>
            </label>
            <div className="total-row">
              <span>Total</span>
              <strong>{formatMoney(event.price * quantity)}</strong>
            </div>
          </>
        )}

        {bookingError && <div className="alert alert-error">{bookingError}</div>}

        <button className="btn btn-primary btn-block" disabled={soldOut || booking} onClick={handleBook}>
          {soldOut ? 'Sold out' : booking ? 'Booking…' : user ? 'Book now' : 'Login to book'}
        </button>
        <p className="muted small center">You'll get one QR ticket per seat.</p>
      </aside>
    </div>
  )
}

import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { bookingApi, getErrorMessage, qrImageUrl } from '../api/client'
import { formatDateTime, formatMoney } from '../utils/format'

export default function MyBookings() {
  const location = useLocation()
  const justBookedId = location.state?.justBookedId

  const [bookings, setBookings] = useState(null)
  const [error, setError] = useState('')
  const [cancellingId, setCancellingId] = useState(null)

  useEffect(() => {
    bookingApi.mine().then(setBookings).catch((err) => setError(getErrorMessage(err)))
  }, [])

  const handleCancel = async (booking) => {
    if (!window.confirm(`Cancel ${booking.quantity} ticket(s) for "${booking.eventTitle}"?`)) return
    setCancellingId(booking.id)
    setError('')
    try {
      const updated = await bookingApi.cancel(booking.id)
      setBookings((list) => list.map((b) => (b.id === updated.id ? updated : b)))
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setCancellingId(null)
    }
  }

  if (!bookings && !error) return <p className="center muted">Loading your tickets…</p>

  return (
    <>
      <div className="page-header">
        <h1>My Tickets</h1>
        {bookings?.length > 0 && <button className="btn btn-ghost no-print" onClick={() => window.print()}>🖨️ Print tickets</button>}
      </div>

      {justBookedId && <div className="alert alert-success no-print">🎉 Booking confirmed! Show these QR codes at the entrance.</div>}
      {error && <div className="alert alert-error">{error}</div>}

      {bookings?.length === 0 && (
        <div className="empty">
          <p>You haven't booked anything yet.</p>
          <Link to="/" className="btn btn-primary">Browse events</Link>
        </div>
      )}

      <div className="booking-list">
        {bookings?.map((booking) => {
          const cancelled = booking.status === 'CANCELLED'
          const anyUsed = booking.tickets.some((t) => t.checkedIn)
          const upcoming = new Date(booking.eventStartTime) > new Date()
          const canCancel = !cancelled && !anyUsed && upcoming

          return (
            <article key={booking.id} className={`card booking-card ${cancelled ? 'is-cancelled' : ''} ${booking.id === justBookedId ? 'is-highlighted' : ''}`}>
              <div className="booking-head">
                <div>
                  <Link to={`/events/${booking.eventId}`}><h3>{booking.eventTitle}</h3></Link>
                  <p className="muted">🕒 {formatDateTime(booking.eventStartTime)} · 📍 {booking.venue}, {booking.city}</p>
                  <p className="muted small">
                    Booking #{booking.id} · {booking.quantity} ticket(s) · {formatMoney(booking.totalAmount)}
                  </p>
                </div>
                <div className="booking-actions">
                  <span className={`badge ${cancelled ? 'badge-danger' : 'badge-success'}`}>{booking.status}</span>
                  {canCancel && (
                    <button className="btn btn-danger-outline btn-sm no-print" disabled={cancellingId === booking.id} onClick={() => handleCancel(booking)}>
                      {cancellingId === booking.id ? 'Cancelling…' : 'Cancel'}
                    </button>
                  )}
                </div>
              </div>

              {!cancelled && (
                <div className="ticket-grid">
                  {booking.tickets.map((ticket, index) => (
                    <div key={ticket.ticketCode} className={`ticket ${ticket.checkedIn ? 'is-used' : ''}`}>
                      <img src={qrImageUrl(ticket.ticketCode)} alt={`QR code for ticket ${index + 1}`} width="160" height="160" />
                      <strong>Ticket {index + 1}</strong>
                      <code className="ticket-code">{ticket.ticketCode.slice(0, 8).toUpperCase()}</code>
                      {ticket.checkedIn
                        ? <span className="badge badge-muted">✓ Used</span>
                        : <span className="badge badge-success">Valid</span>}
                    </div>
                  ))}
                </div>
              )}
            </article>
          )
        })}
      </div>
    </>
  )
}

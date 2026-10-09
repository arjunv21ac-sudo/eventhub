import { Link } from 'react-router-dom'
import { dayAndMonth, formatDateTime, formatPrice } from '../utils/format'

export function SeatsBadge({ available, total }) {
  if (available === 0) return <span className="badge badge-danger">Sold out</span>
  if (available <= Math.max(10, total * 0.1)) return <span className="badge badge-warning">Only {available} left</span>
  return <span className="badge badge-success">{available} seats left</span>
}

export default function EventCard({ event }) {
  const { day, month } = dayAndMonth(event.startTime)

  return (
    <Link to={`/events/${event.id}`} className="card event-card">
      <div className="event-card-top">
        <div className="date-badge">
          <span className="date-day">{day}</span>
          <span className="date-month">{month}</span>
        </div>
        <SeatsBadge available={event.availableSeats} total={event.totalSeats} />
      </div>
      <h3 className="event-title">{event.title}</h3>
      <p className="muted">📍 {event.venue}, {event.city}</p>
      <p className="muted">🕒 {formatDateTime(event.startTime)}</p>
      <div className="event-card-bottom">
        <span className="price">{formatPrice(event.price)}</span>
        <span className="link-arrow">View →</span>
      </div>
    </Link>
  )
}

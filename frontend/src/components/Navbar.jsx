import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Navbar() {
  const { user, isOrganizer, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <header className="navbar">
      <div className="container navbar-inner">
        <Link to="/" className="brand">🎟️ EventHub</Link>

        <nav className="nav-links">
          <NavLink to="/" end>Events</NavLink>
          {user && <NavLink to="/my-bookings">My Tickets</NavLink>}
          {isOrganizer && <NavLink to="/organizer" end>Dashboard</NavLink>}
          {isOrganizer && <NavLink to="/organizer/checkin">Scan</NavLink>}
        </nav>

        <div className="nav-user">
          {user ? (
            <>
              <span className="nav-name">
                {user.name}
                {isOrganizer && <span className="badge badge-brand">Organizer</span>}
              </span>
              <button className="btn btn-ghost btn-sm" onClick={handleLogout}>Logout</button>
            </>
          ) : (
            <>
              <Link to="/login" className="btn btn-ghost btn-sm">Login</Link>
              <Link to="/register" className="btn btn-primary btn-sm">Sign up</Link>
            </>
          )}
        </div>
      </div>
    </header>
  )
}

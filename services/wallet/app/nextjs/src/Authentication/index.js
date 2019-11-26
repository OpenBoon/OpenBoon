import { useState } from 'react'

import Login from '../Login'

const Authentication = ({ children }) => {
  const [user, setUser] = useState({ isAuthenticated: false })

  if (!user.isAuthenticated) {
    return <Login onSubmit={() => setUser({ isAuthenticated: true })} />
  }

  return children
}

export default Authentication

import { useState } from 'react'

import Login from '../Login'

const Authentication = ({ children }) => {
  const [user, setUser] = useState({ isAuthenticated: false })

  if (!user.isAuthenticated) {
    return (
      <Login
        onSubmit={({ email }) => () =>
          setUser({ isAuthenticated: true, email })}
      />
    )
  }

  return children({ user })
}

export default Authentication

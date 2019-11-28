import { useState, useEffect } from 'react'

import Login from '../Login'

import {
  getTokens,
  isUserAuthenticated,
  authenticateUser,
  getTokenTimeout,
  logout,
} from './helpers'

const Authentication = ({ children }) => {
  const [user, setUser] = useState({ isLoading: true, isAuthenticated: false })

  useEffect(() => {
    let timeoutId

    const { refreshToken } = getTokens()

    if (user.isLoading) {
      setUser({
        hasLoaded: true,
        isAuthenticated: isUserAuthenticated({ now: Date.now(), refreshToken }),
      })
    }

    if (user.isAuthenticated) {
      timeoutId = setTimeout(
        logout({ setUser }),
        getTokenTimeout({ now: Date.now(), refreshToken }),
      )
    }

    return () => clearTimeout(timeoutId)
  }, [user])

  if (user.isLoading) return null

  if (!user.isAuthenticated) {
    return <Login onSubmit={authenticateUser({ setUser })} />
  }

  return children({ user, logout: logout({ setUser }) })
}

export default Authentication

import { useState, useEffect } from 'react'

import Login from '../Login'

import {
  getTokens,
  isUserAuthenticated,
  clearTokens,
  authenticateUser,
  getTokenTimeout,
} from './helpers'

const Authentication = ({ children }) => {
  const [user, setUser] = useState({ isLoading: true, isAuthenticated: false })

  useEffect(() => {
    let timeoutId

    const { refreshToken } = getTokens()

    if (user.isLoading) {
      setUser({
        hasLoaded: true,
        isAuthenticated: isUserAuthenticated({ refreshToken }),
      })
    }

    if (user.isAuthenticated) {
      timeoutId = setTimeout(() => {
        clearTokens()
        setUser({ isAuthenticated: false, isTimedOut: true })
      }, getTokenTimeout({ refreshToken }))
    }

    return () => clearTimeout(timeoutId)
  }, [user])

  const logout = () => {
    clearTokens()
    setUser({ isAuthenticated: false })
  }

  if (user.isLoading) return null

  if (!user.isAuthenticated) {
    return <Login onSubmit={authenticateUser} />
  }

  return children({ user, logout })
}

export default Authentication

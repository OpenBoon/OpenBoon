import React, { useState } from 'react'
import {
  isUserAuthenticated,
  getAuthTokens,
  authenticateUser,
  clearAuthTokens,
  getTokenTimeout,
} from '../services/authServices'
import User from '../models/User'

const AuthContext = React.createContext()

function AuthProvider(props) {
  // check if user is logged in (tokens in localStorage)
  const initialUser = new User({
    attrs: { isAuthenticated: isUserAuthenticated(), tokens: getAuthTokens() },
  })

  const [user, setUser] = useState(initialUser)

  const login = (email, password) => {
    return authenticateUser(email, password).then(() => {
      const user = new User({
        ...initialUser,
        email,
        attrs: { isAuthenticated: true, tokens: getAuthTokens() },
      })
      setUser(user)
    })
  }

  const register = () => {
    // create user
  }

  const logout = () => {
    clearAuthTokens()
    setUser(new User({}))
  }

  const sessionTimeout = () => {
    clearAuthTokens()
    setUser(new User({ ...user, attrs: { isTimedOut: true } }))
  }

  if (user.attrs.isAuthenticated) {
    const { refreshToken } = user.attrs.tokens
    setTimeout(sessionTimeout, getTokenTimeout(refreshToken))
  }

  return (
    <AuthContext.Provider
      value={{ user, login, logout, register }}
      {...props}
    />
  )
}

const useAuth = () => React.useContext(AuthContext)

export { AuthProvider, useAuth }

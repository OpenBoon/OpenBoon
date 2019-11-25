import React, { useState } from 'react'
import {
  getAuthTokens,
  authenticateUser,
  unauthenticateUser,
} from '../services/authServices'
import User from '../models/User'

const AuthContext = React.createContext()

function AuthProvider(props) {
  // check if user is logged in (tokens in localStorage)
  const tokens = getAuthTokens()
  const initialUser = new User({ attrs: { isAuthenticated: tokens !== undefined, tokens } })

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
    unauthenticateUser()
    setUser(new User({}))
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

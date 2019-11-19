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
  const initialUser = new User({ attrs: { tokens: getAuthTokens() } })

  const [user, setUser] = useState(initialUser)

  const login = (email, password) => {
    authenticateUser(email, password).then(() => {
      const user = new User({
        ...initialUser,
        email,
        attrs: { tokens: getAuthTokens() },
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

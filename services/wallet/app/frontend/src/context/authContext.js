import React, { useState } from 'react'
import {
  getAuthTokens,
  authenticateUser,
  unauthenticateUser,
} from '../services/authServices'

const AuthContext = React.createContext()

function AuthProvider(props) {
  // check if user is logged in (tokens in localStorage)
  const tokens = getAuthTokens()
  const initialUser = { data: { tokens } }

  const [user, setUser] = useState(initialUser)

  const login = (email, password) => {
    authenticateUser(email, password).then(() => {
      const user = { ...initialUser, data: { tokens: getAuthTokens() } }
      setUser(user)
    })
  }

  const register = () => {
    // create user
  }

  const logout = () => {
    unauthenticateUser()
    setUser({ data: { tokens: undefined } })
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

import React, { useState } from 'react'
import { getAuthTokens } from '../services/authServices'
import { authenticateUser, unauthenticateUser } from '../services/authServices'

const AuthContext = React.createContext()

function AuthProvider(props) {
  // check if user is logged in (tokens in localStorage)
  const tokens = getAuthTokens()
  const user = { data: { tokens } }

  const [localUser, setUser] = useState(user)

  const login = (email, password) => {
    authenticateUser(email, password).then(() => {
      setUser({
        user: { ...user, data: { tokens: getAuthTokens() } }
      })
    })
  }

  const register = () => {
    // create user
  }

  const logout = () => {
    unauthenticateUser()
    setUser({ user })
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

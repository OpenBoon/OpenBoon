import React from 'react'
import { checkAuthentication, getAuthTokens } from '../services/authServices'

const AuthContext = React.createContext()

function AuthProvider(props) {
  // add code to check if user is logged in (localStorage)
  const tokens = getAuthTokens()

  // add some async logic for when use data is being fetched
  const user = { data: { tokens } }

  const loginFn = () => {
    // log user in (i.e. add tokens to user)
    user.data.tokens = getAuthTokens()
  }

  const registerFn = () => {
    // create user
  }

  const logoutFn = () => {
    // log user out
  }

  return (
    <AuthContext.Provider value={{ user, loginFn, logoutFn, registerFn }} {...props} />
  )
}

const useAuth = () => React.useContext(AuthContext)

export { AuthProvider, useAuth }
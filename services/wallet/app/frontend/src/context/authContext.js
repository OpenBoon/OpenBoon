import React from 'react'
import { checkAuthentication } from '../services/authServices'

const AuthContext = React.createContext()

function AuthProvider(props) {
  // add code to check if user is logged in (localStorage)
  const isAuthenticated = checkAuthentication()

  if (isAuthenticated) {
    // request user info; display spinner while loading
    return <div>{"Loading..."}</div>
  }

  const user = { data: {} }

  const loginFn = () => {
    // log user in
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
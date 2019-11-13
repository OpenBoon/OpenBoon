import React from 'react'

const AuthContext = React.createContext()

function AuthProvider(props) {
  // add code to check if user is logged in (localStorage)
  const user = { data: {} }
  const isAuthenticated = true

  if (!isAuthenticated) {
    return <div>{"Loading..."}</div>
  }

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
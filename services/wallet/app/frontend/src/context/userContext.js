import React from 'react'
import { useAuth } from './authContext'

const UserContext = React.createContext()

function UserProvider(props) {
  const { user } = useAuth()
  return (
    <UserContext.Provider value={user.data} {...props} />
  )
}

const useUser = () => React.useContext(UserContext)

export { UserProvider, useUser }
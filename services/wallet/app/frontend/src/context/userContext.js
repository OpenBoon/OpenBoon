import React from 'react'
import { useAuth } from './authContext'
import User from '../models/User'

const UserContext = React.createContext()

function UserProvider(props) {
  const { user } = useAuth()
  const userObj = new User({ ...user })
  return <UserContext.Provider value={{ user: userObj }} {...props} />
}

const useUser = () => React.useContext(UserContext)

export { UserProvider, useUser }

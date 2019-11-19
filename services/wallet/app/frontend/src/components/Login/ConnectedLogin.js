import React from 'react'
import Login from './Login'
import { useAuth } from '../../context/authContext'
import { useUser } from '../../context/userContext'

function ConnectedLogin(props) {
  const authContext = useAuth()
  const userContext = useUser()

  return <Login {...authContext} {...userContext} {...props} />
}
export default ConnectedLogin

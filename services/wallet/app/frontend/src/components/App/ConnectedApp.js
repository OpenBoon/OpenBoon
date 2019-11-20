import React from 'react'
import App from './App'
import { useAuth } from '../../context/authContext'
import { useUser } from '../../context/userContext'

function ConnectedApp() {
  const authContext = useAuth()
  const userContext = useUser()

  return <App {...authContext} {...userContext} />
}
export default ConnectedApp

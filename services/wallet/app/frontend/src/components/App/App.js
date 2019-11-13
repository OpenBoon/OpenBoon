import React from 'react'
import { useUser } from '../../context/userContext'
import Wallet from '../Wallet'
import Login from '../Login'

function App() {
  const user = useUser()
  return user ? <Wallet /> : <Login />
}

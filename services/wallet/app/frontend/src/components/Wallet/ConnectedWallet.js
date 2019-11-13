import React from 'react'
import Wallet from './Wallet'
import { useAuth } from '../../context/authContext'
import { useUser } from '../../context/userContext'

function ConnectedWallet() {
  const authContext = useAuth()
  const userContext = useUser()

  return (
    <Wallet {...authContext} {...userContext} />
  )
}
export default ConnectedWallet
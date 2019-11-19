import React from 'react'
import Wallet from './Wallet'
import { useAuth } from '../../context/authContext'
import { useUser } from '../../context/userContext'

function ConnectedWallet(props) {
  const authContext = useAuth()
  const userContext = useUser()

  return <Wallet {...authContext} {...userContext} {...props} />
}
export default ConnectedWallet

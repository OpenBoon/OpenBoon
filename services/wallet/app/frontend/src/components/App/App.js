import React from 'react'
import Wallet from '../Wallet'
import Login from '../Login'

function App(props) {
  const { tokens } = props.user.data
  return tokens ? <Wallet /> : <Login />
}

export default App

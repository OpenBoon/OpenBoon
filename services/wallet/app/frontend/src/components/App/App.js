import React from 'react'
import PropTypes from 'prop-types'
import Wallet from '../Wallet'
import Login from '../Login'

function App(props) {
  const { tokens } = props.user.data
  return tokens ? <Wallet /> : <Login />
}

App.propTypes = {
  user: PropTypes.shape({
    data: PropTypes.object,
  }).isRequired,
}

export default App

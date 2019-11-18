import React from 'react'
import PropTypes from 'prop-types'
import Wallet from '../Wallet'
import Login from '../Login'
import User from '../../models/User'

function App(props) {
  const { tokens } = props.user.attrs
  return tokens ? <Wallet /> : <Login />
}

App.propTypes = {
  user: PropTypes.instanceOf(User),
}

App.defaultProps = {
  user: new User({}),
}

export default App

import React from 'react'
import PropTypes from 'prop-types'
import Wallet from '../Wallet'
import Login from '../Login'

function App(props) {
  const { tokens } = props
  return tokens ? <Wallet /> : <Login />
}

App.propTypes = {
  user: PropTypes.shape({
    data: PropTypes.object,
  }).isRequired,
}

App.propTypes = {
  tokens: PropTypes.shape({
    access: PropTypes.string,
    refresh: PropTypes.string,
  }),
}

App.defaultProps = {
  tokens: undefined,
}

export default App

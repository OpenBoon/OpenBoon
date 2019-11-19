import React from 'react'
import PropTypes from 'prop-types'
import Workspace from '../Workspace'

function Wallet(props) {
  return (
    <Workspace {...props} />
  )
}

Wallet.propTypes = {
  logout: PropTypes.func.isRequired,
}

export default Wallet

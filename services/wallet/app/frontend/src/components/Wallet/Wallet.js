import React from 'react'
import PropTypes from 'prop-types'

function Wallet(props) {
  return (
    <div>
      <div className="Wallet">{'Hello World!'}</div>
      <button onClick={props.logout}>{'Logout'}</button>
    </div>
  )
}

Wallet.propTypes = {
  logout: PropTypes.func.isRequired,
}

export default Wallet

import React from 'react'
import PropTypes from 'prop-types'

function Workspace(props) {
  return (
    <div>
      <div className="Wallet">{'Hello World!'}</div>
      <button onClick={props.logout}>{'Logout'}</button>
    </div>
  )
}

Workspace.propTypes = {
  logout: PropTypes.func.isRequired,
}

export default Workspace

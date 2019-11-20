import React from 'react'
import PropTypes from 'prop-types'

function Workspace({ user, logout }) {
  if (user.attrs.tokens) {
    return <Redirect to={'/login'} />
  }

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

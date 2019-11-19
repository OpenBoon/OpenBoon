import React from 'react'

function Workspace(props) {
  return (
    <div>
      <div className="Wallet">{'Hello World!'}</div>
      <button onClick={props.logout}>{'Logout'}</button>
    </div>
  )
}

export default Workspace
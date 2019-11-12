import React from 'react'

function Auth(props) {
  return (
    <div>
      <div>{`Auth state: ${props.isAuthenticated}`}</div>
      <button onClick={props.updateAuthState}></button>
    </div>
  )
}

export default Auth
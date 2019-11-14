import React, { Component } from 'react'

function Wallet(props) {
  return (
    <div>
      <div className="Wallet">{'Hello World!'}</div>
      <button onClick={props.logout}>Logout</button>
    </div>)
}

export default Wallet

import React, { Component } from 'react'
import { checkAuthentication } from '../../services/authServices'

class Wallet extends Component {
  state = {
    authenticated: false,
    username: '',
    password: '',
  }

  componentWillMount() {
    this.setState({ authenticated: checkAuthentication() })
  }

  render() {
    return (
      <div className="Wallet">
        {`Hi I'm wallet`}
      </div>
    )
  }
}

export default Wallet

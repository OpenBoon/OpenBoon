import React, { Component } from 'react'
import PropTypes from 'prop-types'
import { Redirect } from 'react-router-dom'

import User from '../../models/User'

class Login extends Component {
  constructor(props) {
    super(props)

    this.state = {
      email: '',
      password: '',
    }

    this.onSubmit = this.onSubmit.bind(this)
  }

  onSubmit(event) {
    event.preventDefault()

    const { email, password } = this.state

    if (email !== '' && password !== '') {
      this.props.login(email, password)
    }
  }

  render() {
    const { email, password } = this.state
    const { user } = this.props

    if (user.attrs.tokens) {
      return <Redirect to={'/'} />
    }

    return (
      <div className="login-container">
        <form className="login-form" onSubmit={this.onSubmit}>
          <div className="login-inputs">
            <div className="login-input">
              <label htmlFor="email">{'Email'}</label>
              <input
                type="text"
                value={email}
                name="email"
                onChange={e => this.setState({ email: e.target.value })}
              />
            </div>

            <div className="login-input">
              <label htmlFor="password">{'Password'}</label>
              <input
                type="password"
                value={password}
                name="password"
                onChange={e => this.setState({ password: e.target.value })}
              />
            </div>

            <button className="login-button" type="submit">
              {'Login'}
            </button>
          </div>
        </form>
      </div>
    )
  }
}

Login.propTypes = {
  login: PropTypes.func.isRequired,
  user: PropTypes.instanceOf(User).isRequired,
}

export default Login

import React, { Component } from 'react'
import { Redirect } from 'react-router-dom'
import { authenticateUser, checkAuthentication } from '../../services/authServices'
import { AuthContext } from '../../context/authContext'


class Login extends Component {
  state = {
    isLoggedIn: false,
    setLoggedIn: () => { },
    username: '',
    password: '',
  }

  componentWillMount() {
    if (this.state.isLoggedIn !== checkAuthentication()) {
      this.setLoggedIn()
    }
  }

  submit = (event) => {
    const { username, password } = this.state
    event.preventDefault()
    authenticateUser(username, password).then(response => {
      this.context.setAuthTokens(response)
      this.setLoggedIn(checkAuthentication())
    })
  }

  setLoggedIn() {
    const loggedInStatus = this.state.isLoggedIn
    this.setState({ isLoggedIn: !loggedInStatus })
  }

  changeUsername = (event) => {
    this.setState({ username: event.target.value })
  }

  changePassword = (event) => {
    this.setState({ password: event.target.value })
  }

  render() {
    if (this.state.isLoggedIn) {
      return <Redirect to="/" />
    }
    return (
      <div>
        <form onSubmit={this.submit} className="auth-form">
          <div className="auth-field">
            <input
              className="auth-input"
              type="text"
              value={this.state.username}
              name="username"
              onChange={this.changeUsername}
            />

            <label className="auth-label">
              Username
            </label>
          </div>
          <div className="auth-field">
            <input
              className="auth-input"
              type="password"
              value={this.state.password}
              name="password"
              onChange={this.changePassword}
            />
            <label className="auth-label">
              Password
            </label>
          </div>
          <input type="submit"></input>
        </form>
      </div>
    )
  }
}

Login.contextType = AuthContext
export default Login
import React, { useState } from 'react'
import PropTypes from 'prop-types'

import Page from '../Page'
import { Redirect } from 'react-router-dom'

import User from '../../models/User'

function Login() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')

  function handleSubmit(e) {
    console.log(email, password)
    e.preventDefault()

    if (email !== '' && password !== '') {
      this.props.login(email, password)
    }
  }

  if (user.attrs.tokens) {
    return <Redirect to={'/'} />
  }

  return (
    <Page>
      <div className="login__page">
        <form className="login__form" onSubmit={handleSubmit}>
          <h3 className="login__form-heading">Welcome. Please login.</h3>

          <small className="">- or -</small>

          <div className="login__form-group">
            <label className="login__form-label" htmlFor="email">
              Email
            </label>
            <input
              id="email"
              type="text"
              value={email}
              name="email"
              className="login__form-input"
              onChange={e => setEmail(e.target.value)}
            />
          </div>

          <div className="login__form-group">
            <label className="login__form-label" htmlFor="password">
              Password
            </label>
            <input
              id="password"
              type="password"
              value={password}
              name="password"
              className="login__form-input"
              onChange={e => setPassword(e.target.value)}
            />
          </div>

          <button className="login__btn btn btn-primary" type="submit">
            Login
          </button>

          <small className="login__form-tagline">
            Forgot Password? Need login help?
          </small>
        </form>
      </div>
    </Page>
  )
}


Login.propTypes = {
  login: PropTypes.func.isRequired,
  user: PropTypes.instanceOf(User).isRequired,
}

export default Login

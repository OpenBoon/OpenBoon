import React, { useState, useRef } from 'react'
import PropTypes from 'prop-types'
import cx from 'classnames'
import { Redirect, Link } from 'react-router-dom'

import User from '../../models/User'
import Page from '../Page'
import Logo from '../Logo'

const ERROR_MESSAGE = 'Invalid email or password'

function Login({ user, login, history }) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const emailInput = useRef(null)

  function handleSubmit(e) {
    e.preventDefault()
    setError('')

    if (email !== '' && password !== '') {
      login(email, password)
        .then(() => {
          history.push('/workspace')
        })
        .catch(() => {
          setError(ERROR_MESSAGE)
          emailInput.current.focus()
        })
    } else {
      setError(ERROR_MESSAGE)
      emailInput.current.focus()
    }
  }

  if (user.attrs.tokens) {
    return <Redirect to={'/workspace'} />
  }

  return (
    <Page>
      <div className="login__page">
        <form className="login__form" onSubmit={handleSubmit}>
          <Logo width="143" height="42" />
          <h3 className="login__form-heading">Welcome. Please login.</h3>

          {/*
          (loading) && (
            <p className="login__form-loader">Loading</p>
          )
          */}

          {error && (
            <div className="login__form-error-container">
              <i className="fas fa-exclamation-triangle"></i>
              <p className="login__form-error-message">{error}</p>
            </div>
          )}

          {/* <p className="login__form-sub-heading">- or -</p> */}

          <div className="login__form-group">
            <label className="login__form-label" htmlFor="email">
              Email
            </label>
            <input
              id="email"
              ref={emailInput}
              type="text"
              value={email}
              name="email"
              className={cx(
                error ? 'login__form-input--error' : 'login__form-input',
              )}
              onChange={e => {
                setEmail(e.target.value)
                setError('')
              }}
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
              className={cx(
                error ? 'login__form-input--error' : 'login__form-input',
              )}
              onChange={e => {
                setPassword(e.target.value)
                setError('')
              }}
            />
          </div>

          <button
            className="login__btn btn btn-primary"
            type="submit"
            disabled={error.length}>
            Login
          </button>

          <Link to="/" className="login__form-tagline">
            Forgot Password? Need login help?
          </Link>
        </form>
      </div>
    </Page>
  )
}

Login.propTypes = {
  user: PropTypes.instanceOf(User).isRequired,
  login: PropTypes.func.isRequired,
  history: PropTypes.object.isRequired,
}

export default Login

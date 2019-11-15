import React, { useState } from 'react'

import Page from '../Page'

function Login() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')

  function handleSubmit(e) {
    console.log(email, password)
    e.preventDefault()
  }

  return (
    <Page>
      <div className="login__page">
        <form className="login__form" onSubmit={handleSubmit}>
          <h3 className="login__form-heading">Welcome. Please login.</h3>

          <small className="">- or -</small>

          <div className="login__form-group">
            <label className="login__form-label" htmlFor="email">Email</label>
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
            <label className="login__form-label" htmlFor="password">Password</label>
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

          <small className="login__form-tagline">Forgot Password? Need login help?</small>
        </form>
      </div>
    </Page>
  )
}

export default Login

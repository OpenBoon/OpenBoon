import PropTypes from 'prop-types'
import React from 'react'
import { Route, Redirect } from 'react-router-dom'

import User from '../../models/User'

const RequireAuth = ({ component: Component, user, ...rest }) => {
  return (
    <Route
      {...rest}
      render={props => {
        const { isAuthenticated } = user.attrs
        if (isAuthenticated) {
          return <Component {...props} />
        }
        return <Redirect to={'/login'} />
      }}
    />
  )
}

RequireAuth.propTypes = {
  component: PropTypes.func.isRequired,
  user: PropTypes.instanceOf(User).isRequired,
}

export default RequireAuth

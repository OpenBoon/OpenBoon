import PropTypes from 'prop-types'
import React from 'react'
import { Route, Redirect } from 'react-router-dom'

const RequireAuth = ({
  component: Component,
  user,
  ...rest
}) => (
    <Route
      {...rest}
      render={props => {
        const authenticated = !!user.attrs.tokens
        if (authenticated === true) {
          return <Component {...props} />
        }
        return <Redirect to={'/login'} />
      }}
    />
  )

export default RequireAuth

RequireAuth.propTypes = {
  component: PropTypes.func,
}

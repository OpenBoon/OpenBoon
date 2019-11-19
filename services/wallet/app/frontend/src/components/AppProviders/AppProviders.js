import React from 'react'
import PropTypes from 'prop-types'
import { AuthProvider } from '../../context/authContext'
import { UserProvider } from '../../context/userContext'

function AppProviders({ children }) {
  return (
    <AuthProvider>
      <UserProvider>
        {children}
      </UserProvider>
    </AuthProvider>
  )
}

AppProviders.propTypes = {
  children: PropTypes.node.isRequired,
}

export default AppProviders

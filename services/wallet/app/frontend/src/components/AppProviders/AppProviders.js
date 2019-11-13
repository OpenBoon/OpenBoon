import React from 'react'
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

export default AppProviders
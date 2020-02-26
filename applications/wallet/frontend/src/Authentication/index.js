import { useContext, useState, useEffect } from 'react'
import PropTypes from 'prop-types'
import getConfig from 'next/config'
import { SWRConfig } from 'swr'
import Router from 'next/router'

import { initializeFetcher } from '../Fetch/helpers'

import { UserContext } from '../User'

import Login from '../Login'
import Projects from '../Projects'
import Layout from '../Layout'
import ErrorBoundary from '../ErrorBoundary'

import { authenticateUser, logout } from './helpers'

const AUTHENTICATION_LESS_ROUTES = ['/create-account', '/reset-password']

const {
  publicRuntimeConfig: { GOOGLE_OAUTH_CLIENT_ID },
} = getConfig()

export const noop = () => () => {}

const Authentication = ({ route, children }) => {
  const { user, setUser, googleAuth, setGoogleAuth } = useContext(UserContext)

  const [hasGoogleLoaded, setHasGoogleLoaded] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  const fetcher = initializeFetcher({ setUser })

  useEffect(() => {
    window.onload = () => {
      window.gapi.load('auth2', async () => {
        setGoogleAuth(
          window.gapi.auth2.init({
            client_id: `${GOOGLE_OAUTH_CLIENT_ID}`,
          }),
        )
        setHasGoogleLoaded(true)
      })
    }
  }, [setGoogleAuth])

  if (AUTHENTICATION_LESS_ROUTES.includes(route)) {
    if (user.id) {
      Router.push('/')
      return null
    }

    return children
  }

  if (!user.id) {
    return (
      <Login
        googleAuth={googleAuth}
        hasGoogleLoaded={hasGoogleLoaded}
        errorMessage={errorMessage}
        setErrorMessage={setErrorMessage}
        onSubmit={authenticateUser({ setUser, setErrorMessage })}
      />
    )
  }

  return (
    <SWRConfig value={{ fetcher }}>
      <Projects projectId={user.projectId} setUser={setUser}>
        <Layout user={user} logout={logout({ googleAuth, setUser })}>
          <ErrorBoundary>{children}</ErrorBoundary>
        </Layout>
      </Projects>
    </SWRConfig>
  )
}

Authentication.propTypes = {
  children: PropTypes.node.isRequired,
  route: PropTypes.string.isRequired,
}

export default Authentication

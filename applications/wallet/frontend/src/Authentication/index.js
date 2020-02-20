import { useContext, useState, useEffect } from 'react'
import PropTypes from 'prop-types'
import getConfig from 'next/config'
import { SWRConfig } from 'swr'

import { initializeFetcher } from '../Fetch/helpers'

import { authenticateUser, initializeUserstorer, logout } from '../User/helpers'

import { UserContext } from '../User'

import Login from '../Login'
import Projects from '../Projects'
import Layout from '../Layout'

const {
  publicRuntimeConfig: { GOOGLE_OAUTH_CLIENT_ID },
} = getConfig()

export const noop = () => () => {}

const Authentication = ({ children }) => {
  const { user, setUser, googleAuth, setGoogleAuth } = useContext(UserContext)

  const [hasGoogleLoaded, setHasGoogleLoaded] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  const fetcher = initializeFetcher({ setUser })

  initializeUserstorer({ setUser })

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

  if (!user.id) {
    return (
      <Login
        googleAuth={googleAuth}
        hasGoogleLoaded={hasGoogleLoaded}
        errorMessage={errorMessage}
        setErrorMessage={setErrorMessage}
        onSubmit={authenticateUser({ setErrorMessage })}
      />
    )
  }

  return (
    <SWRConfig value={{ fetcher }}>
      <Projects>
        <Layout user={user} logout={logout({ googleAuth, setUser })}>
          {children}
        </Layout>
      </Projects>
    </SWRConfig>
  )
}

Authentication.propTypes = {
  children: PropTypes.node.isRequired,
}

export default Authentication

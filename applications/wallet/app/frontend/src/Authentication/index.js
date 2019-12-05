import { useState, useEffect } from 'react'
import PropTypes from 'prop-types'
import { SWRConfig } from 'swr'

import { axiosCreate, fetcher } from '../Axios/helpers'

import Login from '../Login'

import {
  getTokens,
  isUserAuthenticated,
  authenticateUser,
  getTokenTimeout,
  logout,
} from './helpers'

const Authentication = ({ children }) => {
  const axiosInstance = axiosCreate({})

  const [user, setUser] = useState({ hasLoaded: false, isAuthenticated: false })

  useEffect(() => {
    let timeoutId

    const { refreshToken } = getTokens()

    if (!user.hasLoaded) {
      setUser({
        hasLoaded: true,
        isAuthenticated: isUserAuthenticated({ now: Date.now(), refreshToken }),
      })
    }

    if (user.isAuthenticated) {
      timeoutId = setTimeout(
        logout({ setUser }),
        getTokenTimeout({ now: Date.now(), refreshToken }),
      )
    }

    return () => clearTimeout(timeoutId)
  }, [user])

  if (!user.hasLoaded) return null

  if (!user.isAuthenticated) {
    return <Login onSubmit={authenticateUser({ axiosInstance, setUser })} />
  }

  return (
    <SWRConfig value={{ fetcher: fetcher({ axiosInstance }) }}>
      {children({ user, logout: logout({ setUser }) })}
    </SWRConfig>
  )
}

Authentication.propTypes = {
  children: PropTypes.func.isRequired,
}

export default Authentication

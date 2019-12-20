import { useState, useEffect } from 'react'
import PropTypes from 'prop-types'
import { SWRConfig } from 'swr'

import Login from '../Login'
import Projects from '../Projects'

import { initialize } from '../Fetch/helpers'

import { getUser, authenticateUser, logout } from './helpers'

const Authentication = ({ children }) => {
  const [hasLoaded, setHasLoaded] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [user, setUser] = useState({})

  const fetcher = initialize({ setUser })

  useEffect(() => {
    if (hasLoaded) return

    const storedUser = getUser()

    setUser(storedUser)
    setHasLoaded(true)
  }, [hasLoaded, user])

  if (!hasLoaded) return null

  if (!user.username) {
    return (
      <Login
        errorMessage={errorMessage}
        setErrorMessage={setErrorMessage}
        onSubmit={authenticateUser({ setErrorMessage, setUser })}
      />
    )
  }

  return (
    <SWRConfig value={{ fetcher }}>
      <Projects user={user} logout={logout({ setUser })}>
        {({ selectedProject }) =>
          children({
            user,
            logout: logout({ setUser }),
            selectedProject,
          })
        }
      </Projects>
    </SWRConfig>
  )
}

Authentication.propTypes = {
  children: PropTypes.func.isRequired,
}

export default Authentication

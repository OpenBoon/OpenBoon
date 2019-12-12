import { useState, useEffect } from 'react'
import PropTypes from 'prop-types'
import { SWRConfig } from 'swr'

import Login from '../Login'
import Projects from '../Projects'

import { getUser, authenticateUser, logout, fetcher } from './helpers'

const Authentication = ({ children }) => {
  const [hasLoaded, setHasLoaded] = useState(false)
  const [user, setUser] = useState({})

  useEffect(() => {
    if (hasLoaded) return

    const storedUser = getUser()

    setUser(storedUser)
    setHasLoaded(true)
  }, [hasLoaded, user])

  if (!hasLoaded) return null

  if (!user.id) {
    return <Login onSubmit={authenticateUser({ setUser })} />
  }

  return (
    <SWRConfig value={{ fetcher: fetcher({ setUser }) }}>
      <Projects>
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

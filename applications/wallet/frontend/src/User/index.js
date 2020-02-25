import { createContext, useState, useEffect } from 'react'
import PropTypes from 'prop-types'

import { getUser, setUser } from './helpers'

export const noop = () => {}

export const UserContext = createContext({
  user: {},
  setUser: noop,
  googleAuth: {},
  setGoogleAuth: noop,
})

const User = ({ initialUser, children }) => {
  const [user, setStateUser] = useState(initialUser)
  const [hasLocalStorageLoaded, setHasLocalStorageLoaded] = useState(false)
  const [googleAuth, setGoogleAuth] = useState({ signIn: noop, signOut: noop })

  useEffect(() => {
    if (initialUser.id || hasLocalStorageLoaded) return

    const storedUser = getUser()

    setStateUser(storedUser)

    setHasLocalStorageLoaded(true)
  }, [initialUser, hasLocalStorageLoaded, user, setStateUser])

  if (!initialUser.id && !hasLocalStorageLoaded) return null

  return (
    <UserContext.Provider
      value={{
        user,
        setUser: setUser({ setStateUser, user }),
        googleAuth,
        setGoogleAuth,
      }}>
      {children}
    </UserContext.Provider>
  )
}

User.propTypes = {
  initialUser: PropTypes.shape({
    id: PropTypes.number,
    email: PropTypes.string,
  }).isRequired,
  children: PropTypes.node.isRequired,
}

export default User

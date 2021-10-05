import { createContext, useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'

import { noop, meFetcher } from './helpers'

export const UserContext = createContext({
  user: {},
  googleAuth: {},
  setGoogleAuth: noop,
})

const User = ({ initialUser, children }) => {
  const [googleAuth, setGoogleAuth] = useState({ signIn: noop, signOut: noop })

  const { data } = useSWR(`/api/v1/me/`, meFetcher)

  const user = initialUser.id ? initialUser : data

  if (!user) return null

  return (
    <UserContext.Provider
      value={{
        user,
        googleAuth,
        setGoogleAuth,
      }}
    >
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

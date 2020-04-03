import { createContext, useState, useEffect } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'
import * as Sentry from '@sentry/browser'

export const noop = () => {}

export const UserContext = createContext({
  user: {},
  mutate: noop,
  googleAuth: {},
  setGoogleAuth: noop,
})

const User = ({ initialUser, children }) => {
  const [googleAuth, setGoogleAuth] = useState({ signIn: noop, signOut: noop })

  const { data, mutate } = useSWR(
    `/api/v1/me/`,
    typeof window === 'undefined'
      ? noop
      : async (url) => {
          try {
            const response = await fetch(url)
            if (response.status >= 400) throw response
            return response.json()
          } catch (error) {
            return {}
          }
        },
  )

  const user = initialUser.id ? initialUser : data

  useEffect(() => {
    /* istanbul ignore next */
    Sentry.configureScope((scope) => {
      scope.setUser(user)
    })
  }, [user])

  if (!user) return null

  return (
    <UserContext.Provider
      value={{
        user,
        mutate,
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

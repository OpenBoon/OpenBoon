import { createContext, useState } from 'react'
import PropTypes from 'prop-types'

export const noop = () => {}

export const UserContext = createContext({
  user: {},
  setUser: noop,
  googleAuth: {},
  setGoogleAuth: noop,
})

const User = ({ children }) => {
  const [user, setUser] = useState({})
  const [googleAuth, setGoogleAuth] = useState({ signIn: noop, signOut: noop })

  return (
    <UserContext.Provider value={{ user, setUser, googleAuth, setGoogleAuth }}>
      {children}
    </UserContext.Provider>
  )
}

User.propTypes = {
  children: PropTypes.node.isRequired,
}

export default User

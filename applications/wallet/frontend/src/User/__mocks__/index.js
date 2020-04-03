import { createContext, createElement } from 'react'

import user from './user'

export const noop = () => {}

export const UserContext = createContext({
  user,
  mutate: noop,
  googleAuth: {},
  setGoogleAuth: noop,
})

const User = ({ children, ...rest }) => createElement('User', rest, children)

export default User

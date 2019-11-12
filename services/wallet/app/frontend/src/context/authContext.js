import { createContext, useContext } from 'react'

export const AuthContext = createContext({
  authTokens: {},
  setAuthTokens: () => { },
})

export function useAuth() {
  return useContext(AuthContext)
}
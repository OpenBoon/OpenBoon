import jwtDecode from 'jwt-decode'

import { ACCESS_TOKEN, REFRESH_TOKEN } from './constants'

export const getTokens = () => {
  const accessToken = localStorage.getItem(ACCESS_TOKEN)
  const refreshToken = localStorage.getItem(REFRESH_TOKEN)

  if (accessToken && refreshToken) {
    return { accessToken, refreshToken }
  }

  return {}
}

export const isUserAuthenticated = ({ now, refreshToken }) => {
  if (!refreshToken) return false

  const decodedToken = jwtDecode(refreshToken)
  const currentTime = now / 1000
  const expirationTime = decodedToken.exp

  const isAuthenticated = currentTime < expirationTime

  return isAuthenticated
}

export const clearTokens = () => {
  localStorage.removeItem(ACCESS_TOKEN)
  localStorage.removeItem(REFRESH_TOKEN)
}

export const storeTokens = ({ accessToken, refreshToken }) => {
  localStorage.setItem(ACCESS_TOKEN, accessToken)
  localStorage.setItem(REFRESH_TOKEN, refreshToken)
}

export const getTokenTimeout = ({ now, refreshToken }) => {
  const decodedToken = jwtDecode(refreshToken)
  const currentTime = now / 1000
  const expirationTime = decodedToken.exp

  return (expirationTime - currentTime - 30) * 1000 // set to 30 seconds before expiration
}

export const authenticateUser = ({ axiosInstance, setUser }) => ({
  username,
  password,
}) => {
  return axiosInstance
    .post('/auth/token/', { username, password })
    .then(({ data }) => {
      const { access: accessToken, refresh: refreshToken } = data
      storeTokens({ accessToken, refreshToken })
      setUser({ isAuthenticated: true })
    })
}

export const logout = ({ setUser }) => () => {
  clearTokens()
  setUser({ isAuthenticated: false })
}

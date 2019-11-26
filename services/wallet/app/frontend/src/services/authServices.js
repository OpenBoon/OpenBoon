import jwtDecode from 'jwt-decode'
import { axiosCreate } from './axiosServices'
import { ACCESS_TOKEN, REFRESH_TOKEN } from '../constants/authConstants'

export function getAuthTokens() {
  const accessToken = JSON.parse(localStorage.getItem(ACCESS_TOKEN))
  const refreshToken = JSON.parse(localStorage.getItem(REFRESH_TOKEN))
  if (accessToken && refreshToken) {
    return { accessToken, refreshToken }
  }
}

export function isUserAuthenticated() {
  const tokens = getAuthTokens()
  if (!tokens) return false

  const { refreshToken } = tokens
  const parsedToken = jwtDecode(refreshToken.slice(7)) // slice off 'Bearer' and feed token only
  const currentTime = Date.now() / 1000
  const expirationTime = parsedToken.exp

  const isAuthenticated = currentTime < expirationTime
  return isAuthenticated
}

export function authenticateUser(username, password) {
  const instance = axiosCreate({})
  const tokenPromise = instance
    .post('/auth/token/', {
      username,
      password,
    })
    .then(response => {
      const tokens = response.data
      storeAuthTokens(tokens)
    })
  return tokenPromise
}

export function clearAuthTokens() {
  localStorage.removeItem(ACCESS_TOKEN)
  localStorage.removeItem(REFRESH_TOKEN)
}

export function storeAuthTokens(tokens) {
  localStorage.setItem(ACCESS_TOKEN, JSON.stringify(`Bearer ${tokens.access}`))
  localStorage.setItem(
    REFRESH_TOKEN,
    JSON.stringify(`Bearer ${tokens.refresh}`),
  )
}

export function getTokenTimeout(refreshToken) {
  const parsedToken = jwtDecode(refreshToken.slice(7))
  const currentTime = Date.now() / 1000
  const expirationTime = parsedToken.exp

  return (expirationTime - currentTime - 30) * 1000 // set to 30 seconds before expiration
}

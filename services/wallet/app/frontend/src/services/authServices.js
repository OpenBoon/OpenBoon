import jwtDecode from 'jwt-decode'
import axiosCreate from './axiosCreate'
import { ACCESS_TOKEN, REFRESH_TOKEN } from '../constants/authConstants'

export function checkAuthentication() {
  const token = localStorage.getItem(REFRESH_TOKEN)
  if (!token) return false

  const currentTime = Date.now() / 1000
  const refreshToken = jwtDecode(token)
  const expirationTime = refreshToken.exp

  const isAuthenticated = refreshToken && currentTime < expirationTime
  return isAuthenticated
}

export function authenticateUser(username, password) {
  const instance = axiosCreate({})
  const tokenPromise = instance.post('/auth/token/', {
    username,
    password
  })
  return tokenPromise
}

export function unauthenticateUser() {
  localStorage.setItem(ACCESS_TOKEN, '')
  localStorage.setItem(REFRESH_TOKEN, '')
}

export function refreshTokens(tokens) {
  console.log(tokens)
}

export function storeAuthAccessTokens(tokens) {
  localStorage.setItem(
    ACCESS_TOKEN,
    JSON.stringify(tokens['access'])
  )

  localStorage.setItem(
    REFRESH_TOKEN,
    JSON.stringify(tokens['refresh'])
  )
}
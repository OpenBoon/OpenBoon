/* eslint-disable no-restricted-imports */
import axios from 'axios'
import createAuthRefreshInterceptor from 'axios-auth-refresh'
import { REFRESH_TOKEN } from '../constants/authConstants'
import { storeAuthTokens } from '../services/authServices'

const ORIGIN = 'http://localhost:8000'

function axiosIntercept(axiosInstance) {

  const refreshAuthTokens = (failedRequest) => {
    const refreshToken = localStorage.getItem(REFRESH_TOKEN)
    axiosInstance.post('/auth/refresh/', { refresh: refreshToken }).then(response => {
      const tokens = response.data
      storeAuthTokens(tokens)
      failedRequest.response.config.headers['Authorization'] = `Bearer ${tokens.access}`
    })
    return Promise.resolve()
  }

  return createAuthRefreshInterceptor(axiosInstance, refreshAuthTokens)
}

export function axiosCreate(options = {}) {
  const customDefaultOptions = {
    baseURL: ORIGIN,
    withCredentials: true,
  }

  const axiosInstance = axios.create({
    ...customDefaultOptions,
    ...options,
  })

  return axiosIntercept(axiosInstance)
}
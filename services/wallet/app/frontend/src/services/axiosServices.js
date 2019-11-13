import axios from 'axios'

const ORIGIN = 'http://localhost:8000'

export function axiosCreate(options = {}) {
  const customDefaultOptions = {
    baseURL: ORIGIN,
    withCredentials: true,
  }

  const axiosInstance = axios.create({
    ...customDefaultOptions,
    ...options,
  })

  return axiosInstance
}

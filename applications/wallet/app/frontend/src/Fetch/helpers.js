import { clearUser } from '../Authentication/helpers'

let enhancedFetch

export const getCsrfToken = () => {
  if (typeof document === 'undefined') return ''

  const { csrftoken } = Object.fromEntries(
    document.cookie.split(/; */).map(c => {
      const [key, ...v] = c.split('=')
      return [key, decodeURIComponent(v.join('='))]
    }),
  )

  return csrftoken
}

export const initialize = ({ setUser }) => {
  enhancedFetch = async (url, options = {}) => {
    const csrftoken = getCsrfToken()

    const response = await fetch(url, {
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': csrftoken,
        ...options.headers,
      },
      ...options,
    })

    if (response.status === 401) {
      clearUser()

      setUser({})

      return {}
    }

    if (response.status !== 200) return response

    return response.json()
  }

  return enhancedFetch
}

export const fetcher = (...args) => {
  return enhancedFetch(...args)
}

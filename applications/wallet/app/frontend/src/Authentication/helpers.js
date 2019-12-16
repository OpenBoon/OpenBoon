export const USER = 'user'

export const getUser = () => {
  try {
    return JSON.parse(localStorage.getItem(USER)) || {}
  } catch (error) {
    return {}
  }
}

export const clearUser = () => {
  localStorage.removeItem(USER)
}

export const storeUser = ({ user }) => {
  localStorage.setItem(USER, JSON.stringify(user))
}

export const authenticateUser = ({ setErrorMessage, setUser }) => async ({
  username,
  password,
}) => {
  setErrorMessage('')

  const response = await fetch('/api/v1/login/', {
    method: 'POST',
    headers: { 'content-type': 'application/json;charset=UTF-8' },
    body: JSON.stringify({ username, password }),
  })

  if (response.status === 401) {
    return setErrorMessage('Invalid email or password.')
  }

  const user = await response.json()

  storeUser({ user })

  return setUser(user)
}

export const logout = ({ setUser }) => async () => {
  const { csrftoken } = Object.fromEntries(
    document.cookie.split(/; */).map(c => {
      const [key, ...v] = c.split('=')
      return [key, decodeURIComponent(v.join('='))]
    }),
  )

  await fetch('/api/v1/logout/', {
    method: 'POST',
    headers: {
      'content-type': 'application/json;charset=UTF-8',
      'X-CSRFToken': csrftoken,
    },
  })

  clearUser()

  setUser({})
}

export const fetcher = ({ setUser }) => async (...args) => {
  const response = await fetch(...args)

  if (response.status === 401) {
    clearUser()

    setUser({})

    return {}
  }

  return response.json()
}

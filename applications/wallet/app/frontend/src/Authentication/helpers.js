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

export const authenticateUser = ({ setUser }) => async ({
  username,
  password,
}) => {
  const response = await fetch('/api/v1/login', {
    method: 'POST',
    headers: { 'content-type': 'application/json;charset=UTF-8' },
    body: JSON.stringify({ username, password }),
  })

  const user = await response.json()

  storeUser({ user })

  setUser(user)
}

export const logout = ({ setUser }) => async () => {
  await fetch('/api/v1/logout', {
    method: 'POST',
    headers: { 'content-type': 'application/json;charset=UTF-8' },
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

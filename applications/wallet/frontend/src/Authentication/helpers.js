import Router from 'next/router'

let storeUser

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

export const initializeUserstorer = ({ setUser }) => {
  storeUser = ({ user }) => {
    localStorage.setItem(USER, JSON.stringify(user))

    setUser(user)
  }
}

export const userstorer = ({ user }) => {
  return storeUser({ user })
}

export const authenticateUser = ({ setErrorMessage }) => async ({
  username,
  password,
  idToken,
}) => {
  setErrorMessage('')

  const response = await fetch('/api/v1/login/', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
      ...(idToken ? { Authorization: `Bearer ${idToken}` } : {}),
    },
    ...(idToken ? {} : { body: JSON.stringify({ username, password }) }),
  })

  if (response.status === 401) {
    return setErrorMessage('Invalid email or password.')
  }

  if (response.status !== 200) {
    return setErrorMessage('Network error.')
  }

  const user = await response.json()

  return userstorer({ user })
}

export const logout = ({ googleAuth, setUser }) => async () => {
  googleAuth.signOut()

  const { csrftoken } = Object.fromEntries(
    document.cookie.split(/; */).map(c => {
      const [key, ...v] = c.split('=')
      return [key, decodeURIComponent(v.join('='))]
    }),
  )

  await fetch('/api/v1/logout/', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
      'X-CSRFToken': csrftoken,
    },
  })

  clearUser()

  setUser({})

  Router.push('/')
}

export const USER = 'user'

export const getUser = () => {
  try {
    return JSON.parse(localStorage.getItem(USER)) || {}
  } catch (error) {
    return {}
  }
}

export const setUser = ({ setStateUser, user }) => ({ user: updates }) => {
  if (updates === null) {
    localStorage.removeItem(USER)

    setStateUser({})
  } else {
    const updatedUser = { ...user, ...updates }

    localStorage.setItem(USER, JSON.stringify(updatedUser))

    setStateUser(updatedUser)
  }
}

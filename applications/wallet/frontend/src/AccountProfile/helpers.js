import { fetcher } from '../Fetch/helpers'

export const onSubmit = async ({
  dispatch,
  state: { id, firstName, lastName },
  setUser,
}) => {
  try {
    const user = await fetcher(`/api/v1/users/${id}/`, {
      method: 'PATCH',
      body: JSON.stringify({ firstName, lastName }),
    })

    dispatch({
      firstName: user.firstName,
      lastName: user.lastName,
      showForm: false,
      success: true,
      errors: {},
    })

    setUser({ user })
  } catch (response) {
    const errors = await response.json()
    const parsedErrors = Object.keys(errors).reduce((acc, errorKey) => {
      acc[errorKey] = errors[errorKey].join(' ')

      return acc
    }, {})

    dispatch({ success: false, errors: parsedErrors })
  }
}

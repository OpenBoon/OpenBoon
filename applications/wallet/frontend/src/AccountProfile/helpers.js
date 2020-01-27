import { fetcher } from '../Fetch/helpers'
import { storeUser } from '../Authentication/helpers'

export const onSubmit = async ({
  dispatch,
  state: { id, firstName, lastName },
}) => {
  try {
    const user = await fetcher(`/api/v1/users/${id}/`, {
      method: 'PATCH',
      body: JSON.stringify({ firstName, lastName }),
    })

    storeUser({ user })

    dispatch({
      firstName: user.firstName,
      lastName: user.lastName,
      errors: {},
    })
  } catch (response) {
    const errors = await response.json()
    const parsedErrors = Object.keys(errors).reduce((acc, errorKey) => {
      acc[errorKey] = errors[errorKey].join(' ')

      return acc
    }, {})

    dispatch({ errors: parsedErrors })
  }
}

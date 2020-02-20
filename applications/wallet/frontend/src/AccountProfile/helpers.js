import { fetcher } from '../Fetch/helpers'

import { userstorer } from '../User/helpers'

export const onSubmit = async ({
  dispatch,
  state: { id, firstName, lastName },
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

    userstorer({ user })
  } catch (response) {
    const errors = await response.json()
    const parsedErrors = Object.keys(errors).reduce((acc, errorKey) => {
      acc[errorKey] = errors[errorKey].join(' ')

      return acc
    }, {})

    dispatch({ success: false, errors: parsedErrors })
  }
}

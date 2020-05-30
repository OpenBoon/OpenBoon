import { mutate } from 'swr'

import { fetcher } from '../Fetch/helpers'

export const onSubmit = async ({
  dispatch,
  state: { id, firstName, lastName },
}) => {
  dispatch({ isLoading: true })

  try {
    await fetcher(`/api/v1/users/${id}/`, {
      method: 'PATCH',
      body: JSON.stringify({ firstName, lastName }),
    })

    dispatch({
      showForm: false,
      success: true,
      isLoading: false,
      errors: {},
    })

    mutate('/api/v1/me/', (user) => ({ ...user, firstName, lastName }), false)
  } catch (response) {
    const errors = await response.json()
    const parsedErrors = Object.keys(errors).reduce((acc, errorKey) => {
      acc[errorKey] = errors[errorKey].join(' ')

      return acc
    }, {})

    dispatch({ success: false, isLoading: false, errors: parsedErrors })
  }
}

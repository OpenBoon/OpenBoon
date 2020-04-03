import { fetcher } from '../Fetch/helpers'

export const onSubmit = async ({
  dispatch,
  state: { id, firstName, lastName },
  mutate,
}) => {
  try {
    await fetcher(`/api/v1/users/${id}/`, {
      method: 'PATCH',
      body: JSON.stringify({ firstName, lastName }),
    })

    dispatch({
      showForm: false,
      success: true,
      errors: {},
    })

    mutate((user) => ({ ...user, firstName, lastName }), false)
  } catch (response) {
    const errors = await response.json()
    const parsedErrors = Object.keys(errors).reduce((acc, errorKey) => {
      acc[errorKey] = errors[errorKey].join(' ')

      return acc
    }, {})

    dispatch({ success: false, errors: parsedErrors })
  }
}

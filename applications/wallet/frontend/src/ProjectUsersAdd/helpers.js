import { fetcher } from '../Fetch/helpers'

export const onSubmit = async ({
  projectId,
  dispatch,
  state: { emails: e, roles: r },
}) => {
  dispatch({ isLoading: true })

  try {
    const emails = e.split(',').map((str) => str.trim(''))
    const roles = Object.keys(r).filter((name) => r[name])
    const body = JSON.stringify({
      batch: emails.map((email) => ({ email, roles })),
    })

    const {
      results: { succeeded, failed },
    } = await fetcher(`/api/v1/projects/${projectId}/users/`, {
      method: 'POST',
      body,
    })

    dispatch({ succeeded, failed, isLoading: false })
  } catch (response) {
    try {
      const errors = await response.json()

      const parsedErrors = Object.keys(errors).reduce((acc, errorKey) => {
        acc[errorKey] = errors[errorKey].join(' ')
        return acc
      }, {})

      dispatch({ isLoading: false, errors: parsedErrors })
    } catch (error) {
      dispatch({
        isLoading: false,
        errors: { global: 'Something went wrong. Please try again.' },
      })
    }
  }
}

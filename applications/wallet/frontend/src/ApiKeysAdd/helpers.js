import { fetcher } from '../Fetch/helpers'

export const onSubmit = async ({
  dispatch,
  projectId,
  state: { name, permissions: p },
}) => {
  dispatch({ isLoading: true })

  try {
    const permissions = Object.keys(p).filter((key) => p[key])

    const apikey = await fetcher(`/api/v1/projects/${projectId}/api_keys/`, {
      method: 'POST',
      body: JSON.stringify({ name, permissions }),
    })

    dispatch({ apikey, isLoading: false })
  } catch (response) {
    const errors = await response.json()

    const parsedErrors = Object.keys(errors).reduce((accumulator, errorKey) => {
      accumulator[errorKey] = errors[errorKey].join(' ')
      return accumulator
    }, {})

    dispatch({ isLoading: false, errors: parsedErrors })
  }
}

import { fetcher, parseResponse } from '../Fetch/helpers'

export const onSubmit = async ({
  dispatch,
  projectId,
  state: { name, permissions: p },
}) => {
  dispatch({ isLoading: true, errors: {} })

  try {
    const permissions = Object.keys(p).filter((key) => p[key])

    const apikey = await fetcher(`/api/v1/projects/${projectId}/api_keys/`, {
      method: 'POST',
      body: JSON.stringify({ name, permissions }),
    })

    dispatch({ apikey, isLoading: false })
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({ isLoading: false, errors })
  }
}

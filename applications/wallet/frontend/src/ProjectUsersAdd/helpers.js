import { fetcher, parseResponse } from '../Fetch/helpers'

export const onSubmit = async ({
  projectId,
  dispatch,
  state: { emails: e, roles: r },
}) => {
  dispatch({ isLoading: true, errors: {} })

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
    const errors = await parseResponse({ response })

    dispatch({ isLoading: false, errors })
  }
}

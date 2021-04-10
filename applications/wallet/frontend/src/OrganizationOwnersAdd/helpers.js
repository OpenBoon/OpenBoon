import { fetcher, parseResponse } from '../Fetch/helpers'

export const onSubmit = async ({
  organizationId,
  dispatch,
  state: { emails: e },
}) => {
  dispatch({ isLoading: true, errors: {} })

  try {
    const emails = e.split(',').map((str) => str.trim(''))

    const body = JSON.stringify({ emails })

    const {
      results: { succeeded, failed },
    } = await fetcher(`/api/v1/organizations/${organizationId}/owners/`, {
      method: 'POST',
      body,
    })

    dispatch({ succeeded, failed, isLoading: false })
  } catch (response) {
    const errors = await parseResponse({ response })

    dispatch({ isLoading: false, errors })
  }
}
